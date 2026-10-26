package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.navigation.SolarEaseNavGraph
import com.example.ui.theme.SolarEaseTheme

class MainActivity : ComponentActivity() {
  private val destinationState = androidx.compose.runtime.mutableStateOf<Any?>(null)

  private fun resolveDestination(destStr: String?): Any? {
    return when (destStr) {
      "SOLAR_VENDOR" -> com.example.navigation.Screen.EbServiceNumber
      "CUSTOMER_CATEGORY" -> com.example.navigation.Screen.CustomerCategory
      "SYSTEM_TYPE" -> com.example.navigation.Screen.SystemType
      "MONTHLY_CONSUMPTION" -> com.example.navigation.Screen.MonthlyConsumption
      "MOUNT_TYPE" -> com.example.navigation.Screen.MountType
      "AVAILABLE_AREA" -> com.example.navigation.Screen.AvailableArea
      "RECOMMENDATION" -> com.example.navigation.Screen.Recommendation
      "PANEL_SELECTION" -> com.example.navigation.Screen.PanelSelection
      "NEARBY_VENDORS" -> com.example.navigation.Screen.NearbyVendors
      "VENDOR_PROFILE" -> com.example.navigation.Screen.VendorProfile("abc_solar")
      "ESTIMATE_REQUEST" -> com.example.navigation.Screen.EstimateRequest
      "REQUEST_CONFIRMATION" -> com.example.navigation.Screen.RequestConfirmation
      "HOME" -> com.example.navigation.Screen.Home
      "RECOMMENDS" -> com.example.navigation.Screen.Recommends
      else -> null
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val dest = resolveDestination(intent.getStringExtra("START_DESTINATION"))
    if (dest != null) {
      destinationState.value = dest
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    destinationState.value = resolveDestination(intent?.getStringExtra("START_DESTINATION"))
    com.example.core.session.UserSession.init(this)
    com.example.feature.home.ui.modules.TnebBillCache.init(this)
    com.example.feature.home.ui.modules.MeterReadingCache.init(this)
    com.example.feature.home.ui.modules.BillCalculatorHistoryStorage.init(this)
    com.example.feature.lens.data.LensRoomStorage.init(this)
    com.example.feature.recommends.data.RewardStorage.init(this)
    enableEdgeToEdge()
    setContent {
      SolarEaseTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                val isUserSignedIn = com.example.core.session.UserSession.hasValidSession()
                val defaultDest = if (isUserSignedIn) com.example.navigation.Screen.Home else com.example.navigation.Screen.AuthGraph
                val targetDest = destinationState.value ?: defaultDest
                androidx.compose.runtime.key(targetDest) {
                    val navController = rememberNavController()
                    SolarEaseNavGraph(navController = navController, startDestination = targetDest)
                }
            }
        }
      }
    }
  }
}

