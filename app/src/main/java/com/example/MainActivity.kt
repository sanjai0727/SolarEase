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
    
    if (BuildConfig.DEBUG) {
        com.example.core.session.UserSession.clear(this)
    }
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
                val startDest = if (com.example.core.session.UserSession.currentUser != null) com.example.navigation.Screen.Home else com.example.navigation.Screen.AuthGraph
                SolarEaseNavGraph(navController = navController, startDestination = startDest)
            }
        }
      }
    }
  }
}

