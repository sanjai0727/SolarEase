package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Enterprise secure session and encrypted storage factory.
 * Provides hardware-backed AES256-GCM / AES256-SIV encrypted shared preferences with graceful fallback.
 */
object SecureSessionPreferences {
    private const val TAG = "SecureSessionPrefs"

    fun getEncryptedPrefs(context: Context, name: String): SharedPreferences {
        val appContext = context.applicationContext ?: context
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                name,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Hardware KeyStore / EncryptedSharedPreferences unavailable, falling back to standard SharedPreferences: ${e.message}")
            appContext.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }
}

/**
 * Backward-compatible alias for existing callers.
 */
@Deprecated("Use SecureSessionPreferences instead", ReplaceWith("SecureSessionPreferences"))
typealias SecurePreferencesHelper = SecureSessionPreferences
