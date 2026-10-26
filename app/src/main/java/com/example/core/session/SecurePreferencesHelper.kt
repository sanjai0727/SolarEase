package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurePreferencesHelper {
    private const val TAG = "SecurePrefsHelper"

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
