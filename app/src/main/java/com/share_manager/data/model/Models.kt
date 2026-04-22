package com.share_manager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Room Entity ──────────────────────────────────────────────────────────────

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",
    val boid: String = "",           // 16-digit BOID
    val crn: String = "",            // optional
    val transactionPin: String = "", // stored AES-256-GCM encrypted
    val isForeignEmployment: Boolean = false
)

// ── Network DTOs — iporesult.cdsc.com.np ─────────────────────────────────────

/**
 * Represents a single company share entry from:
 * GET /result/companyShares/fileUploaded
 *
 * Raw JSON shape:
 * {
 *   "success": true,
 *   "body": {
 *     "companyShareList": [
 *       { "id": 1, "name": "...", "scrip": "...", "isFileUploaded": true }
 *     ]
 *   }
 * }
 *
 * Only entries where isFileUploaded == true are kept (results are available).
 */
data class CompanyShare(
    val id: Int,
    val name: String,
    val scrip: String,
    val isFileUploaded: Boolean
)

/**
 * Request body for:
 * POST /result/result/check
 *
 * NOTE: userCaptcha and captchaIdentifier are static values the server
 * currently accepts. If the endpoint ever enforces real captchas these
 * will need to be fetched dynamically.
 */
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
