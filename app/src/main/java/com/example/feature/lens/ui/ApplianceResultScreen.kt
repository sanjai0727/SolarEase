package com.example.feature.lens.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.assessment.util.EstimateConstants
import com.example.feature.lens.ui.theme.LensColors
import com.example.feature.lens.ui.theme.LensTypography
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplianceResultScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onScanAnother: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    // Aggregated calculations
    val totalConnectedKw: Double = rooms.sumOf { it.connectedLoadKw.toDouble() }
    val simultaneousKw: Double = totalConnectedKw * 0.68
    val totalMonthlyKwh: Double = rooms.sumOf { it.monthlyKwh.toDouble() }
    val dailyAvgKwh: Double = if (totalMonthlyKwh > 0.0) totalMonthlyKwh / 30.0 else 0.0
    val totalSimplifiedBaselineCost: Double = rooms.sumOf { it.simplifiedBaselineCost }
    val totalAppliancesCount: Int = rooms.sumOf { room -> room.appliances.sumOf { it.quantity } }

    // Solar sizing
    val rawPvSize: Double = if (dailyAvgKwh > 0.0) dailyAvgKwh / (4.5 * 0.79) else 3.0
    val recommendedPvKw: Double = if (rawPvSize <= 0.0) 3.0 else (ceil(rawPvSize * 2.0) / 2.0).coerceAtLeast(1.0)
    val solarMonthlyGen: Double = recommendedPvKw * 4.33 * 30.0 * 0.79
    val annualSavings: Double = solarMonthlyGen * 12.0 * EstimateConstants.TARIFF_PER_KWH
    val offsetPct: Double = if (totalMonthlyKwh > 0.0) ((solarMonthlyGen / totalMonthlyKwh) * 100.0).coerceAtMost(150.0) else 100.2

    val zonePalette = listOf(
        LensColors.Primary,
        LensColors.Tertiary,
        Color(0xFF8B5CF6),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF06B6D4)
    )

    Scaffold(
        containerColor = LensColors.Background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.InverseSurface,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = LensColors.PrimaryLive,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = data.visuals.message,
                            color = LensColors.InverseOnSurface,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.Background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LensColors.SlateText, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Your Results",
                            style = LensTypography.headlineMd,
                            color = LensColors.SlateText
                        )
                    }
                    IconButton(onClick = onNavigateHome) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = LensColors.SlateMuted, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Whole Home Analysis",
                        style = LensTypography.headlineLg,
                        color = LensColors.SlateText
                    )
                    val roomLabel = if (rooms.size == 1) "1 room" else "${rooms.size} rooms"
                    val appLabel = if (totalAppliancesCount == 1) "1 appliance" else "$totalAppliancesCount appliances"
                    Text(
                        "Based on $roomLabel and $appLabel scanned.",
                        style = LensTypography.bodyMd,
                        color = LensColors.SlateMuted
                    )
                }
            }

            // Key metrics — 3 cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ResultMetricCard(
                        label = "Load",
                        value = String.format(Locale.US, "%.2f", totalConnectedKw),
                        unit = "kW",
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricCard(
                        label = "Monthly",
                        value = String.format(Locale.US, "%.0f", totalMonthlyKwh),
                        unit = "kWh",
                        modifier = Modifier.weight(1f)
                    )
                    ResultMetricCard(
                        label = "Est. Cost",
                        value = "₹${String.format(Locale.US, "%,d", totalSimplifiedBaselineCost.toInt())}",
                        unit = "/mo",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "Estimate at ₹7/kWh. Actual bills depend on tariff slabs, subsidies, and taxes.",
                    style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                    color = LensColors.SlateCaption,
                    lineHeight = 15.sp
                )
            }

            // Room breakdown bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Energy by Room",
                            style = LensTypography.headlineSm,
                            color = LensColors.SlateText
                        )

                        // Proportional bar
                        if (totalMonthlyKwh > 0.0 && rooms.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LensColors.Outline)
                            ) {
                                rooms.forEachIndexed { index, room ->
                                    val weight = (room.monthlyKwh.toDouble() / totalMonthlyKwh).toFloat().coerceAtLeast(0.01f)
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .fillMaxHeight()
                                            .background(zonePalette[index % zonePalette.size])
                                    )
                                }
                            }
                        }

                        // Legend
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rooms.forEachIndexed { index, room ->
                                val pct = if (totalMonthlyKwh > 0.0) (room.monthlyKwh.toDouble() / totalMonthlyKwh * 100.0) else 0.0
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
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(zonePalette[index % zonePalette.size])
                                        )
                                        Text(
                                            room.name,
                                            style = LensTypography.bodyMd,
                                            color = LensColors.SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        "${String.format(Locale.US, "%.0f", pct)}%",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                        color = LensColors.SlateText
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Room detail cards
            item {
                Text(
                    "Room Details",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(rooms) { room ->
                CleanRoomCard(room = room)
            }

            // Appliance table
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "All Appliances",
                            style = LensTypography.headlineSm,
                            color = LensColors.SlateText
                        )

                        val allAppliances = rooms.flatMap { room ->
                            room.appliances.map { app -> Pair(room.name, app) }
                        }

                        if (allAppliances.isEmpty()) {
                            Text(
                                "No appliances found",
                                style = LensTypography.bodyMd,
                                color = LensColors.SlateMuted,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val tableScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(tableScrollState)
                            ) {
                                // Header
                                Row(
                                    modifier = Modifier
                                        .width(520.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LensColors.SurfaceContainerLow)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Appliance", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, modifier = Modifier.width(160.dp))
                                    Text("Qty", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
                                    Text("Watts", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, textAlign = TextAlign.End, modifier = Modifier.width(70.dp))
                                    Text("Hrs/d", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                                    Text("kWh/mo", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, textAlign = TextAlign.End, modifier = Modifier.width(85.dp))
                                    Text("Cost", style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = LensColors.SlateMuted, textAlign = TextAlign.End, modifier = Modifier.width(95.dp))
                                }

                                // Rows
                                allAppliances.forEach { (roomName, appliance) ->
                                    val rowMonthlyKwh = appliance.monthlyKwh.toDouble()
                                    val rowCost = appliance.monthlyCost.toDouble()

                                    Row(
                                        modifier = Modifier
                                            .width(520.dp)
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            appliance.label,
                                            style = LensTypography.bodyMd,
                                            color = LensColors.SlateText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(160.dp)
                                        )
                                        Text(
                                            "${appliance.quantity}",
                                            style = LensTypography.bodyMd,
                                            color = LensColors.SlateMuted,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(40.dp)
                                        )
                                        Text(
                                            "${appliance.powerWatts.toInt()}W",
                                            style = LensTypography.bodyMd.copy(fontWeight = FontWeight.Medium),
                                            color = LensColors.SlateText,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(70.dp)
                                        )
                                        Text(
                                            "${String.format(Locale.US, "%.1f", appliance.usageHoursPerDay)}h",
                                            style = LensTypography.bodyMd,
                                            color = LensColors.SlateMuted,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(60.dp)
                                        )
                                        Text(
                                            String.format(Locale.US, "%.1f", rowMonthlyKwh),
                                            style = LensTypography.bodyMd.copy(fontWeight = FontWeight.Medium),
                                            color = LensColors.SlateText,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(85.dp)
                                        )
                                        Text(
                                            "₹${String.format(Locale.US, "%,d", rowCost.toInt())}",
                                            style = LensTypography.bodyMd.copy(fontWeight = FontWeight.Medium),
                                            color = LensColors.SlateText,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(95.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Solar recommendation
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.PrimaryFixed
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(LensColors.Primary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WbSunny, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Recommended Solar System",
                                    style = LensTypography.headlineSm,
                                    color = LensColors.OnPrimaryFixed
                                )
                                Text(
                                    "Sized to cover your usage",
                                    style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                    color = LensColors.OnPrimaryFixed.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // System size
                        Text(
                            "${String.format(Locale.US, "%.1f", recommendedPvKw)} kW Solar System",
                            style = LensTypography.headlineLg.copy(fontSize = 24.sp),
                            color = LensColors.OnPrimaryFixed
                        )

                        // Key numbers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = LensColors.SurfaceContainerLowest.copy(alpha = 0.7f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Yearly Savings", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "₹${String.format(Locale.US, "%,d", annualSavings.toInt())}",
                                        style = LensTypography.metricValMd,
                                        color = LensColors.Primary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = LensColors.SurfaceContainerLowest.copy(alpha = 0.7f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Monthly Generation", style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "~${solarMonthlyGen.toInt()} kWh",
                                        style = LensTypography.metricValMd,
                                        color = LensColors.SlateText
                                    )
                                }
                            }
                        }

                        // Offset bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = LensColors.SurfaceContainerLowest.copy(alpha = 0.7f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Bill offset", style = LensTypography.labelSm, color = LensColors.SlateMuted)
                                    Text(
                                        "${String.format(Locale.US, "%.0f", offsetPct.coerceAtMost(100.0))}%",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.Bold),
                                        color = LensColors.Primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(LensColors.Outline)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth((offsetPct / 100.0).coerceAtMost(1.0).toFloat())
                                            .fillMaxHeight()
                                            .background(LensColors.Primary)
                                    )
                                }
                            }
                        }

                        // Inverter info
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = LensColors.SurfaceContainerLowest.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Memory, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Inverter: ${String.format(Locale.US, "%.1f", recommendedPvKw)} kW",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                        color = LensColors.OnPrimaryFixed
                                    )
                                    Text(
                                        "Hybrid / string inverter recommended",
                                        style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                        color = LensColors.OnPrimaryFixed.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Action buttons
                        Button(
                            onClick = onNavigateHome,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LensColors.Primary
                            )
                        ) {
                            Text(
                                "Explore Solar Options",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                                color = LensColors.OnPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Generating energy report for ${rooms.size} rooms..."
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LensColors.OnPrimaryFixed.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = LensColors.OnPrimaryFixed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Report (PDF)", style = LensTypography.labelSm, color = LensColors.OnPrimaryFixed)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ResultMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LensColors.SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, LensColors.Outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = LensTypography.labelSm.copy(fontSize = 11.sp),
                color = LensColors.SlateMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = LensTypography.metricValMd.copy(fontSize = 18.sp), color = LensColors.SlateText)
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(unit, style = LensTypography.labelSm.copy(fontSize = 10.sp), color = LensColors.SlateMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CleanRoomCard(room: Room) {
    val roomIcon = when {
        room.name.contains("Bed", ignoreCase = true) -> Icons.Default.Bed
        room.name.contains("Hall", ignoreCase = true) || room.name.contains("Living", ignoreCase = true) -> Icons.Default.Weekend
        room.name.contains("Kitchen", ignoreCase = true) -> Icons.Default.Kitchen
        room.name.contains("Bath", ignoreCase = true) -> Icons.Default.Bathtub
        else -> Icons.Default.HomeWork
    }

    val totalAppliances = room.appliances.sumOf { it.quantity }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = LensColors.SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, LensColors.Outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(LensColors.SurfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(roomIcon, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            room.name,
                            style = LensTypography.headlineSm,
                            color = LensColors.SlateText
                        )
                        val appLabel = if (totalAppliances == 1) "1 appliance" else "$totalAppliances appliances"
                        Text(
                            appLabel,
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                            color = LensColors.SlateMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${room.monthlyCost.toInt()}/mo",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = LensColors.SlateText
                    )
                    Text(
                        "${String.format(Locale.US, "%.2f", room.connectedLoadKw)} kW • ${room.monthlyKwh.toInt()} kWh",
                        style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                        color = LensColors.SlateMuted
                    )
                }
            }

            if (room.appliances.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    room.appliances.forEach { appliance ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LensColors.SurfaceContainerLow
                        ) {
                            Text(
                                "${appliance.quantity}× ${appliance.label} (${appliance.powerWatts.toInt()}W)",
                                style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                color = LensColors.SlateText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
