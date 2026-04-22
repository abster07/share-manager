package com.share_manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Room Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val boid: String = "",                // 16-digit BOID  (also used as "username" for MeroShare login)
    val dp: String = "",                  // DP/broker code, e.g. "13200"  — needed for login clientId lookup
    val password: String = "",            // MeroShare portal password (stored AES-256-GCM encrypted)
    val crn: String = "",                 // optional CRN
    val transactionPin: String = "",      // stored AES-256-GCM encrypted
    val isForeignEmployment: Boolean = false
) {
    /** Convenience alias — MeroShare uses the BOID as the login username. */
    val username: String get() = boid
}

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

// ── Network DTOs — meroShare API ──────────────────────────────────────────────

/** POST /mero-share/api/v1/auth/login */
data class AuthRequest(
    val clientId: Int,
    val username: String,
    val password: String
)

/** Single bank returned by GET /mero-share/api/v1/bank */
data class BankJson(
    val id: Int,
    val name: String
)

/** Branch info returned by GET /mero-share/api/v1/bank/{bankId}/branch */
data class BranchInfo(
    val id: Int,
    val accountNumber: String,
    val accountBranchId: Int,
    val accountTypeId: Int,
    var bankId: Int = 0          // populated after fetching, not in JSON
)

/** Request body for POST /mero-share/api/v1/client/applicable-issues */
data class ApplicableIssueRequest(
    val filterFieldParams: List<Any> = emptyList(),
    val page: Int = 1,
    val size: Int = 200
)

/** Wrapper response shape for applicable-issues and application-reports */
data class IssueListResponse(
    val `object`: List<IssueJson> = emptyList(),
    val totalCount: Int = 0
)

/** Single issue / application item */
data class IssueJson(
    val companyShareId: Int,
    val companyName: String,
    val scrip: String,
    val statusName: String,
    val isUnappliedOrdinary: Boolean = false  // true  ↔ can still be applied to
)

/** GET /mero-share/api/v1/myPurchase/{applicantFormId}/allotment — response */
data class AllotmentDetail(
    val statusName: String,
    val allottedKitta: Int
)

/** Request body for POST /mero-share/api/v1/applicantForm/share/apply */
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

/** Simple response wrapper for apply endpoint */
data class ApplyResponse(
    val message: String = ""
)

/** Date-range filter param used in report requests */
data class FilterDateParam(
    val key: String,
    val value: String
)

/** Request body for POST /mero-share/api/v1/applicantForm/report */
data class ReportRequest(
    val filterDateParams: List<FilterDateParam>,
    val page: Int = 1,
    val size: Int = 200
)

// ── Apply status (used by ApplyViewModel) ────────────────────────────────────

sealed class ApplyStatus {
    object Loading : ApplyStatus()
    data class Success(val message: String) : ApplyStatus()
    data class Skipped(val reason: String) : ApplyStatus()
    data class Failed(val reason: String) : ApplyStatus()
}

data class AccountApplyResult(
    val account: Account,
    val status: ApplyStatus
)

// ── Report item ───────────────────────────────────────────────────────────────

data class ReportItem(
    val applicantFormId: Int,
    val companyName: String,
    val scrip: String,
    val appliedKitta: Int,
    val statusName: String,
    var allotmentStatus: String = "",
    var allottedKitta: Int = 0
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
