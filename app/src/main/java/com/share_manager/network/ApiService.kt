package com.share_manager.network

import com.share_manager.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for backend.cdsc.com.np
 * Base URL: https://backend.cdsc.com.np/api/meroShare/
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("auth/")
    suspend fun login(
        @Body request: AuthRequest
    ): Response<Unit>   // token is in response header "Authorization"

    // ── Banks / Branch ────────────────────────────────────────────────────────

    /**
     * GET /capital/
     * Returns list of all banks/capitals.
     */
    @GET("capital/")
    suspend fun getBanks(
        @Header("Authorization") token: String
    ): Response<List<BankDto>>

    /**
     * GET /bank/{bankId}/branchList
     * Returns branch info for a given bank.
     * Mirror of Python: UserSession.bank_info() + set_branch_info()
     */
    @GET("bank/{bankId}/branchList")
    suspend fun getBranchInfo(
        @Path("bankId") bankId: Int,
        @Header("Authorization") token: String
    ): Response<List<BranchInfo>>

    // ── Issues ─────────────────────────────────────────────────────────────────

    /**
     * POST /companyShare/currentIssue
     * Returns open/applicable share issues for the authenticated user.
     */
    @POST("companyShare/currentIssue")
    suspend fun getApplicableIssues(
        @Body request: ApplicableIssueRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    /**
     * GET /active/{companyShareId}?demat={demat}
     * Checks whether this account can apply for the given issue.
     * Server returns JSON with "message": "Customer can apply." on success.
     */
    @GET("active/{companyShareId}")
    suspend fun canApply(
        @Path("companyShareId") companyShareId: Int,
        @Query("demat") demat: String,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    // ── Apply ─────────────────────────────────────────────────────────────────

    /**
     * POST /applicantForm/
     * Submits an IPO application for the authenticated user.
     */
    @POST("applicantForm/")
    suspend fun applyForShare(
        @Body request: ApplyRequest,
        @Header("Authorization") token: String
    ): Response<ApplyResponse>

    // ── Reports ───────────────────────────────────────────────────────────────

    /**
     * POST /applicantForm/active/search/
     * Returns application history for the authenticated user.
     */
    @POST("applicantForm/active/search/")
    suspend fun getApplicationReports(
        @Body request: ReportRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    /**
     * GET /applicantForm/report/detail/{formId}
     * Returns allotment detail for a submitted application form.
     */
    @GET("applicantForm/report/detail/{formId}")
    suspend fun getAllotmentDetail(
        @Path("formId") formId: Int,
        @Header("Authorization") token: String
    ): Response<AllotmentDetailResponse>
}
