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

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: com.example.core.network.dto.RefreshTokenRequest): Response<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @POST("auth/resolve-identifier")
    suspend fun resolveIdentifier(
        @Body request: com.example.core.network.dto.ResolveIdentifierRequest
    ): Response<com.example.core.network.dto.ResolveIdentifierResponse>

    @retrofit2.http.GET("auth/check-username")
    suspend fun checkUsername(
        @retrofit2.http.Query("username") username: String
    ): Response<com.example.core.network.dto.CheckUsernameResponse>
}

