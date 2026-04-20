package com.share_manager.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.share_manager.data.db.AccountDao
import com.share_manager.data.model.*
import com.share_manager.network.IpoApiService
import com.share_manager.util.EncryptionUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    // ── IPO Companies — raw OkHttp to avoid Gson strictness issues ────────────

    suspend fun getCompanyShares(): Result<List<CompanyShare>> = runCatching {
        val request = Request.Builder()
            .url("https://iporesult.cdsc.com.np/result/companyShares/fileUploaded")
            .addHeader("Accept", "application/json, text/plain, */*")
            .get()
            .build()

        val responseStr = okHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
        val json = gson.fromJson(responseStr.trim(), JsonObject::class.java)
        val list = json.getAsJsonObject("body")
            ?.getAsJsonArray("companyShareList")
            ?: return@runCatching emptyList()

        list.map { el ->
            val obj = el.asJsonObject
            CompanyShare(
                id = obj.get("id").asInt,
                name = obj.get("name").asString,
                scrip = obj.get("scrip").asString,
                isFileUploaded = obj.get("isFileUploaded").asBoolean
            )
        }.filter { it.isFileUploaded }
    }

    // ── Result Check — raw OkHttp ─────────────────────────────────────────────

    suspend fun checkResult(companyShareId: Int, boid: String): ResultStatus = runCatching {
        val payload = """{"companyShareId":$companyShareId,"boid":"$boid","userCaptcha":"28157","captchaIdentifier":"b12025e7-12bc-4c87-8919-54b68c03f780"}"""
        val body = payload.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://iporesult.cdsc.com.np/result/result/check")
            .addHeader("Accept", "application/json, text/plain, */*")
            .addHeader("Authorization", "null")
            .post(body)
            .build()

        val responseStr = okHttpClient.newCall(request).execute().use { it.body?.string() ?: "" }
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
        ResultStatus.Error(e.message ?: "Unknown error")
    }
}
