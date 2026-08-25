package com.example.feature.lens.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensLandingScreen(
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SolarEase Lens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Icon or Illustration (Using a large icon for now)
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "SolarEase Lens",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Scan your home appliances and get an estimated power consumption.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "How it works:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val steps = listOf(
                        "1. Point your camera at an appliance.",
                        "2. SolarEase Lens identifies the appliance.",
                        "3. View the estimated power consumption.",
                        "4. Add usage details.",
                        "5. Save the appliance to your energy profile."
                    )
                    
                    steps.forEach { step ->
                        Text(
                            text = step,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onOpenCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Open Camera",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
