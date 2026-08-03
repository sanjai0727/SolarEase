package com.example.feature.order.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.ErrorOutline
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
import com.example.ui.components.SelectableCard
import com.example.feature.assessment.util.SolarEstimateResult
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentOptionRoute(
    estimateResult: SolarEstimateResult?,
    onNavigateNext: (String) -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(estimateResult) {
        viewModel.setEstimateResult(estimateResult)
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
        StepProgress(currentStep = 11, totalSteps = 11)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Payment Method",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Choose how you'd like to pay.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (uiState.error != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        
        uiState.estimateResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Total", style = MaterialTheme.typography.bodyLarge)
                        Text(currencyFormat.format(result.grossTotal), style = MaterialTheme.typography.bodyLarge)
                    }
                    if (result.subsidyAmount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subsidy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            Text("-${currencyFormat.format(result.subsidyAmount)}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Payable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(result.netPayable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val options = listOf(
            Pair("UPI", Icons.Default.QrCode),
            Pair("Net Banking", Icons.Default.AccountBalance),
            Pair("Debit/Credit Card", Icons.Default.CreditCard),
            Pair("Loan Disbursement", Icons.Default.RequestQuote)
        )
        
        options.forEach { (option, icon) ->
            SelectableCard(
                selected = uiState.selectedPaymentMethod == option,
                onClick = { viewModel.selectPaymentMethod(option) },
                title = option,
                icon = icon
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { uiState.selectedPaymentMethod?.let { onNavigateNext(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = uiState.selectedPaymentMethod != null && uiState.error == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            val buttonText = if (uiState.paymentAmount > 0) "Pay ${currencyFormat.format(uiState.paymentAmount)}" else "Pay"
            Text(buttonText, style = MaterialTheme.typography.titleMedium)
        }
    }
}
