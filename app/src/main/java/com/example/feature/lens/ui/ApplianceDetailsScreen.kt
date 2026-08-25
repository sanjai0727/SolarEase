package com.example.feature.lens.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceDetailsScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState is LensUiState.ConfirmingAppliance) {
        val state = uiState as LensUiState.ConfirmingAppliance
        
        var name by remember { mutableStateOf(state.applianceName) }
        var type by remember { mutableStateOf(state.applianceType) }
        var power by remember { mutableStateOf(state.powerRatingWatts.toString()) }
        var hours by remember { mutableStateOf(state.dailyUsageHours.toString()) }
        var quantity by remember { mutableStateOf(state.quantity.toString()) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Appliance Details") },
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
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Appliance Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Appliance Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = power,
                    onValueChange = { power = it },
                    label = { Text("Power Rating (Watts)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = hours,
                    onValueChange = { hours = it },
                    label = { Text("Daily Usage (Hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.updateApplianceForm(
                            name,
                            type,
                            power.toFloatOrNull() ?: 0f,
                            hours.toFloatOrNull() ?: 0f,
                            quantity.toIntOrNull() ?: 1
                        )
                        viewModel.saveAppliance()
                        onNavigateToResult()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calculate & Save")
                }
            }
        }
    }
}
