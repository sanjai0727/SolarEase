package com.example.feature.home.ui.modules

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TNEB_BILL_CALCULATOR_URL = "https://tneb-billcalculator.in/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCalculatorRoute(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val openCalculator = {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TNEB_BILL_CALCULATOR_URL))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open the calculator right now.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F7FB),
                    titleContentColor = Color(0xFF1E222D),
                    navigationIconContentColor = Color(0xFF1E222D)
                )
            )
        },
        containerColor = Color(0xFFF5F7FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Hero / Introduction ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF9E6)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Estimate Your Electricity Bill",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E222D)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Get an estimate of your Tamil Nadu electricity bill based on your consumed units.",
                fontSize = 15.sp,
                color = Color(0xFF7D8492),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── What you can do ──────────────────────────────────────────
            Text(
                text = "What you can do",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E222D)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val features = listOf(
                "Enter your electricity units",
                "Get an estimated bill",
                "View the bill calculation/breakdown",
                "Understand your consumption"
            )
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    features.forEachIndexed { index, feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = feature,
                                fontSize = 15.sp,
                                color = Color(0xFF1E222D)
                            )
                        }
                    }
                }
            }



            // ── Primary CTA ───────────────────────────────────────────────
            Button(
                onClick = openCalculator,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E7038), // Brand Green
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Calculate My Bill",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Powered by the TNEB Bill Calculator website",
                fontSize = 12.sp,
                color = Color(0xFFB0B5C2),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
