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
    val totalRooms = sortedRooms.size.coerceAtLeast(4)

    // Dynamic Live Calculations for Room Power Envelope
    val roomPeakWatts = appliances.sumOf { (it.powerWatts * it.quantity).toDouble() }.toFloat()
    val roomPeakKw = roomPeakWatts / 1000f
    val roomMonthlyKwh = appliances.sumOf { it.monthlyKwh.toDouble() }.toFloat()
    val roomMonthlyCost = ApplianceMaster.calculateMonthlyCost(roomMonthlyKwh)

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.Background,
                border = BorderStroke(1.dp, LensColors.Outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LensColors.SlateText)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                "Array Calibration",
                                style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                                color = LensColors.SlateText
                            )
                            Text(
                                "ROOM AUDIT: ${room.name.uppercase()}",
                                style = LensTypography.labelCaps,
                                color = LensColors.SlateCaption
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(LensColors.Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.SurfaceContainerLowest,
                border = BorderStroke(1.dp, LensColors.Outline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.addReviewAppliance() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Appliance", style = LensTypography.labelSm, color = LensColors.SlateText)
                    }

                    Button(
                        onClick = { viewModel.confirmReview() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Confirm ${room.name} & Continue",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = LensColors.OnPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                        }
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step Progress Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(LensColors.Primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Room $activeIdx of $totalRooms • Diagnostic Audit",
                                style = LensTypography.labelCaps,
                                color = LensColors.Primary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LensColors.SurfaceContainer
                        ) {
                            Text(
                                "Phase A • 230V Balanced",
                                style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                color = LensColors.SlateCaption,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 4-Step Linear Progress
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..totalRooms) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (i <= activeIdx) LensColors.Primary else LensColors.SurfaceContainerHighest)
                            )
                        }
                    }

                    Text("Review Detected Appliances", style = LensTypography.headlineMd, color = LensColors.SlateText)
                    val loadsCount = appliances.sumOf { it.quantity }
                    val loadsText = if (loadsCount == 1) "1 Electrical Load Identified" else "$loadsCount Electrical Loads Identified"
                    Text("${room.name} • $loadsText", style = LensTypography.bodyMd, color = LensColors.SlateCaption)
                }
            }

            // Room Power Envelope Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerLow,
                    border = BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bed, contentDescription = null, tint = LensColors.SlateCaption, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${room.name.uppercase()} POWER ENVELOPE",
                                    style = LensTypography.labelCaps,
                                    color = LensColors.SlateCaption
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.PrimaryFixed
                            ) {
                                Text(
                                    "ACTIVE INFERENCE",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = LensColors.SurfaceContainerLowest
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Peak Connected Load", style = LensTypography.labelCaps, color = LensColors.SlateCaption)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            String.format(java.util.Locale.US, "%.2f", roomPeakKw),
                                            style = LensTypography.metricValLg,
                                            color = LensColors.SlateText
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("kW", style = LensTypography.labelSm, color = LensColors.SlateCaption)
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = LensColors.SurfaceContainerLowest
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Est. Room Consumption", style = LensTypography.labelCaps, color = LensColors.SlateCaption)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            "~${roomMonthlyKwh.toInt()}",
                                            style = LensTypography.metricValLg,
                                            color = LensColors.SlateText
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("kWh/mo", style = LensTypography.labelSm, color = LensColors.SlateCaption)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Calibrated tariff basis: ₹7.00 / kWh",
                                style = LensTypography.labelSm.copy(fontSize = 11.sp),
                                color = LensColors.SlateCaption
                            )
                            Text(
                                "≈ ₹${roomMonthlyCost.toInt()} / mo",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = LensColors.SlateText
                            )
                        }
                    }
                }
            }

            // Interactive Appliance Review Cards
            if (appliances.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = LensColors.SurfaceContainerLow,
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
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
                                "No appliances selected",
                                style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                                color = LensColors.SlateText
                            )
                            Text(
                                "All detected items have been removed. Tap '+ Add Appliance' below to add appliances manually.",
                                style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                color = LensColors.SlateCaption,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(appliances) { appliance ->
                    ApplianceCardStitch(
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
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplianceCardStitch(
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
            title = { Text("Adjust Rated Load", style = LensTypography.headlineSm, color = LensColors.SlateText) },
            text = {
                Column {
                    Text("Enter rated power in Watts for ${appliance.label}:", style = LensTypography.bodyMd, color = LensColors.SlateCaption)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wattageInput,
                        onValueChange = { wattageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Power Rating (Watts)") },
                        suffix = { Text("W") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val w = wattageInput.toFloatOrNull() ?: appliance.powerWatts
                        onUpdate(appliance.id, appliance.label, w.coerceAtLeast(1f), appliance.usageHoursPerDay, appliance.monthlyUsageDays, appliance.quantity)
                        showWattageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWattageDialog = false }) {
                    Text("Cancel", color = LensColors.SlateCaption)
                }
            },
            containerColor = LensColors.SurfaceContainerLowest
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LensColors.SurfaceContainerLowest,
        border = BorderStroke(1.dp, LensColors.Outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Icon, Detection Tag, and Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LensColors.SurfaceContainerLow,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(applianceIcon, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = LensColors.PrimaryFixed
                            ) {
                                Text(
                                    "AI Suggested",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.Primary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("• ${(appliance.monthlyKwh).toInt()} kWh/mo", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateCaption)
                        }
                        Text(
                            "₹${appliance.monthlyCost.toInt()} / mo estimated",
                            style = LensTypography.labelSm.copy(fontSize = 10.sp),
                            color = LensColors.SlateCaption
                        )
                    }
                }
                IconButton(onClick = { onRemove(appliance.id) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LensColors.SlateCaption, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Standard Appliance Name Dropdown
            Text(
                "APPLIANCE NAME",
                style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                color = LensColors.SlateCaption
            )
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
                    shape = RoundedCornerShape(8.dp),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    textStyle = LensTypography.headlineSm.copy(fontSize = 14.sp)
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

            // Custom Appliance Name Input Field (Shown when Other / Custom is selected)
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
                    label = { Text("Custom Appliance Name") },
                    placeholder = { Text("e.g. Aquarium Filter, Water Purifier, Server Rack") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantitative Specs Controls (2-Column Grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Quantity Stepper
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("QUANTITY", style = LensTypography.labelCaps.copy(fontSize = 9.sp), color = LensColors.SlateCaption)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(4.dp))
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
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = LensColors.SlateText, modifier = Modifier.size(14.dp))
                            }
                            Text("${appliance.quantity}", style = LensTypography.metricValMd, color = LensColors.SlateText)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(4.dp))
                                    .clickable {
                                        onUpdate(
                                            appliance.id, appliance.label, appliance.powerWatts,
                                            appliance.usageHoursPerDay, appliance.monthlyUsageDays,
                                            appliance.quantity + 1
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = LensColors.SlateText, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Rated Load Pill (Clickable to adjust wattage)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showWattageDialog = true },
                    shape = RoundedCornerShape(6.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("RATED LOAD", style = LensTypography.labelCaps.copy(fontSize = 9.sp), color = LensColors.SlateCaption)
                            Icon(Icons.Default.Edit, contentDescription = "Edit Wattage", tint = LensColors.Primary, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${appliance.powerWatts.toInt()} W", style = LensTypography.metricValMd, color = LensColors.SlateText)
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = LensColors.PrimaryContainer
                            ) {
                                Text(
                                    if (isCustomMode) "CUSTOM" else "SPEC",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.OnPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Daily Duty Cycle Slider / Stepper
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = LensColors.SurfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("DAILY RUNTIME", style = LensTypography.labelCaps.copy(fontSize = 9.sp), color = LensColors.SlateCaption)
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
                            inactiveTrackColor = LensColors.SurfaceContainerHighest
                        )
                    )
                }
            }
        }
    }
}
