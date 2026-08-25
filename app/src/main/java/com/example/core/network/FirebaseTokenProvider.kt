package com.example.core.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseTokenProvider {
    private const val TAG = "FirebaseTokenProvider"

    suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "Current Firebase user exists: ${currentUser != null}")
        
        if (currentUser == null) {
            return null
        }

        return try {
            val result = currentUser.getIdToken(forceRefresh).await()
            val token = result.token
            Log.d(TAG, "Token successfully obtained: ${token != null}")
            if (token != null) {
                Log.d(TAG, "Token length: ${token.length}")
                Log.d(TAG, "User UID: ${currentUser.uid}")
            }
            token
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase token", e)
            null
        }
    }
}
