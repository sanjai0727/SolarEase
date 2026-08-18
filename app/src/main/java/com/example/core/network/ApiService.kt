package com.example.core.network

import com.example.core.network.dto.FirebaseTokenRequest
import com.example.core.network.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("auth/firebase")
    suspend fun verifyFirebaseToken(@Body request: FirebaseTokenRequest): Response<TokenResponse>
}
