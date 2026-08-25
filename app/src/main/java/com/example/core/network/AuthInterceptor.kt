package com.example.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import android.util.Log

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Skip auth for endpoints that don't need the SolarEase access token
        if (original.url.encodedPath.contains("/auth/firebase")) {
            return chain.proceed(original)
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
