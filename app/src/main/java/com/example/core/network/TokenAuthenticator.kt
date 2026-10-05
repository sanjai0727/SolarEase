package com.example.core.network

import android.util.Log
import com.example.core.network.dto.TokenResponse
import com.example.core.session.UserSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * OkHttp Authenticator that automatically catches HTTP 401 Unauthorized responses,
 * performs refresh token rotation against the SolarEase backend, and transparently
 * retries the failed requests with the new access token.
 */
class TokenAuthenticator : Authenticator {

    companion object {
        private const val TAG = "TokenAuthenticator"
        private const val MAX_RETRY_COUNT = 2
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        // Do not intercept or loop on auth endpoints
        if (path.contains("/auth/refresh") ||
            path.contains("/auth/firebase") ||
            path.contains("/auth/logout")
        ) {
            return null
        }

        if (responseCount(response) >= MAX_RETRY_COUNT) {
            Log.w(TAG, "Max authentication retry count reached for path: $path")
            return null
        }

        synchronized(this) {
            val currentRefreshToken = UserSession.refreshToken
            if (currentRefreshToken.isNullOrBlank()) {
                Log.w(TAG, "No refresh token available to recover from 401")
                return null
            }

            // If another thread already refreshed the token, retry with the updated access token
            val currentAccessToken = UserSession.accessToken
            val requestAuth = response.request.header("Authorization")?.removePrefix("Bearer ")
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAuth) {
                Log.d(TAG, "Re-trying request with already rotated access token")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            Log.i(TAG, "Attempting synchronous refresh token rotation...")
            val newTokens = performTokenRefresh(currentRefreshToken)
            return if (newTokens != null) {
                Log.i(TAG, "Refresh token rotation succeeded, retrying request")
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            } else {
                Log.e(TAG, "Refresh token rotation failed. Invalidating local session.")
                null
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun performTokenRefresh(refreshToken: String): TokenResponse? {
        return try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val jsonBody = """{"refresh_token":"$refreshToken"}"""
            val request = Request.Builder()
                .url(RetrofitClient.BASE_URL + "auth/refresh")
                .post(jsonBody.toRequestBody(mediaType))
                .build()

            val resp = client.newCall(request).execute()
            if (resp.isSuccessful) {
                val respBodyString = resp.body?.string() ?: return null
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(TokenResponse::class.java)
                val tokenResponse = adapter.fromJson(respBodyString)

                if (tokenResponse != null) {
                    UserSession.accessToken = tokenResponse.accessToken
                    UserSession.refreshToken = tokenResponse.refreshToken
                    return tokenResponse
                }
            } else {
                Log.w(TAG, "Backend rejected refresh token with HTTP ${resp.code}")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
            null
        }
    }
}
