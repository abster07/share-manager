package com.share_manager.data.repository

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
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
    private val captchaValue      = "28157"
    private val captchaIdentifier = "b12025e7-12bc-4c87-8919-54b68c03f780"

    // ── Accounts ──────────────────────────────────────────────────────────────

    val accounts: Flow<List<Account>> = dao.getAllAccounts().map { list ->
        list.map { acc ->
            acc.copy(transactionPin = EncryptionUtil.decrypt(acc.transactionPin))
        }
    }

    suspend fun saveAccount(account: Account) {
        dao.insertAccount(account.copy(transactionPin = EncryptionUtil.encrypt(account.transactionPin)))
    }

    suspend fun updateAccount(account: Account) {
        dao.updateAccount(account.copy(transactionPin = EncryptionUtil.encrypt(account.transactionPin)))
    }

    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Safely parses a raw response string to JsonObject.
     * Throws [IllegalStateException] with a readable message if the server
     * returns a primitive, array, or anything other than a JSON object —
     * which is the root cause of the "Expected JsonObject but was
     * JsonPrimitive; at path $" crash.
     */
    private fun parseJsonObject(responseStr: String): JsonObject {
        val element: JsonElement = try {
            gson.fromJson(responseStr.trim(), JsonElement::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Server returned non-JSON response: ${responseStr.take(200)}")
        }

        if (!element.isJsonObject) {
            throw IllegalStateException(
                "Expected a JSON object from server but got: ${responseStr.take(200)}"
            )
        }

        return element.asJsonObject
    }

    // ── Company shares ────────────────────────────────────────────────────────

    /**
     * GET https://iporesult.cdsc.com.np/result/companyShares/fileUploaded
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

            val json = parseJsonObject(responseStr)

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

                val json = parseJsonObject(responseStr)

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