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
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    com.example.core.session.UserSession.init(this)
    com.example.feature.home.ui.modules.TnebBillCache.init(this)
    com.example.feature.home.ui.modules.MeterReadingCache.init(this)
    com.example.feature.home.ui.modules.BillCalculatorHistoryStorage.init(this)
    com.example.feature.lens.data.LensRoomStorage.init(this)
    enableEdgeToEdge()
    setContent {
      SolarEaseTheme {
        val navController = rememberNavController()
        Surface(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                val isUserSignedIn = com.example.core.session.UserSession.currentUser != null ||
                    try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null } catch (_: Throwable) { false }
                val startDest = if (isUserSignedIn) com.example.navigation.Screen.Home else com.example.navigation.Screen.AuthGraph
                SolarEaseNavGraph(navController = navController, startDestination = startDest)
            }
        }
      }
    }
  }
}

