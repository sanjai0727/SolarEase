package com.example.feature.home.domain

import com.example.core.network.RetrofitClient
import com.example.core.network.dto.ChatQueryRequest
import com.example.core.network.dto.ChatResponse

class ChatbotRepository {
    suspend fun queryChatbot(query: String, mode: String, language: String): Result<ChatResponse> {
        return try {
            val request = ChatQueryRequest(query, mode, language)
            val response = RetrofitClient.apiService.queryChatbot(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
