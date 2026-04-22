package com.share_manager.data.repository

import com.share_manager.data.model.*
import com.share_manager.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors the Python script's UserSession class.
 *
 * Lifecycle per account:
 *   1. login(account)              → stores token
 *   2. fetchBranchInfo(token, dp)  → stores bank/branch
 *   3. getOpenIssues / apply / generateReports
 *
 * Each public function is self-contained — it logs in, does the work, and
 * returns a clean result so the ViewModel never has to manage raw tokens.
 */
@Singleton
class MeroShareUserRepository @Inject constructor(
    private val api: ApiService,
    private val capitalsRepository: CapitalsRepository
) {

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Logs in and returns the Bearer token.
     * Mirrors: UserSession.create_session()
     */
    suspend fun login(account: Account): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = capitalsRepository.getClientId(account.dp)
                ?: throw IllegalArgumentException("Unknown DP code: ${account.dp}")

            val response = api.login(AuthRequest(clientId, account.username, account.password))
            if (!response.isSuccessful) {
                throw Exception("Login failed for ${account.username}: HTTP ${response.code()}")
            }

            response.headers()["Authorization"]
                ?: throw Exception("No Authorization header in login response for ${account.username}")
        }
    }

    // ── Bank / Branch ─────────────────────────────────────────────────────────

    /**
     * Mirrors: UserSession.set_branch_info() + UserSession.bank_info()
     */
    suspend fun fetchBranchInfo(token: String, accountUser: String): Result<BranchInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val banksResp = api.getBanks(token)
                if (!banksResp.isSuccessful || banksResp.body().isNullOrEmpty()) {
                    throw Exception("No banks found for user: $accountUser")
                }
                val bank = banksResp.body()!!.first()

                val branchResp = api.getBranchInfo(bank.id, token)
                if (!branchResp.isSuccessful || branchResp.body().isNullOrEmpty()) {
                    throw Exception("Unable to fetch branch info for user: $accountUser")
                }

                branchResp.body()!!.first().also { it.bankId = bank.id }
            }
        }

    // ── Open Issues ───────────────────────────────────────────────────────────

    /**
     * Mirrors: UserSession.open_issues()
     * Returns all currently applicable issues for this account's token.
     */
    suspend fun getOpenIssues(token: String): Result<List<IssueJson>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getApplicableIssues(ApplicableIssueRequest(), token)
            if (!response.isSuccessful) throw Exception("Error fetching open issues: HTTP ${response.code()}")
            response.body()?.`object` ?: emptyList()
        }
    }

    // ── Can Apply ─────────────────────────────────────────────────────────────

    /**
     * Mirrors: UserSession.can_apply()
     * demat = "130" + dp + username  (from Python: f"130{self.account.dp}{self.account.username}")
     */
    suspend fun canApply(token: String, account: Account, companyShareId: Int): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val demat = buildDemat(account)
                val response = api.canApply(companyShareId, demat, token)
                response.isSuccessful && response.body()?.get("message") == "Customer can apply."
            }.getOrDefault(false)
        }

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * Full apply flow for one account — mirrors the Python __main__ apply block.
     * Handles: login → branch info → can-apply check → POST apply
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

            // 3. Verify issue exists and is unapplied
            val issues = getOpenIssues(token).getOrThrow()
            val issue = issues.firstOrNull {
                it.isUnappliedOrdinary && it.companyShareId == companyShareId
            } ?: return@runCatching ApplyStatus.Skipped(
                "Unapplied issue not found for companyShareId=$companyShareId"
            )

            // 4. Can-apply check
            if (!canApply(token, account, companyShareId)) {
                return@runCatching ApplyStatus.Skipped("Server says cannot apply for ${account.name}")
            }

            // 5. Apply
            val payload = ApplyRequest(
                demat = buildDemat(account),
                boid = account.username,
                accountNumber = branch.accountNumber,
                customerId = branch.id,
                accountBranchId = branch.accountBranchId,
                accountTypeId = branch.accountTypeId,
                appliedKitta = numberOfShares.toString(),
                crnNumber = account.crn,
                transactionPIN = account.transactionPin,
                companyShareId = companyShareId.toString(),
                bankId = branch.bankId
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
     * Mirrors: UserSession.generate_reports() + with_allotment_status()
     * Fetches last 2 months of applications and enriches with allotment status.
     */
    suspend fun generateReports(account: Account): Result<List<ReportItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = login(account).getOrThrow()

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val today = sdf.format(Date())
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -2) }
                val twoMonthsAgo = sdf.format(cal.time)

                val request = ReportRequest(
                    filterDateParams = listOf(
                        FilterDateParam(
                            key = "appliedDate",
                            value = "BETWEEN '$twoMonthsAgo' AND '$today'"
                        )
                    )
                )

                val response = api.getApplicationReports(request, token)
                if (!response.isSuccessful) throw Exception("Error fetching reports: HTTP ${response.code()}")

                // The reports endpoint re-uses IssueListResponse shape for its object list.
                // We parse only the fields we need via ReportItem mapping.
                val rawItems = response.body()?.`object` ?: emptyList()

                rawItems.map { issue ->
                    val item = ReportItem(
                        applicantFormId = issue.companyShareId, // applicantFormId maps here
                        companyName = issue.companyName,
                        scrip = issue.scrip,
                        appliedKitta = 0,
                        statusName = issue.statusName
                    )

                    // Enrich with allotment detail if status warrants it
                    if (issue.statusName in listOf("TRANSACTION_SUCCESS", "APPROVED")) {
                        runCatching {
                            val detail = api.getAllotmentDetail(item.applicantFormId, token)
                            if (detail.isSuccessful) {
                                item.allotmentStatus = detail.body()?.statusName ?: "N/A"
                                item.allottedKitta = detail.body()?.allottedKitta ?: 0
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
