package com.share_manager.network

import com.share_manager.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the MeroShare portal API.
 * Base URL: https://backend.cdsc.com.np/
 *
 * All endpoints require a Bearer token in the Authorization header
 * (obtained from [login]) except [login] itself.
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * POST /mero-share/api/v1/auth/login
     *
     * On success the server returns HTTP 200 with the Bearer token
     * in the **Authorization** response header.
     */
    @POST("mero-share/api/v1/auth/login")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<Unit>

    // ── Bank / Branch ─────────────────────────────────────────────────────────

    /** GET /mero-share/api/v1/bank */
    @GET("mero-share/api/v1/bank")
    suspend fun getBanks(
        @Header("Authorization") token: String
    ): Response<List<BankJson>>

    /**
     * GET /mero-share/api/v1/bank/{bankId}/branch
     *
     * Returns the customer's linked account/branch detail for the given bank.
     */
    @GET("mero-share/api/v1/bank/{bankId}/branch")
    suspend fun getBranchInfo(
        @Path("bankId") bankId: Int,
        @Header("Authorization") token: String
    ): Response<List<BranchInfo>>

    // ── Issues ────────────────────────────────────────────────────────────────

    /**
     * POST /mero-share/api/v1/client/applicable-issues
     *
     * Returns all open IPO/FPO issues applicable to the authenticated user.
     */
    @POST("mero-share/api/v1/client/applicable-issues")
    suspend fun getApplicableIssues(
        @Body request: ApplicableIssueRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    // ── Can Apply ─────────────────────────────────────────────────────────────

    /**
     * GET /mero-share/api/v1/client/isEligible/{companyShareId}?demat={demat}
     *
     * Returns a JSON object with a "message" key:
     *   "Customer can apply."  → eligible
     *   anything else          → not eligible
     */
    @GET("mero-share/api/v1/client/isEligible/{companyShareId}")
    suspend fun canApply(
        @Path("companyShareId") companyShareId: Int,
        @Query("demat") demat: String,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * POST /mero-share/api/v1/applicantForm/share/apply
     */
    @POST("mero-share/api/v1/applicantForm/share/apply")
    suspend fun applyForShare(
        @Body request: ApplyRequest,
        @Header("Authorization") token: String
    ): Response<ApplyResponse>

    // ── Reports ───────────────────────────────────────────────────────────────

    /**
     * POST /mero-share/api/v1/applicantForm/report
     *
     * Returns paginated list of the user's past applications.
     */
    @POST("mero-share/api/v1/applicantForm/report")
    suspend fun getApplicationReports(
        @Body request: ReportRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    /**
     * GET /mero-share/api/v1/myPurchase/{applicantFormId}/allotment
     *
     * Returns allotment detail (status + allotted kitta) for a specific
     * applicant form.
     */
    @GET("mero-share/api/v1/myPurchase/{applicantFormId}/allotment")
    suspend fun getAllotmentDetail(
        @Path("applicantFormId") applicantFormId: Int,
        @Header("Authorization") token: String
    ): Response<AllotmentDetail>
}
