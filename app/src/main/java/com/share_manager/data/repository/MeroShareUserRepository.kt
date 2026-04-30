package com.share_manager.data.repository

import com.share_manager.data.model.*
import com.share_manager.network.ApiService
import com.share_manager.util.EncryptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the Python MeroShare class / UserSession lifecycle:
 *
 *   1. login(account)           → POST /auth/ → Authorization header token
 *   2. getBranchInfo(token)     → GET /capital/ + GET /bank/{id}/branchList
 *   3. getOpenIssues(token)     → POST /companyShare/currentIssue
 *   4. canApply(token, ...)     → GET /active/{cid}?demat=...
 *   5. applyForAccount(...)     → POST /applicantForm/
 *   6. generateReports(...)     → POST /applicantForm/active/search/ + detail
 */
@Singleton
class MeroShareUserRepository @Inject constructor(
    private val api: ApiService,
    private val capitalsRepository: CapitalsRepository
) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Logs in and returns the Bearer token from the Authorization response header.
     * Mirrors: Python __loginRequest__()
     *
     * Account fields used:
     *   account.dp       → broker/DP code e.g. "13200"  → converted to clientId
     *   account.username → numeric BOID used as MeroShare login username
     *   account.password → decrypted password (EncryptionUtil.decrypt before call)
     */
    suspend fun login(account: Account): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = capitalsRepository.getClientId(account.dp)
                ?: throw IllegalArgumentException("Unknown DP code: ${account.dp}")

            val response = api.login(
                AuthRequest(
                    clientId = clientId,
                    username = account.username,
                    password = EncryptionUtil.decrypt(account.password)
                )
            )
            if (!response.isSuccessful) {
                throw Exception("Login failed for ${account.name}: HTTP ${response.code()}")
            }
            response.headers()["Authorization"]
                ?: throw Exception("No Authorization header in login response for ${account.name}")
        }
    }

    // ── Bank / Branch ─────────────────────────────────────────────────────────

    /**
     * Fetches the first available bank and its branch info for the logged-in user.
     * Mirrors: Python getBanks() + getBranchInfo()
     */
    suspend fun fetchBranchInfo(token: String, accountName: String): Result<BranchInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val banksResp = api.getBanks(token)
                if (!banksResp.isSuccessful || banksResp.body().isNullOrEmpty()) {
                    throw Exception("No banks found for user: $accountName")
                }
                val bank = banksResp.body()!!.first()

                val branchResp = api.getBranchInfo(bank.id, token)
                if (!branchResp.isSuccessful || branchResp.body().isNullOrEmpty()) {
                    throw Exception("Unable to fetch branch info for user: $accountName")
                }

                branchResp.body()!!.first().also { it.bankId = bank.id }
            }
        }

    // ── Open Issues ───────────────────────────────────────────────────────────

    /**
     * Returns all currently open/applicable share issues.
     * Mirrors: Python getCurrentIssues()
     */
    suspend fun getOpenIssues(token: String): Result<List<IssueJson>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getApplicableIssues(ApplicableIssueRequest(), token)
                if (!response.isSuccessful) {
                    throw Exception("Error fetching open issues: HTTP ${response.code()}")
                }
                response.body()?.`object` ?: emptyList()
            }
        }

    // ── Can Apply ─────────────────────────────────────────────────────────────

    /**
     * Checks whether this account can apply for the given companyShareId.
     * Mirrors: Python canApply() — demat = "130" + dp + username
     */
    suspend fun canApply(token: String, account: Account, companyShareId: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val demat = buildDemat(account)
                val response = api.canApply(companyShareId, demat, token)
                response.isSuccessful &&
                    response.body()?.get("message") == "Customer can apply."
            }.getOrDefault(false)
        }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * Full apply flow for one account:
     *   login → branch info → find open issue → can-apply check → POST apply
     *
     * Mirrors the Python __main__ apply block.
     */
    suspend fun applyForAccount(
        account: Account,
        companyShareId: Int,
        numberOfShares: Int = 10
    ): ApplyStatus = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Login
            val token = login(account).getOrThrow()

            // 2. Branch info
            val branch = fetchBranchInfo(token, account.name).getOrThrow()

            // 3. Verify the issue exists and is unapplied
            val issues = getOpenIssues(token).getOrThrow()
            val issue = issues.firstOrNull {
                it.isUnappliedOrdinary && it.companyShareId == companyShareId
            } ?: return@runCatching ApplyStatus.Skipped(
                "Unapplied issue not found for companyShareId=$companyShareId"
            )

            // 4. Can-apply check
            if (!canApply(token, account, companyShareId)) {
                return@runCatching ApplyStatus.Skipped(
                    "Server says cannot apply for ${account.name}"
                )
            }

            // 5. Submit application
            val payload = ApplyRequest(
                demat          = buildDemat(account),
                boid           = account.boid,          // 16-digit BOID
                accountNumber  = branch.accountNumber,
                customerId     = branch.id,
                accountBranchId = branch.accountBranchId,
                accountTypeId  = branch.accountTypeId,
                appliedKitta   = numberOfShares.toString(),
                crnNumber      = account.crn,
                transactionPIN = EncryptionUtil.decrypt(account.transactionPin),
                companyShareId = companyShareId.toString(),
                bankId         = branch.bankId
            )

            val r = api.applyForShare(payload, token)
            if (r.isSuccessful) {
                ApplyStatus.Success(r.body()?.message ?: "Applied successfully")
            } else {
                ApplyStatus.Failed("HTTP ${r.code()}: ${r.errorBody()?.string()}")
            }
        }.getOrElse { e ->
            ApplyStatus.Failed(e.message ?: "Unknown error")
        }
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    /**
     * Fetches the last 2 months of applications and enriches with allotment detail.
     * Mirrors: Python getApplicationReport() + getFormDetails()
     */
    suspend fun generateReports(account: Account): Result<List<ReportItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = login(account).getOrThrow()

                val sdf   = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val today = sdf.format(Date())
                val cal   = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }
                val from  = sdf.format(cal.time)

                val request = ReportRequest(
                    filterDateParams = listOf(
                        FilterDateParam(key = "appliedDate", value = ""),
                        FilterDateParam(key = "appliedDate", value = "")
                    )
                )

                val response = api.getApplicationReports(request, token)
                if (!response.isSuccessful) {
                    throw Exception("Error fetching reports: HTTP ${response.code()}")
                }

                val rawItems = response.body()?.`object` ?: emptyList()

                rawItems.map { issue ->
                    val item = ReportItem(
                        applicantFormId = issue.companyShareId,
                        companyName     = issue.companyName,
                        scrip           = issue.scrip,
                        appliedKitta    = 0,
                        statusName      = issue.statusName
                    )

                    // Enrich with allotment detail where applicable
                    if (issue.statusName in listOf("TRANSACTION_SUCCESS", "APPROVED")) {
                        runCatching {
                            val detail = api.getAllotmentDetail(item.applicantFormId, token)
                            if (detail.isSuccessful) {
                                item.allotmentStatus = detail.body()?.statusName ?: "N/A"
                                item.allottedKitta   = detail.body()?.allottedKitta ?: 0
                            }
                        }
                    }
                    item
                }
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Mirrors Python: f"130{self.account.dp}{self.account.username}"
     */
    fun buildDemat(account: Account) = "130${account.dp}${account.username}"
}
