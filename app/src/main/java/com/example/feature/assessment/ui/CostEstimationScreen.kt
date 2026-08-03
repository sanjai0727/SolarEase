package com.example.feature.assessment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StepProgress
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CostEstimationRoute(
    monthlyConsumption: Double,
    availableArea: Double,
    mountType: String,
    systemType: String,
    panelType: String,
    customerType: String,
    onNavigateNext: (com.example.feature.assessment.util.SolarEstimateResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EstimateViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.calculateEstimate(
            monthlyConsumptionKwh = monthlyConsumption,
            availableAreaSqM = availableArea,
            mountType = mountType,
            systemType = systemType,
            panelType = panelType,
            customerType = customerType
        )
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }}

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepProgress(currentStep = 10, totalSteps = 11)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cost Estimate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        uiState.result?.let { result ->
            if (result.areaStatus == "Insufficient Area") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Insufficient Area", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("Required: ${result.requiredArea} sq.m | Available: ${result.availableArea} sq.m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Panels & Inverter", style = MaterialTheme.typography.bodyLarge)
                        Text(currencyFormat.format(result.panelsCost), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mounting & Wiring", style = MaterialTheme.typography.bodyLarge)
                        Text(currencyFormat.format(result.mountingCost), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Installation Labour", style = MaterialTheme.typography.bodyLarge)
                        Text(currencyFormat.format(result.labourCost), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                    
                    if (result.foundationCost > 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ground Foundation", style = MaterialTheme.typography.bodyLarge)
                            Text(currencyFormat.format(result.foundationCost), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(result.grossTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    
                    if (result.subsidyEligible) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "✓ PM Surya Ghar Subsidy Applied", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subsidy Deduction", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("-${currencyFormat.format(result.subsidyAmount)}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Government Subsidy", style = MaterialTheme.typography.titleMedium)
                            Text(currencyFormat.format(0.0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = result.subsidyReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Payable", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(result.netPayable), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (result.annualSavings > 0) {
                Text(text = "Estimated Payback: ~${result.estimatedPaybackYears} Years", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(text = "Estimated Payback: N/A", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { uiState.result?.let { onNavigateNext(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            enabled = uiState.result?.areaStatus != "Insufficient Area"
        ) {
            Text("Proceed to Payment", style = MaterialTheme.typography.titleMedium)
        }
    }
}
