package com.example.core.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object FirebaseTokenProvider {
    private const val TAG = "FirebaseTokenProvider"

    suspend fun getFirebaseIdToken(forceRefresh: Boolean = false): String? {
        val currentUser = FirebaseAuth.getInstance().currentUser
        Log.d(TAG, "[LENS AUTH] Firebase user available: ${currentUser != null}")
        
        if (currentUser == null) {
            return null
        }

        return try {
            val result = currentUser.getIdToken(forceRefresh).await()
            val token = result.token
            Log.d(TAG, "[LENS AUTH] Firebase token available: ${!token.isNullOrEmpty()}")
            token
        } catch (e: Exception) {
            Log.w(TAG, "[LENS AUTH] Firebase ID token request failed: ${e.javaClass.simpleName}")
            null
        }
    }
}
