package com.example.feature.lens.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}
