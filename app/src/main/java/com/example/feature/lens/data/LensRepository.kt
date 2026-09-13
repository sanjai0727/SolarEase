package com.example.feature.lens.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class LensRepository(private val api: LensApi) {
    suspend fun getAppliances(): Result<List<Appliance>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAppliances()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to get appliances: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("LensRepository", "Error getting appliances", e)
            Result.failure(e)
        }
    }

    suspend fun createAppliance(request: ApplianceCreateRequest): Result<Appliance> = withContext(Dispatchers.IO) {
        try {
            val response = api.createAppliance(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create appliance: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("LensRepository", "Error creating appliance", e)
            Result.failure(e)
        }
    }

    suspend fun updateAppliance(id: String, request: ApplianceCreateRequest): Result<Appliance> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateAppliance(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to update appliance: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("LensRepository", "Error updating appliance", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAppliance(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteAppliance(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete appliance: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("LensRepository", "Error deleting appliance", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeImage(imageBytes: ByteArray, source: String): Result<DetectionResult> = withContext(Dispatchers.IO) {
        try {
            val mediaTypeImage = "image/jpeg".toMediaTypeOrNull()
            val requestBody = imageBytes.toRequestBody(mediaTypeImage)
            val multipartBody = okhttp3.MultipartBody.Part.createFormData("image", "image.jpg", requestBody)

            val mediaTypeText = "text/plain".toMediaTypeOrNull()
            val sourceBody = source.toRequestBody(mediaTypeText)

            val response = api.analyzeImage(multipartBody, sourceBody)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorDetail = try {
                    val rawError = response.errorBody()?.string()
                    if (!rawError.isNullOrEmpty()) {
                        val match = Regex("\"detail\"\\s*:\\s*\"([^\"]+)\"").find(rawError)
                        match?.groupValues?.get(1) ?: rawError
                    } else null
                } catch (e: Exception) {
                    null
                }

                val message = when {
                    response.code() == 401 ->
                        "Your session could not be verified. Please sign in again and retry."
                    response.code() == 429 ->
                        "Solar Lens AI is currently experiencing high demand. Please try again in a moment."
                    response.code() in listOf(502, 503) ->
                        errorDetail ?: "Solar Lens AI provider is temporarily unavailable. Please try again."
                    response.code() == 504 ->
                        "Solar Lens analysis timed out. Please try again."
                    response.code() == 400 ->
                        errorDetail ?: "The uploaded image could not be processed. Please try another image."
                    !errorDetail.isNullOrEmpty() ->
                        errorDetail
                    else ->
                        "Server error during analysis (${response.code()}). Please try again."
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("LensRepository", "Error analyzing image", e)
            val message = when (e) {
                is java.net.UnknownHostException ->
                    "Unable to connect to SolarEase servers. Please check your internet connection."
                is java.net.SocketTimeoutException ->
                    "Connection timed out while analyzing image. Please try again."
                is java.io.IOException if e.message?.contains("Firebase") == true ->
                    e.message ?: "Authentication required. Please sign in again."
                else ->
                    e.message ?: "Analysis failed due to a connection error. Please try again."
            }
            Result.failure(Exception(message, e))
        }
    }
}
