package com.share_manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Room Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val dp: String = "",             // DP/broker code e.g. "13200"
    val username: String = "",       // numeric BOID/client id used for login
    val password: String = "",       // stored AES-256-GCM encrypted
    val boid: String = "",           // 16-digit BOID for result checks
    val crn: String = "",            // optional CRN
    val transactionPin: String = "", // stored AES-256-GCM encrypted
    val isForeignEmployment: Boolean = false
)

// ── Auth ──────────────────────────────────────────────────────────────────────

data class AuthRequest(
    val clientId: Int,
    val username: String,
    val password: String
)

// ── Bank / Branch DTOs ────────────────────────────────────────────────────────

data class BankDto(
    val id: Int,
    val name: String
)

data class BranchInfo(
    val id: Int,
    val accountNumber: String,
    val accountBranchId: Int,
    val accountTypeId: Int,
    var bankId: Int = 0             // set after fetch
)

// ── Issue / Apply DTOs ────────────────────────────────────────────────────────

data class ApplicableIssueRequest(
    val filterFieldParams: List<Map<String, String>> = listOf(
        mapOf("key" to "companyIssue.companyISIN.script", "alias" to "Scrip"),
        mapOf("key" to "companyIssue.companyISIN.company.name", "alias" to "Company Name"),
        mapOf("key" to "companyIssue.assignedToClient.name", "value" to "", "alias" to "Issue Manager")
    ),
    val page: Int = 1,
    val size: Int = 200,
    val searchRoleViewConstants: String = "VIEW_OPEN_SHARE",
    val filterDateParams: List<Map<String, String>> = listOf(
        mapOf("key" to "minIssueOpenDate", "condition" to "", "alias" to "", "value" to ""),
        mapOf("key" to "maxIssueCloseDate", "condition" to "", "alias" to "", "value" to "")
    )
)

data class IssueListResponse(
    val `object`: List<IssueJson>
)

data class IssueJson(
    val companyShareId: Int,
    val companyName: String,
    val scrip: String,
    val shareTypeName: String,
    val shareGroupName: String,
    val subGroup: String,
    val statusName: String,
    val isUnappliedOrdinary: Boolean = false   // derived from statusName if needed
)

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
    val message: String
)

sealed class ApplyStatus {
    object Loading : ApplyStatus()
    data class Success(val message: String) : ApplyStatus()
    data class Skipped(val reason: String) : ApplyStatus()
    data class Failed(val reason: String) : ApplyStatus()
}

data class AccountApplyResult(
    val account: Account,
    val status: ApplyStatus = ApplyStatus.Loading
)

// ── Report DTOs ───────────────────────────────────────────────────────────────

data class FilterDateParam(
    val key: String,
    val value: String,
    val condition: String = "",
    val alias: String = ""
)

data class ReportRequest(
    val filterFieldParams: List<Map<String, String>> = listOf(
        mapOf("key" to "companyShare.companyIssue.companyISIN.script", "alias" to "Scrip"),
        mapOf("key" to "companyShare.companyIssue.companyISIN.company.name", "alias" to "Company Name")
    ),
    val page: Int = 1,
    val size: Int = 200,
    val searchRoleViewConstants: String = "VIEW_APPLICANT_FORM_COMPLETE",
    val filterDateParams: List<FilterDateParam>
)

data class ReportItem(
    val applicantFormId: Int,
    val companyName: String,
    val scrip: String,
    val appliedKitta: Int,
    val statusName: String,
    var allotmentStatus: String = "",
    var allottedKitta: Int = 0
)

data class AllotmentDetailResponse(
    val statusName: String,
    val allottedKitta: Int
)

// ── Network DTOs — iporesult.cdsc.com.np ─────────────────────────────────────

data class CompanyShare(
    val id: Int,
    val name: String,
    val scrip: String,
    val isFileUploaded: Boolean
)

data class ResultCheckRequest(
    val companyShareId: Int,
    val boid: String,
    val userCaptcha: String,
    val captchaIdentifier: String
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
