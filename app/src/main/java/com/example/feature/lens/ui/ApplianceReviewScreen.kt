package com.example.feature.lens.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.lens.utils.ApplianceMaster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceReviewScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LensUiState.Analytics) {
            onNavigateToResult()
        }
    }

    if (uiState !is LensUiState.Review) {
        return
    }

    val reviewState = uiState as LensUiState.Review
    val appliances = reviewState.appliances

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review Appliances")
                        Text(
                            text = "Review and edit the detected appliances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Outlined "Add Appliance" button
                    OutlinedButton(
                        onClick = { viewModel.addReviewAppliance() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "+ Add Appliance",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    // Filled "Confirm & View Analytics" button with trailing arrow
                    Button(
                        onClick = { viewModel.confirmReview() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(24.dp))
                            Text(
                                "Confirm & View Analytics",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            items(appliances) { appliance ->
                EditableApplianceCard(
                    appliance = appliance,
                    onUpdate = { id, label, watts, usageHours, monthlyDays, qty ->
                        viewModel.updateReviewAppliance(id, label, watts, usageHours, monthlyDays, qty)
                    },
                    onRemove = { id ->
                        viewModel.removeReviewAppliance(id)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditableApplianceCard(
    appliance: EditableAppliance,
    onUpdate: (String, String, Float, Float, Int, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = ApplianceMaster.appliances.map { it.name }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // --- Row 1: "Appliance" label + Delete icon ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appliance",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = { onRemove(appliance.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove appliance",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // --- Appliance Dropdown ---
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = appliance.label,
                    onValueChange = { /* Read-only: dropdown only */ },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, fontSize = 14.sp) },
                            onClick = {
                                val defaults = ApplianceMaster.getDefaults(option)
                                // Auto-populate wattage, usage hours, monthly days. Quantity stays unchanged.
                                onUpdate(
                                    appliance.id,
                                    option,
                                    defaults?.defaultWattage ?: appliance.powerWatts,
                                    defaults?.typicalUsageHoursPerDay ?: appliance.usageHoursPerDay,
                                    defaults?.monthlyUsageDays ?: appliance.monthlyUsageDays,
                                    appliance.quantity  // Quantity remains independent
                                )
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Row 2: Wattage, Usage/Day, Monthly Days, Quantity — all on one row ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                // -- Wattage --
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wattage (W)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    OutlinedTextField(
                        value = if (appliance.powerWatts > 0) appliance.powerWatts.toInt().toString() else if (appliance.powerWatts == 0f) "" else "",
                        onValueChange = { newVal ->
                            if (newVal.isEmpty()) {
                                onUpdate(appliance.id, appliance.label, 0f, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity)
                            } else {
                                val watts = newVal.toFloatOrNull()
                                if (watts != null && watts >= 0f) {
                                    onUpdate(appliance.id, appliance.label, watts, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                }

                // -- Typical Usage / Day --
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Typical Usage / Day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp),
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        OutlinedTextField(
                            value = if (appliance.usageHoursPerDay > 0) {
                                val v = appliance.usageHoursPerDay
                                if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v).trimEnd('0').trimEnd('.')
                            } else "",
                            onValueChange = { newVal ->
                                if (newVal.isEmpty()) {
                                    onUpdate(appliance.id, appliance.label, appliance.powerWatts, 0f, appliance.monthlyUsageDays, appliance.quantity)
                                } else {
                                    val hours = newVal.toFloatOrNull()
                                    if (hours != null && hours >= 0f && hours <= 24f) {
                                        onUpdate(appliance.id, appliance.label, appliance.powerWatts, hours, appliance.monthlyUsageDays, appliance.quantity)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        Text(
                            text = "h",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // -- Monthly Usage Days --
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Usage Days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp),
                        maxLines = 1
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        OutlinedTextField(
                            value = if (appliance.monthlyUsageDays > 0) appliance.monthlyUsageDays.toString() else "",
                            onValueChange = { newVal ->
                                if (newVal.isEmpty()) {
                                    onUpdate(appliance.id, appliance.label, appliance.powerWatts, appliance.usageHoursPerDay, 0, appliance.quantity)
                                } else {
                                    // Integer only, no decimals
                                    val days = newVal.toIntOrNull()
                                    if (days != null && days in 0..31) {
                                        onUpdate(appliance.id, appliance.label, appliance.powerWatts, appliance.usageHoursPerDay, days, appliance.quantity)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                        )
                        Text(
                            text = "days",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // -- Quantity --
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Quantity",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Minus button
                        OutlinedButton(
                            onClick = {
                                if (appliance.quantity > 1) {
                                    onUpdate(appliance.id, appliance.label, appliance.powerWatts, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity - 1)
                                }
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("−", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // Quantity value
                        Text(
                            text = appliance.quantity.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        // Plus button
                        OutlinedButton(
                            onClick = {
                                onUpdate(appliance.id, appliance.label, appliance.powerWatts, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity + 1)
                            },
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
