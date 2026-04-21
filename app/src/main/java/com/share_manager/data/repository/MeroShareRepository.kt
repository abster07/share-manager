package com.share_manager.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.share_manager.data.db.AccountDao
import com.share_manager.data.model.*
import com.share_manager.network.IpoApiService
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
    private val api: IpoApiService,
    private val dao: AccountDao,
    private val okHttpClient: OkHttpClient
) {

    private val gson = Gson()

    // Shared debug log — appended by both network calls
    private val _debugLog = StringBuilder()
    val debugLog: String get() = _debugLog.toString()

    private fun log(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
            .format(java.util.Date())
        _debugLog.appendLine("[$ts] $msg")
        android.util.Log.d("MeroShare", msg)
    }

    fun clearLog() { _debugLog.clear() }

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

    // ── IPO Companies ─────────────────────────────────────────────────────────

    suspend fun getCompanyShares(): Result<List<CompanyShare>> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://iporesult.cdsc.com.np/result/companyShares/fileUploaded"
            log("GET $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Origin", "https://iporesult.cdsc.com.np")
                .addHeader("Referer", "https://iporesult.cdsc.com.np/")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .addHeader("sec-ch-ua", "\"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                .addHeader("sec-ch-ua-mobile", "?1")
                .addHeader("sec-ch-ua-platform", "\"Android\"")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .get()
                .build()

            val response = okHttpClient.newCall(request).execute()
            val code = response.code
            val rawBody = response.use { it.body?.string() ?: "" }

            log("HTTP $code  |  body length=${rawBody.length}")
            if (rawBody.length <= 2000) log("BODY: $rawBody")
            else log("BODY (first 2000): ${rawBody.take(2000)}")

            if (!response.isSuccessful) {
                throw Exception("HTTP $code: ${response.message}")
            }

            val trimmed = rawBody.trim()
            if (trimmed.isEmpty()) throw Exception("Empty response body")

            val json = try {
                gson.fromJson(trimmed, JsonObject::class.java)
            } catch (e: Exception) {
                log("JSON parse error: ${e.message}")
                throw Exception("JSON parse failed: ${e.message}")
            }

            log("JSON keys at root: ${json.keySet()}")

            // Try nested path: body.companyShareList
            val bodyObj = json.getAsJsonObject("body")
            log("body keys: ${bodyObj?.keySet()}")

            val listArray = bodyObj?.getAsJsonArray("companyShareList")
                ?: run {
                    // Fallback: maybe the array is directly at root
                    json.getAsJsonArray("companyShareList")
                }

            if (listArray == null) {
                log("ERROR: could not find companyShareList in response")
                throw Exception("companyShareList not found. Root keys: ${json.keySet()}")
            }

            log("companyShareList size = ${listArray.size()}")

            val all = listArray.mapIndexed { i, el ->
                try {
                    val obj = el.asJsonObject
                    CompanyShare(
                        id = obj.get("id").asInt,
                        name = obj.get("name").asString,
                        scrip = obj.get("scrip")?.asString ?: "",
                        isFileUploaded = obj.get("isFileUploaded")?.asBoolean ?: false
                    )
                } catch (e: Exception) {
                    log("Parse error at index $i: ${e.message} — element: $el")
                    null
                }
            }.filterNotNull()

            val filtered = all.filter { it.isFileUploaded }
            log("Total parsed=${all.size}  fileUploaded=${filtered.size}")
            filtered
        }.onFailure { e ->
            log("FAILURE: ${e::class.simpleName}: ${e.message}")
        }
    }

    // ── Result Check ──────────────────────────────────────────────────────────

    suspend fun checkResult(companyShareId: Int, boid: String): ResultStatus =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = """{"companyShareId":$companyShareId,"boid":"$boid","userCaptcha":"41931","captchaIdentifier":"1b408643-171a-4f30-a515-559f11512a02"}"""
                log("POST check | companyShareId=$companyShareId boid=${boid.takeLast(4).padStart(boid.length, '*')}")

                val body = payload.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://iporesult.cdsc.com.np/result/result/check")
                    .addHeader("Accept", "application/json, text/plain, */*")
                    .addHeader("Accept-Language", "en-US,en;q=0.9")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Origin", "https://iporesult.cdsc.com.np")
                    .addHeader("Referer", "https://iporesult.cdsc.com.np/")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    .addHeader("sec-ch-ua", "\"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                    .addHeader("sec-ch-ua-mobile", "?1")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "same-origin")
                    .addHeader("Authorization", "null")
                    .post(body)
                    .build()

                val responseStr = okHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
                log("check response: $responseStr")

                val json = gson.fromJson(responseStr.trim(), JsonObject::class.java)
                val success = json.get("success")?.asBoolean ?: false
                val message = json.get("message")?.asString ?: ""

                if (success && (message.contains("Alloted", ignoreCase = true) ||
                                message.contains("Allotted", ignoreCase = true))) {
                    val qty = Regex("""quantity\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    ResultStatus.Allotted(qty, message)
                } else {
                    ResultStatus.NotAllotted(message.ifEmpty { "Not allotted" })
                }
            }.getOrElse { e ->
                log("checkResult ERROR: ${e.message}")
                ResultStatus.Error(e.message ?: "Unknown error")
            }
        }
}
