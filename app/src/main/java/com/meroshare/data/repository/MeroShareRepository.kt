package com.meroshare.data.repository

import com.meroshare.data.db.AccountDao
import com.meroshare.data.model.*
import com.meroshare.network.IpoApiService
import com.meroshare.util.EncryptionUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeroShareRepository @Inject constructor(
    private val api: IpoApiService,
    private val dao: AccountDao
) {

    // ── Accounts ──────────────────────────────────────────────────────────────

    val accounts: Flow<List<Account>> = dao.getAllAccounts().map { list ->
        list.map { acc ->
            acc.copy(
                transactionPin = EncryptionUtil.decrypt(acc.transactionPin)
            )
        }
    }

    suspend fun saveAccount(account: Account) {
        val encrypted = account.copy(
            transactionPin = EncryptionUtil.encrypt(account.transactionPin)
        )
        dao.insertAccount(encrypted)
    }

    suspend fun updateAccount(account: Account) {
        val encrypted = account.copy(
            transactionPin = EncryptionUtil.encrypt(account.transactionPin)
        )
        dao.updateAccount(encrypted)
    }

    suspend fun deleteAccount(account: Account) = dao.deleteAccount(account)

    // ── IPO Companies ─────────────────────────────────────────────────────────

    suspend fun getCompanyShares(): Result<List<CompanyShare>> = runCatching {
        val response = api.getCompanyShares()
        if (response.isSuccessful) {
            response.body()?.body?.companyShareList
                ?.filter { it.isFileUploaded }
                ?: emptyList()
        } else {
            throw Exception("Failed to fetch companies: ${response.code()}")
        }
    }

    // ── Result Check ──────────────────────────────────────────────────────────

    suspend fun checkResult(
        companyShareId: Int,
        boid: String
    ): ResultStatus = runCatching {
        val request = ResultCheckRequest(
            companyShareId = companyShareId,
            boid = boid
        )
        val response = api.checkResult(request)
        if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                val message = body.message ?: ""
                if (message.contains("Alloted", ignoreCase = true) ||
                    message.contains("Allotted", ignoreCase = true)
                ) {
                    // Parse quantity from message: "Congratulation Alloted !!! Alloted quantity : 10"
                    val qty = Regex("""quantity\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
                        .find(message)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    ResultStatus.Allotted(qty, message)
                } else {
                    ResultStatus.NotAllotted(message)
                }
            } else {
                ResultStatus.NotAllotted(body?.message ?: "Not allotted")
            }
        } else {
            ResultStatus.Error("Server error: ${response.code()}")
        }
    }.getOrElse { e ->
        ResultStatus.Error(e.message ?: "Unknown error")
    }
}
