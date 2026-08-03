package com.example.feature.assessment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MapsHomeWork
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StepProgress
import com.example.ui.components.SelectableCard

@Composable
fun AssessmentInputRoute(
    customerType: String,
    systemType: String,
    onNavigateNext: (RecommendationResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SiteDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val successColor = Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepProgress(currentStep = 7, totalSteps = 11)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MapsHomeWork,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Site Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Provide details to estimate your plant size.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = uiState.monthlyConsumption,
            onValueChange = { viewModel.updateMonthlyConsumption(it) },
            label = { Text("Monthly Consumption (kWh)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = uiState.consumptionError != null,
            supportingText = {
                if (uiState.consumptionError != null) {
                    Text(text = uiState.consumptionError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        OutlinedTextField(
            value = uiState.availableArea,
            onValueChange = { viewModel.updateAvailableArea(it) },
            label = { Text("Available Area (sq.m)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            isError = uiState.areaError != null,
            supportingText = {
                if (uiState.areaError != null) {
                    Text(text = uiState.areaError!!, color = MaterialTheme.colorScheme.error)
                }
            }
        )

        // Geotag Site Photo Status
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = !uiState.isPhotoCaptured) { viewModel.updatePhotoCaptured(true) },
            color = if (uiState.isPhotoCaptured) successColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (uiState.isPhotoCaptured) {
                    Text(
                        text = "✅ Geotag Site Photo Captured",
                        color = successColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Photo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Geotag Site Photo Required",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Text(
            text = "Mount Type", 
            style = MaterialTheme.typography.titleMedium, 
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
        )
        
        val types = listOf(
            Pair("Rooftop", Icons.Default.Roofing),
            Pair("Ground", Icons.Default.Landscape)
        )
        
        types.forEach { (type, icon) ->
            SelectableCard(
                selected = uiState.mountType == type,
                onClick = { viewModel.updateMountType(type) },
                title = type,
                icon = icon
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .weight(0.35f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text("Back", style = MaterialTheme.typography.titleMedium)
            }
            
            Button(
                onClick = {
                    viewModel.calculateEstimate(customerType, systemType)?.let { result ->
                        onNavigateNext(result)
                    }
                },
                modifier = Modifier
                    .weight(0.65f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                enabled = uiState.isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
