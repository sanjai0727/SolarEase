package com.example.feature.lens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceResultScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Result") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            when (uiState) {
                is LensUiState.Saving -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LensUiState.Result -> {
                    val appliance = (uiState as LensUiState.Result).appliance
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = appliance.applianceName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appliance.applianceType,
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Estimated Monthly Usage",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "${String.format("%.1f", appliance.monthlyConsumptionKwh)} Units",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Estimated Monthly Cost",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", appliance.estimatedMonthlyCost)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Energy Insight",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This appliance may consume approximately ${String.format("%.1f", appliance.monthlyConsumptionKwh)} units per month based on your usage.",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Recommendation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Reducing daily usage by 1-2 hours could significantly reduce monthly electricity consumption.",
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                viewModel.restartScanning()
                                onScanAnother()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Another Appliance")
                        }
                    }
                }
                is LensUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (uiState as LensUiState.Error).message,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                is LensUiState.Analytics -> {
                    val state = uiState as LensUiState.Analytics
                    val appliances = state.appliances

                    // Group by label to show quantities
                    val groupedAppliances = appliances.groupBy { it.label }
                    
                    var totalConnectedLoad = 0f
                    var totalDailyConsumption = 0f
                    var totalMonthlyConsumption = 0f

                    // We assume e.g. 4 hours average daily usage for estimation
                    val defaultDailyHours = 4f 
                    val tariffRate = 5.0f // ₹5 per unit

                    for ((_, list) in groupedAppliances) {
                        val quantity = list.size
                        val powerWatts = list.first().powerWatts
                        
                        totalConnectedLoad += (powerWatts * quantity)
                        val dailyConsumption = (powerWatts / 1000f) * defaultDailyHours * quantity
                        totalDailyConsumption += dailyConsumption
                        totalMonthlyConsumption += (dailyConsumption * 30f)
                    }

                    val totalMonthlyCost = totalMonthlyConsumption * tariffRate

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Scan Complete",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Duration: ${state.scanDurationMillis / 1000} seconds",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // List of detected appliances
                        Text(
                            text = "Detected Appliances (${appliances.size} total)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        for ((label, list) in groupedAppliances) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(
                                    text = "${label.replaceFirstChar { it.uppercase() }} × ${list.size}",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${list.first().powerEstimateStr}",
                                    fontSize = 16.sp,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Total Estimated Connected Load",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "${totalConnectedLoad.toInt()} W",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Estimated Monthly Usage (at 4h/day)",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "${String.format("%.1f", totalMonthlyConsumption)} Units",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Estimated Monthly Cost",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", totalMonthlyCost)}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "* All power and energy values are estimates based on standard appliance ratings.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                viewModel.restartScanning()
                                onScanAnother()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan Again")
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
