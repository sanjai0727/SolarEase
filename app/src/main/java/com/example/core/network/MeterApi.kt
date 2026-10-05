package com.example.core.network

import com.example.core.network.dto.BaselineRequestDto
import com.example.core.network.dto.BaselineResponseDto
import com.example.core.network.dto.CurrentCycleConsumptionDto
import com.example.core.network.dto.GraphResponseDto
import com.example.core.network.dto.MeterReadingCreateDto
import com.example.core.network.dto.MeterReadingResponseDto
import com.example.core.network.dto.MeterReadingUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MeterApi {

    @POST("meter/baseline")
    suspend fun setBaseline(
        @Body request: BaselineRequestDto
    ): Response<BaselineResponseDto>

    @GET("meter/baseline/current")
    suspend fun getCurrentBaseline(): Response<BaselineResponseDto?>

    @POST("meter/readings")
    suspend fun submitReading(
        @Body request: MeterReadingCreateDto
    ): Response<MeterReadingResponseDto>

    @GET("meter/readings/today")
    suspend fun getTodayReading(): Response<MeterReadingResponseDto?>

    @PUT("meter/readings/today")
    suspend fun updateTodayReading(
        @Body request: MeterReadingUpdateDto
    ): Response<MeterReadingResponseDto>

    @PUT("meter/readings/{reading_date}")
    suspend fun updateReadingByDate(
        @retrofit2.http.Path("reading_date") readingDate: String,
        @Body request: MeterReadingUpdateDto
    ): Response<MeterReadingResponseDto>

    @GET("meter/readings")
    suspend fun getReadings(
        @Query("cycle_id") cycleId: String? = null
    ): Response<List<MeterReadingResponseDto>>

    @GET("meter/consumption/current")
    suspend fun getCurrentConsumption(): Response<CurrentCycleConsumptionDto>

    @GET("meter/graph")
    suspend fun getGraphData(): Response<GraphResponseDto>
}
