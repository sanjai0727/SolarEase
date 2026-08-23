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
    suspend fun verifyFirebaseToken(@Body request: FirebaseTokenRequest): Response<TokenResponse>

    @POST("chatbot/query")
    suspend fun queryChatbot(@Body request: ChatQueryRequest): Response<ChatResponse>
}
