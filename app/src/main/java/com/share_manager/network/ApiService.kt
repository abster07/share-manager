package com.share_manager.network

import com.share_manager.data.model.CompanyShareListResponse
import com.share_manager.data.model.ResultCheckRequest
import com.share_manager.data.model.ResultCheckResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IpoApiService {

    @GET("result/companyShares/fileUploaded")
    suspend fun getCompanyShares(): Response<CompanyShareListResponse>

    @POST("result/result/check")
    suspend fun checkResult(
        @Body request: ResultCheckRequest
    ): Response<ResultCheckResponse>
}
