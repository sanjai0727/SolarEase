package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.subtleTopRightGradient
import com.example.ui.theme.SolarEaseSemanticColors
import com.example.ui.theme.*
import java.util.Locale

/**
 * Primary Energy Meter Tracking Card displaying current consumption, today's logging entry,
 * baseline tracking, and drawer metrics.
 */
@Composable
fun PrimaryEnergyCard(
    uiState: HomeUiState,
    onConnectBill: () -> Unit,
    onViewBillHistory: () -> Unit,
    onMeterInputChanged: (String) -> Unit,
    onSaveMeterReading: () -> Unit,
    onUpdateTodayReading: () -> Unit,
    onStartEditingReading: () -> Unit,
    onCancelEditingReading: () -> Unit,
    modifier: Modifier = Modifier,
    onEditCurrentReading: () -> Unit = {},
    onScanMeterReading: () -> Unit = {}
) {
    var isDrawerExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(20.dp),
                    radiusMultiplier = 0.85f
                )
        ) {
            if (!uiState.hasConnectedBill) {
                // ==================================================================
                // STATE A: NEW USER / BILL NOT CONNECTED (Single surface, uncluttered)
                // ==================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ElectricMeter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "TNEB LT-1A DOMESTIC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SolarEaseSemanticColors.WarningContainer
                        ) {
                            Text(
                                text = "Not Connected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SolarEaseSemanticColors.WarningText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Connect Your EB Bill",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Connect your 11–12 digit consumer number to establish your baseline tariff schedule (LT-1A Domestic) and billing cycle.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Button(
                        onClick = onConnectBill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Connect TNEB Bill",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else if (!uiState.hasSufficientReadings) {
                // ==================================================================
                // STATE B: BILL CONNECTED, BASELINE CALIBRATION (READINGS < 2)
                // ==================================================================
                val baseline = uiState.meterBaseline
                val baselineKwh = baseline?.baselineReading ?: 0.0
                val baselineDate = baseline?.baselineDate?.ifBlank { "01 Sep 2026" } ?: "01 Sep 2026"
                val prevBilledUnits = baseline?.previousBilledUnits ?: uiState.unitsConsumed

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Live Baseline Capture Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Today's Meter Reading",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SolarEaseSemanticColors.WarningContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Live Baseline Capture",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SolarEaseSemanticColors.WarningText,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Text(
                        text = "Enter the current total reading displayed on your physical digital electricity meter.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    // Baseline Starting Point Info
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "LAST BILL READING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (baselineKwh > 0.0) "${String.format(Locale.getDefault(), "%,.0f", baselineKwh)} kWh" else "Baseline Pending",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "LAST BILL DATE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = baselineDate,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (prevBilledUnits != null && prevBilledUnits > 0.0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Previous billed units: ${String.format(Locale.getDefault(), "%,.0f", prevBilledUnits)} kWh (Historical baseline)",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // If user entered a reading today and is not currently in edit mode, show recorded state
                    if (uiState.isTodayReadingEntered && !uiState.isEditingReading) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, SolarEaseSemanticColors.SuccessText.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "TODAY'S RECORDED READING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SolarEaseSemanticColors.SuccessText
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.0f", uiState.todayEnteredReading ?: baselineKwh)} kWh",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = onStartEditingReading) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Edit",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else {
                        // Prompt to enter today's meter reading
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.isEditingReading) "Edit today's meter reading" else "Enter cumulative units from meter display",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )

                                IconButton(
                                    onClick = onScanMeterReading,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Scan meter reading",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = uiState.meterInputText,
                                onValueChange = onMeterInputChanged,
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        text = if (baselineKwh > 0.0) "e.g. ${String.format(Locale.getDefault(), "%.0f", baselineKwh + 15)}" else "e.g. 42352",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                trailingIcon = {
                                    Text(
                                        text = "kWh",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                isError = uiState.meterValidationError != null,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            if (uiState.meterValidationError != null) {
                                Text(
                                    text = uiState.meterValidationError,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            if (uiState.isEditingReading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onCancelEditingReading,
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Button(
                                        onClick = onUpdateTodayReading,
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = onSaveMeterReading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Save Reading →",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Tip: Record your meter reading regularly to understand how much electricity your home uses each day.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    // Two Separate Actions: View Bill History & Check Dues
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewBillHistory,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "View Bill History",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = onConnectBill,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SolarEaseSemanticColors.WarningText,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Check Dues",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // ==================================================================
                // STATE C & D: CURRENT CYCLE LIVE DASHBOARD / TODAY'S READING SAVED
                // ==================================================================
                val metrics = uiState.currentEnergyMetrics
                val units = metrics?.unitsConsumed ?: 0.0
                val currentReadingVal = metrics?.currentReading ?: 0.0
                val avgDaily = metrics?.avgUnitsPerDay ?: 0.0
                val estBill = metrics?.estimatedBill ?: 0.0
                val periodDisplay = metrics?.billingPeriodDisplay ?: "Current billing cycle"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Live Tracking Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR CURRENT ENERGY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.8.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (uiState.isTodayReadingEntered) Color(0xFF16A34A) else Color(0xFF245B3A))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.9f))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isTodayReadingEntered) "TODAY SAVED ✓" else "LIVE TRACKING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    // Primary Numbers Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = if (units % 1.0 == 0.0) units.toInt().toString() else String.format(Locale.getDefault(), "%.1f", units),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kWh",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Text(
                                text = "Consumed since last bill",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (estBill > 0.0) "₹${String.format(Locale.getDefault(), "%,.0f", estBill)}" else "₹0",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB8860B)
                            )
                            Text(
                                text = "Estimated Bill",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Collapsed Context Banner (Cycle & Tariff)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cycle: $periodDisplay",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.tariffCategory.ifBlank { "LT-IA • Domestic" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Interactive Collapse/Expand Dropdown Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isDrawerExpanded = !isDrawerExpanded },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDrawerExpanded || uiState.isEditingReading) "Hide Details" else "View Meter Readings & Entry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isDrawerExpanded || uiState.isEditingReading) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isDrawerExpanded || uiState.isEditingReading) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Collapsible Drawer
                    AnimatedVisibility(visible = isDrawerExpanded || uiState.isEditingReading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 3 Telemetry Metric Sub-Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Current Reading Sub-Card
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (!uiState.isTodayReadingEntered) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit previous reading",
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { onEditCurrentReading() },
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%,.0f", currentReadingVal)} kWh",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Average Daily Sub-Card
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "AVERAGE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SolarEaseSemanticColors.SuccessText
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", avgDaily)} kWh/d",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Elapsed Days Sub-Card
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "ELAPSED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${metrics?.daysElapsed ?: 1} Days",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Progress Track
                            val progressFraction = (units / 1000.0).toFloat().coerceIn(0.05f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }

                            // Meter Reading Entry / Status / Edit
                            if (uiState.isTodayReadingEntered) {
                                if (uiState.isEditingReading) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Edit Today's Reading",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = uiState.meterInputText,
                                                    onValueChange = onMeterInputChanged,
                                                    modifier = Modifier.weight(1f),
                                                    trailingIcon = {
                                                        Text(
                                                            text = "kWh",
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(end = 8.dp)
                                                        )
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                    )
                                                )
                                                Button(
                                                    onClick = onUpdateTodayReading,
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                ) {
                                                    Text("Update", fontSize = 12.sp)
                                                }
                                                TextButton(onClick = onCancelEditingReading) {
                                                    Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            if (uiState.meterValidationError != null) {
                                                Text(
                                                    text = uiState.meterValidationError,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SolarEaseSemanticColors.SuccessText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Today's reading (${String.format(Locale.getDefault(), "%,.0f", uiState.todayEnteredReading ?: currentReadingVal)} kWh) logged",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        TextButton(
                                            onClick = onStartEditingReading,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Edit",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Reading not yet entered today
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Enter today's meter reading",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = uiState.meterInputText,
                                                onValueChange = onMeterInputChanged,
                                                modifier = Modifier.weight(1f),
                                                placeholder = {
                                                    Text(
                                                        text = "e.g. ${String.format(Locale.getDefault(), "%.0f", currentReadingVal + 5)}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                },
                                                trailingIcon = {
                                                    Text(
                                                        text = "kWh",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                )
                                            )
                                            Button(
                                                onClick = onSaveMeterReading,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                                )
                                            ) {
                                                Text("Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        if (uiState.meterValidationError != null) {
                                            Text(
                                                text = uiState.meterValidationError,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2-Reading Meter Calibration Progress Tracker.
 */
@Composable
fun MeterCalibrationProgressCard(
    recordedCount: Int,
    todayEnteredReading: Double?,
    baselineKwh: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Meter Calibration Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (recordedCount >= 1 || todayEnteredReading != null) {
                                "1 of 2 readings recorded"
                            } else {
                                "0 of 2 readings recorded"
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (recordedCount >= 1 || todayEnteredReading != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        SolarEaseSemanticColors.WarningContainer
                    }
                ) {
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) "In Calibration" else "Action Needed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (recordedCount >= 1 || todayEnteredReading != null) MaterialTheme.colorScheme.onSecondaryContainer else SolarEaseSemanticColors.WarningText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Visual calibration timeline steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step 1 dot
                val step1Done = recordedCount >= 1 || todayEnteredReading != null
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (step1Done) SolarEaseSemanticColors.SuccessText else MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (step1Done) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (step1Done) "First Reading" else "Enter 1st Reading",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (step1Done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (step1Done) SolarEaseSemanticColors.SuccessText.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                    thickness = 2.dp
                )

                // Step 2 dot
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Second Reading",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Explanation box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) {
                            "Why is a 2nd reading required?"
                        } else {
                            "How calibration works:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) {
                            "A single reading records cumulative kWh. To calculate your daily burn rate and unlock consumption graphs and appliance audit, SolarEase requires a second reading logged on a later date (e.g. tomorrow)."
                        } else {
                            "Enter the cumulative reading directly from your physical meter box above. This will serve as your starting point."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Meter Readings and Entry card (Item 6 of the proposed Home screen redesign).
 * Clean, glassmorphic card for entering today's meter reading or editing previously logged entry.
 */
@Composable
fun HomeMeterReadingEntryCard(
    uiState: HomeUiState,
    onMeterInputChanged: (String) -> Unit,
    onSaveMeterReading: () -> Unit,
    onUpdateTodayReading: () -> Unit,
    onStartEditingReading: () -> Unit,
    onCancelEditingReading: () -> Unit,
    onScanMeterReading: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseline = uiState.meterBaseline
    val baselineKwh = baseline?.baselineReading ?: 0.0
    val baselineDate = baseline?.baselineDate?.ifBlank { "01 Sep 2026" } ?: "01 Sep 2026"

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = OceanGlassCardBg,
        border = BorderStroke(1.dp, OceanGlassCardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = OceanAquaContainer,
                    border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "METER READINGS & ENTRY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanTeal,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (uiState.isTodayReadingEntered) OceanAquaContainer else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (uiState.isTodayReadingEntered) OceanAqua.copy(alpha = 0.5f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (uiState.isTodayReadingEntered) "Today Recorded ✓" else "Daily Entry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.isTodayReadingEntered) OceanTeal else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (uiState.isTodayReadingEntered && !uiState.isEditingReading) {
                // Today's reading already recorded
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = OceanAquaContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TODAY'S RECORDED READING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${String.format(Locale.getDefault(), "%,.0f", uiState.todayEnteredReading ?: baselineKwh)} kWh",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanDeepNavy
                            )
                        }

                        val isDark = LocalIsDarkTheme.current
                        val blueEdit = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                        val blueEditBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF)
                        OutlinedButton(
                            onClick = onStartEditingReading,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, blueEdit),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = blueEditBg,
                                contentColor = blueEdit
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = blueEdit
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = blueEdit
                            )
                        }
                    }
                }
            } else {
                // Input mode
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isEditingReading) "Edit today's meter reading" else "Enter current cumulative reading from meter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OceanTextPrimary
                        )

                        val scanBlue = if (LocalIsDarkTheme.current) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                        IconButton(
                            onClick = onScanMeterReading,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan meter reading",
                                tint = scanBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = uiState.meterInputText,
                        onValueChange = onMeterInputChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (baselineKwh > 0.0) "e.g. ${String.format(Locale.getDefault(), "%.0f", baselineKwh + 10)}" else "e.g. 42390",
                                color = OceanTextMuted.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "kWh",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OceanTextSecondary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = uiState.meterValidationError != null,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OceanTeal,
                            unfocusedBorderColor = OceanGlassCardBorder
                        )
                    )

                    if (uiState.meterValidationError != null) {
                        Text(
                            text = uiState.meterValidationError,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    if (uiState.isEditingReading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelEditingReading,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OceanGlassCardBorder)
                            ) {
                                Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OceanTextPrimary)
                            }
                            val updateBlue = if (LocalIsDarkTheme.current) Color(0xFF3B82F6) else Color(0xFF1D4ED8)
                            Button(
                                onClick = onUpdateTodayReading,
                                modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = updateBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Update", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = onSaveMeterReading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OceanTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Save Reading →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Baseline reference strip
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = SolarBrandGreenSurface,
                border = BorderStroke(1.dp, OceanGlassCardBorder.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (baselineKwh > 0.0) "Last Bill: ${String.format(Locale.getDefault(), "%,.0f", baselineKwh)} kWh" else "Baseline Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OceanTextPrimary
                    )
                    Text(
                        text = baselineDate,
                        fontSize = 11.sp,
                        color = OceanTextSecondary
                    )
                }
            }
        }
    }
}
