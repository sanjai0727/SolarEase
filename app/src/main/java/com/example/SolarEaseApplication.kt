package com.example

import android.app.Application
import android.util.Log

class SolarEaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            com.example.core.session.UserSession.init(this)
            com.example.feature.home.ui.modules.TnebBillCache.init(this)
            com.example.feature.home.ui.modules.MeterReadingCache.init(this)
            com.example.feature.home.ui.modules.BillCalculatorHistoryStorage.init(this)
            com.example.feature.home.ui.modules.ChatHistoryStorage.init(this)
            com.example.feature.lens.data.LensRoomStorage.init(this)
            Log.d("SolarEaseApp", "Global application state initialized successfully")
        } catch (e: Exception) {
            Log.e("SolarEaseApp", "Failed during application state initialization", e)
        }
    }
}
