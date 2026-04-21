package com.share_manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Room Entity ───────────────────────────────────────────────────────────────

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val boid: String,           // 16-digit BOID
    val dp: String,             // depository participant code e.g. "13200"
    val username: String,       // same as boid in most cases
    val password: String,
    val crn: String = "",
    val transactionPin: String = "",
    val isForeignEmployment: Boolean = false
)

// ── Auth ──────────────────────────────────────────────────────────────────────

data class AuthRequest(
    val clientId: Int,
    val username: String,
    val password: String
)

// ── Bank / Branch ─────────────────────────────────────────────────────────────

data class BankInfo(
    val id: Int,
    val code: String,
    val name: String
)

data class BranchInfo(
    val accountBranchId: Int,
    val accountNumber: String,
    val accountTypeId: Int,
    val accountTypeName: String,
    val branchName: String,
    val id: Int,            // customerId
    var bankId: Int = 0     // filled in after bank fetch
)

// ── Open Issues ───────────────────────────────────────────────────────────────

data class ApplicableIssueRequest(
    val filterFieldParams: List<FilterFieldParam> = listOf(
        FilterFieldParam("companyIssue.companyISIN.script", alias = "Scrip"),
        FilterFieldParam("companyIssue.companyISIN.company.name", alias = "Company Name"),
        FilterFieldParam("companyIssue.assignedToClient.name", value = "", alias = "Issue Manager")
    ),
    val filterDateParams: List<FilterDateParam> = listOf(
        FilterDateParam("minIssueOpenDate"),
        FilterDateParam("maxIssueCloseDate")
    ),
    val page: Int = 1,
    val size: Int = 200,
    val searchRoleViewConstants: String = "VIEW_APPLICABLE_SHARE"
)

data class FilterFieldParam(
    val key: String,
    val alias: String = "",
    val value: String = ""
)

data class FilterDateParam(
    val key: String,
    val condition: String = "",
    val alias: String = "",
    val value: String = ""
)

data class IssueListResponse(
    val `object`: List<IssueJson>,
    val totalCount: Int = 0
)

data class IssueJson(
    val companyShareId: Int,
    val companyName: String,
    val scrip: String,
    val shareTypeName: String,      // "IPO", "FPO", "RIGHT", etc.
    val shareGroupName: String,     // "Ordinary Shares", etc.
    val subGroup: String?,
    val statusName: String?,
    val action: String?,            // "edit" means already applied
    val issueOpenDate: String?,
    val issueCloseDate: String?
) {
    val isApplied: Boolean get() = action == "edit"
    val isOrdinaryShares: Boolean get() = shareGroupName == "Ordinary Shares"
    val isUnappliedOrdinary: Boolean get() = isOrdinaryShares && !isApplied
    val isIPO: Boolean get() = shareTypeName == "IPO"
    val isFPO: Boolean get() = shareTypeName == "FPO"
    val statusDisplay: String get() = if (isApplied) "Applied" else "Not Applied"
}

// ── Apply ─────────────────────────────────────────────────────────────────────

data class ApplyRequest(
    val demat: String,
    val boid: String,
    val accountNumber: String,
    val customerId: Int,
    val accountBranchId: Int,
    val accountTypeId: Int,
    val appliedKitta: String,
    val crnNumber: String,
    val transactionPIN: String,
    val companyShareId: String,
    val bankId: Int
)

data class ApplyResponse(
    val message: String?,
    val status: String?
)

// ── Reports ───────────────────────────────────────────────────────────────────

data class ReportRequest(
    val filterFieldParams: List<FilterFieldParam> = listOf(
        FilterFieldParam("companyShare.companyIssue.companyISIN.script", alias = "Scrip"),
        FilterFieldParam("companyShare.companyIssue.companyISIN.company.name", alias = "Company Name")
    ),
    val page: Int = 1,
    val size: Int = 200,
    val searchRoleViewConstants: String = "VIEW_APPLICANT_FORM_COMPLETE",
    val filterDateParams: List<FilterDateParam>
)

data class ReportItem(
    val applicantFormId: Int,
    val companyName: String,
    val scrip: String?,
    val appliedKitta: Int,
    val statusName: String?,
    var allotmentStatus: String = "N/A",
    var allottedKitta: Int = 0
)

data class ReportDetailResponse(
    val statusName: String?,
    val allottedKitta: Int?
)

// ── IPO Result (public endpoint) ──────────────────────────────────────────────

data class CompanyShareListResponse(
    val success: Boolean,
    val message: String,
    val body: CompanyShareBody?
)

data class CompanyShareBody(
    val companyShareList: List<CompanyShare>
)

data class CompanyShare(
    val id: Int,
    val name: String,
    val scrip: String,
    val isFileUploaded: Boolean
)

data class ResultCheckRequest(
    val companyShareId: Int,
    val boid: String,
    val userCaptcha: String = "28157",
    val captchaIdentifier: String = "b12025e7-12bc-4c87-8919-54b68c03f780"
)

data class ResultCheckResponse(
    val success: Boolean,
    val message: String,
    val body: Any?
)

// ── UI State ──────────────────────────────────────────────────────────────────

data class AccountResult(
    val account: Account,
    val status: ResultStatus = ResultStatus.Pending
)

sealed class ResultStatus {
    object Pending : ResultStatus()
    object Loading : ResultStatus()
    data class Allotted(val quantity: Int, val message: String) : ResultStatus()
    data class NotAllotted(val message: String) : ResultStatus()
    data class Error(val message: String) : ResultStatus()
}

// ── Apply UI State ────────────────────────────────────────────────────────────

sealed class ApplyStatus {
    object Idle : ApplyStatus()
    object Loading : ApplyStatus()
    data class Success(val message: String) : ApplyStatus()
    data class Skipped(val reason: String) : ApplyStatus()
    data class Failed(val message: String) : ApplyStatus()
}

data class AccountApplyResult(
    val account: Account,
    val status: ApplyStatus = ApplyStatus.Idle
)
