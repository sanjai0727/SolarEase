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

@JsonClass(generateAdapter = true)
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@JsonClass(generateAdapter = true)
data class Detection(
    val id: String,
    val label: String,
    val confidence: Float,
    val source: String,
    @Json(name = "bounding_box") val boundingBox: BoundingBox,
    @Json(name = "power_rating_watts") val powerRatingWatts: Float? = null,
    @Json(name = "quantity") val quantity: Int? = 1,
    @Json(name = "evidence") val evidence: String? = null,
    @Json(name = "appliance_type") val applianceType: String? = null,
    @Json(name = "appliance_name") val applianceName: String? = null
)

@JsonClass(generateAdapter = true)
data class DetectedAppliancePayload(
    @Json(name = "appliance_type") val applianceType: String,
    @Json(name = "appliance_name") val applianceName: String,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "power_rating_watts") val powerRatingWatts: Float? = null,
    @Json(name = "confidence") val confidence: Float = 0.9f,
    @Json(name = "evidence") val evidence: String? = null
)

@JsonClass(generateAdapter = true)
data class DetectionResult(
    val detections: List<Detection> = emptyList(),
    @Json(name = "detected_appliances") val detectedAppliances: List<DetectedAppliancePayload>? = null
)
