package com.share_manager.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.share_manager.data.db.AccountDao
import com.share_manager.data.model.*
import com.share_manager.util.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
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

    // ── Base URLs ─────────────────────────────────────────────────────────────
    private val baseUrl    = "https://iporesult.cdsc.com.np"
    private val refererUrl = "https://iporesult.cdsc.com.np/"

    // ── Browser-spoof header extension ───────────────────────────────────────
    //
    // Cloudflare bot detection checks multiple signals simultaneously:
    //   1. User-Agent — must look like a real Chrome/mobile build
    //   2. Accept / Accept-Language / Accept-Encoding — Chrome's exact defaults
    //   3. Sec-Fetch-* — only real browsers send these
    //   4. Sec-CH-UA*  — Client Hints, Chrome 90+ sends these on every request
    //   5. Origin + Referer — must be same-origin for XHR/fetch requests
    //   6. Header ORDER — Chrome sends headers in a deterministic order;
    //                     OkHttp preserves insertion order, so we add them
    //                     in the same sequence Chrome uses
    //   7. Cookie       — cf_clearance / __cf_bm cookies from the warm-up
    //                     GET must be replayed; handled by the CookieJar
    //                     wired in AppModule
    //
    private fun Request.Builder.addBrowserHeaders(
        acceptJson: Boolean = true
    ): Request.Builder = this
        .header(                        // .header() replaces; addHeader() appends
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        )
        .addHeader(
            "Accept",
            if (acceptJson) "application/json, text/plain, */*"
            else            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        )
        .addHeader("Accept-Language",  "en-US,en;q=0.9,ne;q=0.8")
        .addHeader("Accept-Encoding",  "gzip, deflate, br")
        .addHeader("Connection",       "keep-alive")
        .addHeader("Origin",           baseUrl)
        .addHeader("Referer",          refererUrl)
        .addHeader("Sec-Fetch-Dest",   "empty")
        .addHeader("Sec-Fetch-Mode",   "cors")
        .addHeader("Sec-Fetch-Site",   "same-origin")
        .addHeader(
            "Sec-CH-UA",
            "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\""
        )
        .addHeader("Sec-CH-UA-Mobile",   "?1")
        .addHeader("Sec-CH-UA-Platform", "\"Android\"")

    // ── Cloudflare warm-up ────────────────────────────────────────────────────
    //
    // Cloudflare issues __cf_bm (bot management) and cf_clearance cookies on
    // the first HTML page load.  Subsequent XHR/fetch calls to the same origin
    // must carry those cookies or CF returns 403/503.
    //
    // Strategy:
    //   1. Do a plain browser-like GET of the site root (HTML, not JSON).
    //   2. OkHttp's CookieJar (set up in AppModule) stores whatever CF sets.
    //   3. All later API requests automatically include those cookies.
    //   4. We only do this once per process lifetime (cfWarmedUp flag).
    //
    @Volatile private var cfWarmedUp = false

    private suspend fun ensureCfCookies() {
        if (cfWarmedUp) return
        withContext(Dispatchers.IO) {
            runCatching {
                val warmReq = Request.Builder()
                    .url(refererUrl)
                    // Navigate-style headers (different Sec-Fetch-* values)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .addHeader(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                    )
                    .addHeader("Accept-Language",          "en-US,en;q=0.9,ne;q=0.8")
                    .addHeader("Accept-Encoding",          "gzip, deflate, br")
                    .addHeader("Connection",               "keep-alive")
                    .addHeader("Upgrade-Insecure-Requests","1")
                    .addHeader("Sec-Fetch-Dest",           "document")
                    .addHeader("Sec-Fetch-Mode",           "navigate")
                    .addHeader("Sec-Fetch-Site",           "none")
                    .addHeader("Sec-Fetch-User",           "?1")
                    .addHeader(
                        "Sec-CH-UA",
                        "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\""
                    )
                    .addHeader("Sec-CH-UA-Mobile",   "?1")
                    .addHeader("Sec-CH-UA-Platform", "\"Android\"")
                    .get()
                    .build()

                okHttpClient.newCall(warmReq).execute().use { /* cookies stored by CookieJar */ }

                // Small pause — CF sometimes redirects once before settling
                delay(800)
            }
            cfWarmedUp = true
        }
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    val accounts: Flow<List<Account>> = dao.getAllAccounts().map { list ->
        list.map { acc ->
            acc.copy(
                password       = EncryptionUtil.decrypt(acc.password),
                transactionPin = EncryptionUtil.decrypt(acc.transactionPin)
            )
        }
    }

    suspend fun saveAccount(account: Account) {
        dao.insertAccount(
            account.copy(
                password       = EncryptionUtil.encrypt(account.password),
                transactionPin = EncryptionUtil.encrypt(account.transactionPin)
            )
        )
    }

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

    suspend fun getCompanyShares(): Result<List<CompanyShare>> = withContext(Dispatchers.IO) {
        runCatching {
            ensureCfCookies()

            val request = Request.Builder()
                .url("$baseUrl/result/companyShares/fileUploaded")
                .addBrowserHeaders(acceptJson = true)
                .get()
                .build()

            val responseStr = okHttpClient.newCall(request).execute()
                .use { it.body?.string().orEmpty() }

            val json    = parseJsonObject(responseStr)
            val success = json.get("success")?.asBoolean ?: false
            if (!success) {
                throw IllegalStateException(
                    json.get("message")?.asString ?: "Unknown error from server"
                )
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

    suspend fun checkResult(companyShareId: Int, boid: String): ResultStatus =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureCfCookies()

                val payload = gson.toJson(
                    ResultCheckRequest(
                        companyShareId    = companyShareId,
                        boid              = boid,
                        userCaptcha       = captchaValue,
                        captchaIdentifier = captchaIdentifier
                    )
                )

                val request = Request.Builder()
                    .url("$baseUrl/result/result/check")
                    .addBrowserHeaders(acceptJson = true)
                    .addHeader("Authorization",  "null")
                    .addHeader("Content-Type",   "application/json")
                    .post(payload.toRequestBody("application/json".toMediaType()))
                    .build()

                val responseStr = okHttpClient.newCall(request).execute()
                    .use { it.body?.string().orEmpty() }

                val json    = parseJsonObject(responseStr)
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

    // ── JSON helpers ──────────────────────────────────────────────────────────

    /**
     * Parses [raw] as a JsonObject.
     * Throws [IllegalStateException] (caught by [runCatching] above) with a
     * human-readable preview when the root is not an object — e.g. Cloudflare
     * block pages, plain-text errors, or bare JSON primitives.
     */
    private fun parseJsonObject(raw: String): com.google.gson.JsonObject {
        val root = try {
            JsonParser.parseString(raw.trim())
        } catch (e: JsonSyntaxException) {
            throw IllegalStateException("Invalid JSON from server: ${raw.take(120)}")
        }
        if (!root.isJsonObject) {
            val preview = if (root.isJsonPrimitive) root.asString else raw.take(200)
            throw IllegalStateException("Unexpected server response: $preview")
        }
        return root.asJsonObject
    }
}
