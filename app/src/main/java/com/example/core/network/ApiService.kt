package com.example.core.network

import com.example.core.network.dto.FirebaseTokenRequest
import com.example.core.network.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

import com.example.core.network.dto.ChatQueryRequest
import com.example.core.network.dto.ChatResponse

interface ApiService {
    @POST("auth/firebase")
    suspend fun verifyFirebaseToken(
        @retrofit2.http.Header("Authorization") authHeader: String,
        @Body request: FirebaseTokenRequest
    ): Response<TokenResponse>

    @POST("chatbot/query")
    suspend fun queryChatbot(@Body request: ChatQueryRequest): Response<ChatResponse>

    @retrofit2.http.PATCH("auth/me")
    suspend fun updateProfile(
        @Body request: com.example.core.network.dto.UserUpdateRequest
    ): Response<com.example.core.network.dto.UserResponse>
    
    @retrofit2.http.GET("auth/me")
    suspend fun getProfile(): Response<com.example.core.network.dto.UserResponse>
}
