package com.example.feature.home.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillDashboard(
    onNavigateBack: () -> Unit,
    viewModel: TnebBillStatusViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TNEB", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        viewModel.resetValidationState()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E7038),
                    titleContentColor = Color.White
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
                .padding(16.dp)
        ) {
            // CURRENT BILL CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CURRENT BILL", color = Color(0xFF1E7038), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("—", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E222D))
                    Text("Amount Due", color = Color.Gray, fontSize = 12.sp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Units Used", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Bill Date", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Billed Amt", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Due By", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Meter Reading", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // SERVICE DETAILS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("SERVICE DETAILS", color = Color(0xFF1E7038), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("LOCATION", color = Color(0xFF1E7038), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Region", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Circle", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Section", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Phase", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sanctioned Load", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Distribution", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("METER INFO", color = Color(0xFF1E7038), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Meter No.", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ACCD / SD", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Service No.", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tariff Code", color = Color.Gray, fontSize = 12.sp)
                            Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Address", color = Color.Gray, fontSize = 12.sp)
                    Text("—", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E222D))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E7038)),
                border = BorderStroke(1.dp, Color(0xFF1E7038))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
                Spacer(modifier = Modifier.width(8.dp))
                Text("SHARE BILL SUMMARY", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            
            // Keep WebView hidden but alive in the dashboard just in case, though it's technically optional
            TnebBillStatusWebView(
                viewModel = viewModel,
                modifier = Modifier.size(1.dp)
            )
        }
    }
}
