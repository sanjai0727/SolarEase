package com.example.feature.lens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.lens.utils.LensConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceResultScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit,
    onNavigateHome: () -> Unit
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
        ) {
            when (uiState) {
                is LensUiState.Saving -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is LensUiState.Result -> {
                    val appliance = (uiState as LensUiState.Result).appliance
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = appliance.applianceName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = appliance.applianceType,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Estimated Monthly Usage",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${String.format("%.1f", appliance.monthlyConsumptionKwh)} Units",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Estimated Monthly Cost",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "₹${String.format("%.2f", appliance.estimatedMonthlyCost)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

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
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onNavigateHome,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Back to Home")
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
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                is LensUiState.Analytics -> {
                    val state = uiState as LensUiState.Analytics
                    AnalyticsContent(
                        state = state,
                        onScanAgain = {
                            viewModel.restartScanning()
                            onScanAnother()
                        },
                        onNavigateHome = onNavigateHome
                    )
                }
                else -> {}
            }
        }
    }
}

// ── Analytics Constants ───────────────────────────────────────────

// TARIFF_RATE is now imported from LensConstants

// ── Computation helpers ───────────────────────────────────────────
private fun monthlyKwh(appliance: EditableAppliance): Float {
    return (appliance.powerWatts * appliance.usageHoursPerDay * appliance.monthlyUsageDays * appliance.quantity) / 1000f
}

private fun monthlyCost(appliance: EditableAppliance): Float {
    return monthlyKwh(appliance) * LensConstants.DEFAULT_TARIFF_RATE
}

// ── Analytics Content ─────────────────────────────────────────────
@Composable
private fun AnalyticsContent(
    state: LensUiState.Analytics,
    onScanAgain: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val appliances = state.appliances

    // Compute totals
    var totalApplianceCount = 0
    var totalConnectedLoad = 0f
    var totalMonthlyKwh = 0f

    for (a in appliances) {
        totalApplianceCount += a.quantity
        totalConnectedLoad += (a.powerWatts * a.quantity)
        totalMonthlyKwh += monthlyKwh(a)
    }
    val totalMonthlyCost = totalMonthlyKwh * LensConstants.DEFAULT_TARIFF_RATE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // ── Scan Complete Banner ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = "Scan Complete",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Duration: ${state.scanDurationMillis / 1000} seconds",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Confirmed Appliances Header ──
        item {
            Text(
                text = "Confirmed Appliances ($totalApplianceCount items)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // ── Appliance Table ──
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column {
                    // Table header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Appliance", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.8f))
                        Text("Qty", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text("Wattage\n(W)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                        Text("Usage / Day\n(h)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text("Days / Month\n(days)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text("Monthly Units\n(kWh)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                        Text("Monthly Cost\n(₹)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

                    // Table rows
                    for (appliance in appliances) {
                        val kwh = monthlyKwh(appliance)
                        val cost = monthlyCost(appliance)
                        val usageStr = if (appliance.usageHoursPerDay == appliance.usageHoursPerDay.toInt().toFloat()) {
                            "${appliance.usageHoursPerDay.toInt()} h"
                        } else {
                            "${String.format("%.1f", appliance.usageHoursPerDay)} h"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = appliance.label,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1.8f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = appliance.quantity.toString(),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.5f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${appliance.powerWatts.toInt()} W",
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = usageStr,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = appliance.monthlyUsageDays.toString(),
                                fontSize = 12.sp,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = String.format("%.1f", kwh),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.9f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "₹${cost.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End
                            )
                        }

                        // Row divider (skip last)
                        if (appliance != appliances.last()) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Item count footer ──
        item {
            Text(
                text = "Showing ${appliances.size} of ${appliances.size} items",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        // ── Summary Card ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryItem(
                            label = "Total Appliances",
                            value = totalApplianceCount.toString()
                        )
                        SummaryItem(
                            label = "Total Connected Load",
                            value = "${totalConnectedLoad.toInt()} W"
                        )
                        SummaryItem(
                            label = "Total Estimated\nMonthly Usage",
                            value = "${String.format("%.1f", totalMonthlyKwh)} kWh"
                        )
                        SummaryItem(
                            label = "Total Estimated\nMonthly Cost",
                            value = "₹${String.format("%.0f", totalMonthlyCost)}"
                        )
                    }
                }
            }
        }

        // ── Disclaimer ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("ℹ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column {
                        Text(
                            text = "Calculations are based on typical usage patterns.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "You can edit appliance details in the previous step.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Tariff Note ──
        item {
            Text(
                text = "Tariff used for calculation: ₹${String.format("%.2f", LensConstants.DEFAULT_TARIFF_RATE)} per kWh (Average domestic tariff)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // ── Scan Again Button ──
        item {
            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Scan Again",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onNavigateHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Back to Home",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

// ── Summary Item ──────────────────────────────────────────────────
@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
