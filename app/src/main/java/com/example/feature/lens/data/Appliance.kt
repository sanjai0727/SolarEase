package com.example.feature.lens.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Appliance(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "appliance_name") val applianceName: String,
    @Json(name = "appliance_type") val applianceType: String,
    @Json(name = "power_rating_watts") val powerRatingWatts: Float,
    @Json(name = "daily_usage_hours") val dailyUsageHours: Float,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "monthly_consumption_kwh") val monthlyConsumptionKwh: Float,
    @Json(name = "estimated_monthly_cost") val estimatedMonthlyCost: Float,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ApplianceCreateRequest(
    @Json(name = "appliance_name") val applianceName: String,
    @Json(name = "appliance_type") val applianceType: String,
    @Json(name = "power_rating_watts") val powerRatingWatts: Float,
    @Json(name = "daily_usage_hours") val dailyUsageHours: Float,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "monthly_consumption_kwh") val monthlyConsumptionKwh: Float,
    @Json(name = "estimated_monthly_cost") val estimatedMonthlyCost: Float
)
