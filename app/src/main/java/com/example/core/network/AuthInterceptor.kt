package com.example.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class AuthInterceptor : Interceptor {
    private companion object {
        const val TAG = "AuthInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Skip auth for endpoints that don't need the SolarEase access token
        if (original.url.encodedPath.contains("/auth/firebase")) {
            return chain.proceed(original)
        }

        if (original.url.encodedPath.contains("/lens/analyze")) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            Log.d(TAG, "[LENS AUTH] Firebase user available: ${currentUser != null}")

            if (currentUser == null) {
                Log.w(TAG, "[LENS AUTH] Authorization header present: false")
                throw java.io.IOException("Firebase authentication is required. Please sign in again.")
            }

            val firebaseToken = runBlocking { FirebaseTokenProvider.getFirebaseIdToken() }
            Log.d(TAG, "[LENS AUTH] Firebase token available: ${!firebaseToken.isNullOrEmpty()}")
            if (firebaseToken.isNullOrEmpty()) {
                Log.w(TAG, "[LENS AUTH] Authorization header present: false")
                throw java.io.IOException("Firebase authentication token is unavailable. Please sign in again.")
            }

            val authenticatedRequest = original.newBuilder()
                .header("Authorization", "Bearer $firebaseToken")
                .build()
            Log.d(TAG, "[LENS AUTH] Authorization header present: ${authenticatedRequest.header("Authorization") != null}")
            return chain.proceed(authenticatedRequest)
        }

        val token = runBlocking { FirebaseTokenProvider.getFirebaseIdToken() }
        val requestBuilder = original.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $token")
        } else {
            Log.w("AuthInterceptor", "Firebase ID token is null or empty, proceeding without Authorization header")
        }
        return chain.proceed(requestBuilder.build())
    }
}
