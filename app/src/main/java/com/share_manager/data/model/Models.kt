package com.share_manager.data.model

// ── Room Entity ──────────────────────────────────────────────────────────────

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,           // friendly label e.g. "Ram's Account"
    val boid: String,           // 16-digit, required
    val crn: String = "",       // optional
    val transactionPin: String = "", // optional, stored encrypted
    val isForeignEmployment: Boolean = false
)

// ── Network DTOs ─────────────────────────────────────────────────────────────

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
    val status: ResultStatus = ResultStatus.PENDING
)

sealed class ResultStatus {
    object Pending : ResultStatus()
    object Loading : ResultStatus()
    data class Allotted(val quantity: Int, val message: String) : ResultStatus()
    data class NotAllotted(val message: String) : ResultStatus()
    data class Error(val message: String) : ResultStatus()
}
