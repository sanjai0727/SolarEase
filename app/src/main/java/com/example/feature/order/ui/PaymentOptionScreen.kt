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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        StepProgress(currentStep = 11, totalSteps = 11)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
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
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFFDDE4D5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = androidx.compose.ui.graphics.Color(0xFFE2E8F0))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Net Payable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(currencyFormat.format(result.netPayable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "*Inclusive of GST. Government subsidy amount is an estimate governed by PM Surya Ghar Muft Bijli Yojana guidelines and requires DISCOM technical approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = androidx.compose.ui.unit.TextUnit(10.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
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

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "By proceeding with payment, you agree to our Terms of Service & Privacy Policy. 100% full refund is guaranteed if cancelled prior to hardware dispatch.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = androidx.compose.ui.unit.TextUnit(11f, androidx.compose.ui.unit.TextUnitType.Sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Button(
            onClick = { uiState.selectedPaymentMethod?.let { onNavigateNext(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            enabled = uiState.selectedPaymentMethod != null && uiState.error == null,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            val buttonText = if (uiState.paymentAmount > 0) "Pay ${currencyFormat.format(uiState.paymentAmount)}" else "Pay"
            Text(buttonText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
