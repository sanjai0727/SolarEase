package com.example.feature.lens.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface LensApi {
    @GET("lens/appliances")
    suspend fun getAppliances(): Response<List<Appliance>>

    @GET("lens/appliances/{id}")
    suspend fun getAppliance(@Path("id") id: String): Response<Appliance>

    @POST("lens/appliances")
    suspend fun createAppliance(@Body request: ApplianceCreateRequest): Response<Appliance>

    @PATCH("lens/appliances/{id}")
    suspend fun updateAppliance(
        @Path("id") id: String,
        @Body request: ApplianceCreateRequest
    ): Response<Appliance>

    @DELETE("lens/appliances/{id}")
    suspend fun deleteAppliance(@Path("id") id: String): Response<Unit>
}
