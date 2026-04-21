package com.share_manager.network

import com.share_manager.data.model.*
import retrofit2.Response
import retrofit2.http.*

// ── Public IPO result endpoint (no auth) ─────────────────────────────────────

interface IpoApiService {
    @GET("result/companyShares/fileUploaded")
    suspend fun getCompanyShares(): Response<CompanyShareListResponse>

    @POST("result/result/check")
    suspend fun checkResult(@Body request: ResultCheckRequest): Response<ResultCheckResponse>
}

// ── Authenticated MeroShare API ───────────────────────────────────────────────

interface MeroShareApiService {

    // Auth
    @POST("api/meroShare/auth/")
    suspend fun login(@Body request: AuthRequest): Response<Unit>

    // Banks
    @GET("api/meroShare/bank/")
    suspend fun getBanks(
        @Header("Authorization") token: String
    ): Response<List<BankInfo>>

    @GET("api/meroShare/bank/{bankId}")
    suspend fun getBranchInfo(
        @Path("bankId") bankId: Int,
        @Header("Authorization") token: String
    ): Response<List<BranchInfo>>

    // Can apply check
    @GET("api/meroShare/applicantForm/customerType/{companyShareId}/{demat}")
    suspend fun canApply(
        @Path("companyShareId") companyShareId: Int,
        @Path("demat") demat: String,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    // Open issues
    @POST("api/meroShare/companyShare/applicableIssue/")
    suspend fun getApplicableIssues(
        @Body request: ApplicableIssueRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    // Apply
    @POST("api/meroShare/applicantForm/share/apply")
    suspend fun applyForShare(
        @Body request: ApplyRequest,
        @Header("Authorization") token: String
    ): Response<ApplyResponse>

    // Reports
    @POST("api/meroShare/applicantForm/active/search/")
    suspend fun getApplicationReports(
        @Body request: ReportRequest,
        @Header("Authorization") token: String
    ): Response<IssueListResponse>

    // Allotment detail
    @GET("api/meroShare/applicantForm/report/detail/{applicationId}")
    suspend fun getAllotmentDetail(
        @Path("applicationId") applicationId: Int,
        @Header("Authorization") token: String
    ): Response<ReportDetailResponse>
}
