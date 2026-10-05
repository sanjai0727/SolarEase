package com.example.feature.lens.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.lens.ui.theme.LensColors
import com.example.feature.lens.ui.theme.LensTypography
import com.example.feature.lens.utils.ApplianceMaster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceReviewScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val rooms by viewModel.rooms.collectAsState()

    BackHandler {
        onNavigateBack()
    }

    LaunchedEffect(uiState) {
        if (uiState is LensUiState.RoomOverview || uiState is LensUiState.RoomCompleted) {
            onNavigateBack()
        } else if (uiState is LensUiState.WholeHouseAnalytics) {
            onNavigateToResult()
        }
    }

    if (uiState !is LensUiState.Review) {
        return
    }

    val reviewState = uiState as LensUiState.Review
    val appliances = reviewState.newAppliances
    val room = reviewState.room

    val sortedRooms = rooms.sortedBy { it.order }
    val activeIdx = (sortedRooms.indexOfFirst { it.id == room.id } + 1).coerceAtLeast(1)
    val totalRooms = sortedRooms.size.coerceAtLeast(1)

    // Dynamic calculations
    val roomPeakWatts = appliances.sumOf { (it.ratedWattage * it.quantity).toDouble() }.toFloat()
    val roomPeakKw = roomPeakWatts / 1000f
    val roomMonthlyEnergyKwh = appliances.sumOf { it.monthlyEnergyKwh }
    val roomSimplifiedBaselineCost = appliances.sumOf { it.simplifiedBaselineCost }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.Background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LensColors.SlateText, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            "Review Appliances",
                            style = LensTypography.headlineMd,
                            color = LensColors.SlateText
                        )
                        Text(
                            room.name,
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateMuted
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.SurfaceContainerLowest,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val hasInvalidAppliance = appliances.any { it.powerWatts <= 0f }
                    if (hasInvalidAppliance) {
                        Text(
                            "Set wattage for all custom appliances before continuing.",
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp, color = LensColors.Error),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.addReviewAppliance() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Appliance", style = LensTypography.labelSm.copy(fontWeight = FontWeight.Medium), color = LensColors.SlateText)
                    }

                    Button(
                        onClick = { viewModel.confirmReview() },
                        enabled = !hasInvalidAppliance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary)
                    ) {
                        Text(
                            "Confirm & Continue",
                            style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                            color = LensColors.OnPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        containerColor = LensColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Progress bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Room $activeIdx of $totalRooms",
                            style = LensTypography.labelSm.copy(fontWeight = FontWeight.Medium),
                            color = LensColors.SlateMuted
                        )
                        val applianceCount = appliances.sumOf { it.quantity }
                        val label = if (applianceCount == 1) "1 appliance" else "$applianceCount appliances"
                        Text(
                            label,
                            style = LensTypography.labelSm,
                            color = LensColors.Primary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..totalRooms) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= activeIdx) LensColors.Primary else LensColors.Outline)
                            )
                        }
                    }
                }
            }

            // Room energy summary
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Load
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Load", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            String.format(java.util.Locale.US, "%.2f", roomPeakKw),
                                            style = LensTypography.metricValMd,
                                            color = LensColors.SlateText
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("kW", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    }
                                }
                            }

                            // Monthly energy
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Monthly", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            String.format(java.util.Locale.US, "%.1f", roomMonthlyEnergyKwh),
                                            style = LensTypography.metricValMd,
                                            color = LensColors.SlateText
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("kWh", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Est. cost",
                                style = LensTypography.labelSm,
                                color = LensColors.SlateMuted
                            )
                            Text(
                                "≈ ₹${String.format(java.util.Locale.US, "%.0f", roomSimplifiedBaselineCost)}/mo",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = LensColors.SlateText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Estimate at ₹7/kWh. Actual bills depend on tariff slabs, subsidies, and taxes.",
                            style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                            color = LensColors.SlateCaption,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Appliance cards
            if (appliances.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = LensColors.SurfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DevicesOther,
                                contentDescription = null,
                                tint = LensColors.SlateCaption,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "No appliances yet",
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Text(
                                "Tap \"Add Appliance\" below to add items manually.",
                                style = LensTypography.bodyMd,
                                color = LensColors.SlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(appliances) { appliance ->
                    CleanApplianceCard(
                        appliance = appliance,
                        onUpdate = { id, label, watts, hours, days, qty ->
                            viewModel.updateReviewAppliance(id, label, watts, hours, days, qty)
                        },
                        onRemove = { id ->
                            viewModel.removeReviewAppliance(id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleanApplianceCard(
    appliance: EditableAppliance,
    onUpdate: (String, String, Float, Float, Int, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showWattageDialog by remember { mutableStateOf(false) }

    val standardOptions = remember {
        ApplianceMaster.appliances
            .map { it.name }
            .filter { !it.contains("Other", ignoreCase = true) }
    }
    val allDropdownOptions = remember {
        standardOptions + "Other / Custom"
    }

    // Determine if current appliance label matches a standard appliance
    val matchedStandard = remember(appliance.label) {
        ApplianceMaster.appliances.find {
            !it.name.contains("Other", ignoreCase = true) &&
            it.name.equals(appliance.label, ignoreCase = true)
        } ?: ApplianceMaster.getDefaults(appliance.label)?.takeIf { !it.name.contains("Other", ignoreCase = true) }
    }

    val isCurrentStandard = matchedStandard != null && standardOptions.contains(matchedStandard.name)
    var selectedDropdownItem by remember(appliance.label) {
        mutableStateOf(if (isCurrentStandard) matchedStandard!!.name else "Other / Custom")
    }

    val isCustomMode = selectedDropdownItem == "Other / Custom"
    var customNameText by remember(appliance.label, isCustomMode) {
        mutableStateOf(if (isCustomMode) {
            if (appliance.label.equals("Other / Custom Appliance", ignoreCase = true)) "" else appliance.label
        } else "")
    }

    val applianceIcon = when {
        appliance.label.lowercase().contains("ac") || appliance.label.lowercase().contains("air conditioner") -> Icons.Default.AcUnit
        appliance.label.lowercase().contains("fan") -> Icons.Default.WindPower
        appliance.label.lowercase().contains("tv") || appliance.label.lowercase().contains("television") -> Icons.Default.Tv
        appliance.label.lowercase().contains("fridge") || appliance.label.lowercase().contains("refrigerator") -> Icons.Default.Kitchen
        appliance.label.lowercase().contains("light") || appliance.label.lowercase().contains("led") -> Icons.Default.Lightbulb
        appliance.label.lowercase().contains("microwave") -> Icons.Default.Microwave
        else -> Icons.Default.Power
    }

    if (showWattageDialog) {
        var wattageInput by remember { mutableStateOf(appliance.powerWatts.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showWattageDialog = false },
            title = { Text("Set Wattage", style = LensTypography.headlineMd, color = LensColors.SlateText) },
            text = {
                Column {
                    Text("Power rating for ${appliance.label}:", style = LensTypography.bodyMd, color = LensColors.SlateMuted)
                    Spacer(modifier = Modifier.height(10.dp))
                    val isInvalid = wattageInput.isNotEmpty() && (wattageInput.toFloatOrNull() ?: 0f) <= 0f
                    OutlinedTextField(
                        value = wattageInput,
                        onValueChange = { wattageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Watts") },
                        suffix = { Text("W") },
                        isError = isInvalid,
                        supportingText = if (isInvalid) {
                            { Text("Must be greater than 0 W", color = LensColors.Error) }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                val inputWatts = wattageInput.toFloatOrNull() ?: 0f
                Button(
                    onClick = {
                        if (inputWatts > 0f) {
                            onUpdate(appliance.id, appliance.label, inputWatts, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity)
                            showWattageDialog = false
                        }
                    },
                    enabled = inputWatts > 0f,
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWattageDialog = false }) {
                    Text("Cancel", color = LensColors.SlateMuted)
                }
            },
            containerColor = LensColors.SurfaceContainerLowest,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = LensColors.SurfaceContainerLowest,
        border = BorderStroke(1.dp, LensColors.Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon + Name + Energy + Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(LensColors.SurfaceContainerLow, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(applianceIcon, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "${String.format(java.util.Locale.US, "%.1f", appliance.monthlyEnergyKwh)} kWh/mo",
                            style = LensTypography.labelSm.copy(fontSize = 12.sp),
                            color = LensColors.SlateMuted
                        )
                        Text(
                            "≈ ₹${String.format(java.util.Locale.US, "%.0f", appliance.simplifiedBaselineCost)}/mo",
                            style = LensTypography.labelSm.copy(fontSize = 11.sp),
                            color = LensColors.SlateCaption
                        )
                    }
                }
                IconButton(onClick = { onRemove(appliance.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = LensColors.SlateCaption, modifier = Modifier.size(18.dp))
                }
            }

            // Invalid wattage warning
            if (appliance.powerWatts <= 0f) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = LensColors.ErrorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = LensColors.Error, modifier = Modifier.size(16.dp))
                        Text(
                            "Set a wattage greater than 0 W",
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                            color = LensColors.Error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Appliance dropdown
            Text("Appliance", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted)
            Spacer(modifier = Modifier.height(4.dp))

            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedDropdownItem,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    textStyle = LensTypography.headlineSm
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    allDropdownOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option,
                                    fontWeight = if (option == selectedDropdownItem) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == "Other / Custom") LensColors.Primary else LensColors.SlateText
                                )
                            },
                            onClick = {
                                selectedDropdownItem = option
                                dropdownExpanded = false
                                if (option != "Other / Custom") {
                                    val def = ApplianceMaster.getDefaults(option)
                                    val newWattage = def?.defaultWattage ?: ApplianceMaster.getDefaultWattage(option)
                                    val newHours = def?.typicalUsageHoursPerDay ?: appliance.usageHoursPerDay
                                    val newDays = def?.monthlyUsageDays ?: appliance.monthlyUsageDays
                                    onUpdate(appliance.id, option, newWattage, newHours, newDays, appliance.quantity)
                                } else {
                                    val currentLabel = if (appliance.label == "Other / Custom Appliance" || standardOptions.contains(appliance.label)) "" else appliance.label
                                    customNameText = currentLabel
                                    if (currentLabel.isNotBlank()) {
                                        onUpdate(appliance.id, currentLabel, appliance.powerWatts, appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Custom name input
            if (isCustomMode) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customNameText,
                    onValueChange = { newName ->
                        customNameText = newName
                        val finalLabel = newName.ifBlank { "Other / Custom Appliance" }
                        onUpdate(
                            appliance.id, finalLabel, appliance.powerWatts,
                            appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity
                        )
                    },
                    label = { Text("Custom Name") },
                    placeholder = { Text("e.g. Water Purifier, Aquarium Filter") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quantity + Wattage row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Quantity", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (appliance.quantity > 1) {
                                            onUpdate(
                                                appliance.id, appliance.label, appliance.powerWatts,
                                                appliance.usageHoursPerDay, appliance.monthlyUsageDays,
                                                appliance.quantity - 1
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = LensColors.SlateText, modifier = Modifier.size(16.dp))
                            }
                            Text("${appliance.quantity}", style = LensTypography.metricValMd, color = LensColors.SlateText)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(6.dp))
                                    .clickable {
                                        onUpdate(
                                            appliance.id, appliance.label, appliance.powerWatts,
                                            appliance.usageHoursPerDay, appliance.monthlyUsageDays,
                                            appliance.quantity + 1
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = LensColors.SlateText, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Wattage
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showWattageDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wattage", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = LensColors.Primary, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${appliance.powerWatts.toInt()}", style = LensTypography.metricValMd, color = LensColors.SlateText)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("W", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Daily usage slider
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = LensColors.SurfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily usage", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                        Text("${appliance.usageHoursPerDay} hrs/day", style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold), color = LensColors.SlateText)
                    }
                    Slider(
                        value = appliance.usageHoursPerDay,
                        onValueChange = { newHours ->
                            val rounded = (Math.round(newHours * 2) / 2.0f).coerceIn(0.5f, 24f)
                            onUpdate(
                                appliance.id, appliance.label, appliance.powerWatts,
                                rounded, appliance.monthlyUsageDays, appliance.quantity
                            )
                        },
                        valueRange = 0.5f..24f,
                        colors = SliderDefaults.colors(
                            thumbColor = LensColors.Primary,
                            activeTrackColor = LensColors.Primary,
                            inactiveTrackColor = LensColors.Outline
                        )
                    )
                }
            }
        }
    }
}
