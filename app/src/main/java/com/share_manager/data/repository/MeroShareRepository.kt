package com.share_manager.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.share_manager.data.db.AccountDao
import com.share_manager.data.model.*
import com.share_manager.util.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeroShareRepository @Inject constructor(
    private val dao: AccountDao,
    private val okHttpClient: OkHttpClient
) {

    private val gson = Gson()

    // ── Captcha constants ─────────────────────────────────────────────────────
    // The iporesult endpoint currently accepts these static values.
    // If the backend ever enforces a real captcha challenge these must be
    // fetched from a /captcha endpoint before each result check.
    private val captchaValue      = "28157"
    private val captchaIdentifier = "b12025e7-12bc-4c87-8919-54b68c03f780"

    // ── Accounts ──────────────────────────────────────────────────────────────

    /**
     * Emits the fully-decrypted account list whenever the DB changes.
     * Both [Account.password] and [Account.transactionPin] are stored
     * encrypted and decrypted here before being exposed to the UI/ViewModels.
     */
    val accounts: Flow<List<Account>> = dao.getAllAccounts().map { list ->
        list.map { acc ->
            acc.copy(
                password       = EncryptionUtil.decrypt(acc.password),
                transactionPin = EncryptionUtil.decrypt(acc.transactionPin)
            )
        }
    }

    /** Encrypts sensitive fields before persisting a new account. */
    suspend fun saveAccount(account: Account) {
        dao.insertAccount(
            account.copy(
                password       = EncryptionUtil.encrypt(account.password),
                transactionPin = EncryptionUtil.encrypt(account.transactionPin)
            )
        )
    }

    /** Encrypts sensitive fields before updating an existing account. */
    suspend fun updateAccount(account: Account) {
        dao.updateAccount(
            account.copy(
                password       = EncryptionUtil.encrypt(account.password),
                transactionPin = EncryptionUtil.encrypt(account.transactionPin)
            )
        )
    }

    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    // ── Company shares ────────────────────────────────────────────────────────

    /**
     * GET https://iporesult.cdsc.com.np/result/companyShares/fileUploaded
     *
     * Uses raw OkHttp + manual Gson parsing because the server sometimes
     * returns non-strict JSON that Retrofit/GsonConverterFactory rejects.
     *
     * Returns only companies where [CompanyShare.isFileUploaded] == true.
     */
    suspend fun getCompanyShares(): Result<List<CompanyShare>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://iporesult.cdsc.com.np/result/companyShares/fileUploaded")
                .addHeader("Accept", "application/json, text/plain, */*")
                .get()
                .build()

            val responseStr = okHttpClient.newCall(request).execute()
                .use { it.body?.string().orEmpty() }

            // Safely parse the root element — the server sometimes returns a bare
            // string or number instead of an object (e.g. during maintenance), which
            // causes a JsonSyntaxException if we cast directly to JsonObject.
            val root = try {
                JsonParser.parseString(responseStr.trim())
            } catch (e: JsonSyntaxException) {
                throw IllegalStateException("Invalid JSON from server: ${responseStr.take(120)}")
            }

            // If the root is not an object, surface it as a readable error.
            if (!root.isJsonObject) {
                val preview = if (root.isJsonPrimitive) root.asString else responseStr.take(120)
                throw IllegalStateException("Unexpected server response: $preview")
            }

            val json    = root.asJsonObject
            val success = json.get("success")?.asBoolean ?: false
            if (!success) {
                val msg = json.get("message")?.asString ?: "Unknown error from server"
                throw IllegalStateException(msg)
            }

            val list = json.getAsJsonObject("body")
                ?.getAsJsonArray("companyShareList")
                ?: return@runCatching emptyList()

            list.mapNotNull { el ->
                runCatching {
                    val obj = el.asJsonObject
                    CompanyShare(
                        id             = obj.get("id").asInt,
                        name           = obj.get("name").asString,
                        scrip          = obj.get("scrip").asString,
                        isFileUploaded = obj.get("isFileUploaded").asBoolean
                    )
                }.getOrNull()
            }.filter { it.isFileUploaded }
        }
    }

    // ── Result check ──────────────────────────────────────────────────────────

    /**
     * POST https://iporesult.cdsc.com.np/result/result/check
     *
     * Parses "quantity : 10" from a success message for allotted shares.
     */
    suspend fun checkResult(companyShareId: Int, boid: String): ResultStatus =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = gson.toJson(
                    ResultCheckRequest(
                        companyShareId    = companyShareId,
                        boid              = boid,
                        userCaptcha       = captchaValue,
                        captchaIdentifier = captchaIdentifier
                    )
                )
                val body = payload.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("https://iporesult.cdsc.com.np/result/result/check")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Authorization", "null")
                    .post(body)
                    .build()

                val responseStr = okHttpClient.newCall(request).execute()
                    .use { it.body?.string().orEmpty() }

                val root = try {
                    JsonParser.parseString(responseStr.trim())
                } catch (e: JsonSyntaxException) {
                    throw IllegalStateException("Invalid JSON from server: ${responseStr.take(120)}")
                }

                if (!root.isJsonObject) {
                    val preview = if (root.isJsonPrimitive) root.asString else responseStr.take(120)
                    throw IllegalStateException("Unexpected server response: $preview")
                }

                val json    = root.asJsonObject
                val success = json.get("success")?.asBoolean ?: false
                val message = json.get("message")?.asString.orEmpty()

                if (success && message.contains("allot", ignoreCase = true)) {
                    val qty = Regex("""quantity\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .find(message)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
                    ResultStatus.Allotted(qty, message)
                } else {
                    ResultStatus.NotAllotted(message.ifEmpty { "Not allotted" })
                }
            }.getOrElse { e ->
                ResultStatus.Error(e.message ?: "Unknown error")
            }
        }
}