package com.example.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FirebaseTokenRequest(
    @Json(name = "firebase_id_token") val firebaseIdToken: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Int,
    @Json(name = "user") val user: UserResponse
)

@JsonClass(generateAdapter = true)
data class UserResponse(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "mobile_number") val mobileNumber: String?,
    @Json(name = "is_verified") val isVerified: Boolean,
    @Json(name = "created_at") val createdAt: String
)
