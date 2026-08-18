package com.example.core.network

import com.example.core.session.UserSession
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Skip auth for endpoints that don't need the SolarEase access token
        if (original.url.encodedPath.contains("/auth/firebase")) {
            return chain.proceed(original)
        }

        val token = UserSession.accessToken
        val requestBuilder = original.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(requestBuilder.build())
    }
}
