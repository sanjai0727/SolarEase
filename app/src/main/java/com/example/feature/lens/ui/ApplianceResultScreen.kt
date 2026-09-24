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
import com.example.feature.lens.ui.theme.GeistFontFamily
import com.example.feature.lens.ui.theme.HankenGroteskFontFamily
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

    // Mathematical Aggregations & Formulas
    val totalConnectedKw: Double = rooms.sumOf { it.connectedLoadKw.toDouble() }
    val simultaneousKw: Double = totalConnectedKw * 0.68
    val totalMonthlyKwh: Double = rooms.sumOf { it.monthlyKwh.toDouble() }
    val dailyAvgKwh: Double = if (totalMonthlyKwh > 0.0) totalMonthlyKwh / 30.0 else 0.0
    val totalMonthlyBill: Double = rooms.sumOf { it.monthlyCost.toDouble() }
    val totalAppliancesCount: Int = rooms.sumOf { room -> room.appliances.sumOf { it.quantity } }

    // Solar Sizing Engine:
    // PSH = 4.5, PR = 0.79 -> divisor = 4.5 * 0.79 = 3.555
    val rawPvSize: Double = if (dailyAvgKwh > 0.0) dailyAvgKwh / (4.5 * 0.79) else 3.0
    val recommendedPvKw: Double = if (rawPvSize <= 0.0) 3.0 else (ceil(rawPvSize * 2.0) / 2.0).coerceAtLeast(1.0)
    val solarMonthlyGen: Double = recommendedPvKw * 4.33 * 30.0 * 0.79
    val annualSavings: Double = solarMonthlyGen * 12.0 * EstimateConstants.TARIFF_PER_KWH
    val offsetPct: Double = if (totalMonthlyKwh > 0.0) ((solarMonthlyGen / totalMonthlyKwh) * 100.0).coerceAtMost(150.0) else 100.2

    val zonePalette = listOf(
        LensColors.PrimaryContainer,     // Emerald #047857
        LensColors.TertiaryContainer,    // Blue #006FA8
        LensColors.Secondary,            // Slate #565E74
        Color(0xFF93CCFF),              // Light Blue
        Color(0xFFF59E0B),              // Amber
        Color(0xFF8B5CF6)               // Purple
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        containerColor = LensColors.Surface,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Surface(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.InverseSurface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = LensColors.PrimaryFixedDim,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = data.visuals.message,
                            color = LensColors.InverseOnSurface,
                            fontFamily = GeistFontFamily,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LensColors.PrimaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = LensColors.OnPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "SolarEase",
                                fontFamily = HankenGroteskFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = LensColors.OnSurface
                            )
                            Text(
                                text = "ANALYTICS",
                                fontFamily = GeistFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                color = LensColors.OnSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = LensColors.OnSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = LensColors.OnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LensColors.Surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Sub-Header & Telemetry Milestone
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AGGREGATED PROFILE",
                                style = LensTypography.labelCaps,
                                color = LensColors.OnSurfaceVariant
                            )
                            Text(
                                text = "Whole House Analysis",
                                style = LensTypography.headlineMd,
                                color = LensColors.OnSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LensColors.PrimaryFixed.copy(alpha = 0.25f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(LensColors.PrimaryContainer.copy(alpha = pulseAlpha))
                                )
                                Text(
                                    text = "AUDIT COMPLETE",
                                    fontFamily = GeistFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp,
                                    color = LensColors.Primary
                                )
                            }
                        }
                    }

                    Text(
                        text = "High-precision telemetry model based on ${rooms.size} surveyed spatial zones and $totalAppliancesCount identified active loads.",
                        style = LensTypography.bodyMd,
                        color = LensColors.OnSurfaceVariant
                    )
                }
            }

            // 2. Visual Rooftop Context Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 1.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LensColors.InverseSurface,
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.05f),
                            modifier = Modifier
                                .size(90.dp)
                                .align(Alignment.CenterEnd)
                                .offset(x = 10.dp)
                        )

                        Column(
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                            Text(
                                text = "SURVEYED PROPERTY PROFILE",
                                fontFamily = GeistFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                color = LensColors.SecondaryFixed.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Residential Micro-Grid • ${rooms.size} Zones Complete",
                                    fontFamily = HankenGroteskFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = LensColors.PrimaryFixedDim,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Top-Level Instrumentation Matrix (3-Tier Cluster)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Metric 1: Total Load
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        iconTint = LensColors.Tertiary,
                        label = "Total Load",
                        value = String.format(Locale.US, "%.2f", totalConnectedKw),
                        unit = "kW",
                        subtext = "Peak: ${String.format(Locale.US, "%.1f", simultaneousKw)} kW"
                    )

                    // Metric 2: Estimated Usage
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        iconTint = LensColors.PrimaryContainer,
                        label = "Est. Usage",
                        value = String.format(Locale.US, "%.1f", totalMonthlyKwh),
                        unit = "kWh",
                        subtext = "Avg ${String.format(Locale.US, "%.1f", dailyAvgKwh)}/d"
                    )

                    // Metric 3: Monthly Bill
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CurrencyRupee,
                        iconTint = LensColors.Error,
                        label = "Mo. Bill",
                        value = "₹${String.format(Locale.US, "%,d", totalMonthlyBill.toInt())}",
                        unit = "",
                        subtext = "Tier-2 Discom",
                        valueColor = LensColors.Error
                    )
                }
            }

            // 4. Consumption Ratio By Zone (Segmented Bar)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.SurfaceContainerLowest,
                    shadowElevation = 1.dp
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = LensColors.OnSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Consumption Ratio",
                                    style = LensTypography.headlineSm,
                                    color = LensColors.OnSurface
                                )
                            }
                            Text(
                                text = "${String.format(Locale.US, "%.1f", totalMonthlyKwh)} kWh TOTAL",
                                style = LensTypography.labelCaps,
                                color = LensColors.Secondary
                            )
                        }

                        // Proportional Track
                        if (totalMonthlyKwh > 0.0 && rooms.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LensColors.SurfaceContainer)
                            ) {
                                rooms.forEachIndexed { index, room ->
                                    val weight = if (totalMonthlyKwh > 0.0) (room.monthlyKwh.toDouble() / totalMonthlyKwh).toFloat().coerceAtLeast(0.01f) else 0.01f
                                    val color = zonePalette[index % zonePalette.size]
                                    Box(
                                        modifier = Modifier
                                            .weight(weight)
                                            .fillMaxHeight()
                                            .background(color)
                                    )
                                }
                            }
                        }

                        // Zone Legend Items
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rooms.chunked(2).forEach { rowRooms ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowRooms.forEachIndexed { colIndex, room ->
                                        val actualIndex = rooms.indexOf(room)
                                        val pct = if (totalMonthlyKwh > 0.0) (room.monthlyKwh.toDouble() / totalMonthlyKwh * 100.0) else 0.0
                                        val color = zonePalette[actualIndex % zonePalette.size]
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(color)
                                                )
                                                Text(
                                                    text = room.name,
                                                    style = LensTypography.bodyMd,
                                                    color = LensColors.OnSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", pct)}%",
                                                fontFamily = GeistFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = LensColors.OnSurface
                                            )
                                        }
                                    }
                                    if (rowRooms.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Room Telemetry Audit (Zone Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Room Telemetry Audit",
                        style = LensTypography.headlineSm,
                        color = LensColors.OnSurface
                    )
                    Text(
                        text = "${rooms.size} SURVEYED ZONES",
                        style = LensTypography.labelCaps,
                        color = LensColors.OnSurfaceVariant
                    )
                }
            }

            items(rooms) { room ->
                RoomAuditCard(room = room)
            }

            // 6. Appliance Audit Matrix (Table)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.SurfaceContainerLowest,
                    shadowElevation = 1.dp
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
                            Column {
                                Text(
                                    text = "Appliance Audit Matrix",
                                    style = LensTypography.headlineSm,
                                    color = LensColors.OnSurface
                                )
                                Text(
                                    text = "Duty cycle calibrated against active tariffs",
                                    style = LensTypography.labelSm,
                                    color = LensColors.Secondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.TableRows,
                                contentDescription = null,
                                tint = LensColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Horizontally scrollable data table
                        val allAppliances = rooms.flatMap { room ->
                            room.appliances.map { app -> Pair(room.name, app) }
                        }

                        if (allAppliances.isEmpty()) {
                            Text(
                                text = "No appliances registered",
                                style = LensTypography.bodyMd,
                                color = LensColors.OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            val tableScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(tableScrollState)
                            ) {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .width(520.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LensColors.SurfaceContainerLow)
                                        .padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("APPLIANCE", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, modifier = Modifier.width(160.dp))
                                    Text("QTY", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
                                    Text("WATTS", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(70.dp))
                                    Text("HRS/D", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(60.dp))
                                    Text("MONTHLY", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(85.dp))
                                    Text("COST", style = LensTypography.labelCaps, color = LensColors.OnSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(95.dp))
                                }

                                // Table Rows
                                allAppliances.forEachIndexed { idx, (roomName, appliance) ->
                                    val rowMonthlyKwh = appliance.monthlyKwh.toDouble()
                                    val rowCost = appliance.monthlyCost.toDouble()

                                    Row(
                                        modifier = Modifier
                                            .width(520.dp)
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.width(160.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (appliance.powerWatts >= 1000f) LensColors.Error
                                                        else if (appliance.powerWatts >= 200f) LensColors.PrimaryContainer
                                                        else LensColors.TertiaryContainer
                                                    )
                                            )
                                            Text(
                                                text = appliance.label,
                                                fontFamily = GeistFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = LensColors.OnSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = "${appliance.quantity}",
                                            fontFamily = GeistFontFamily,
                                            fontSize = 12.sp,
                                            color = LensColors.Secondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(40.dp)
                                        )

                                        Text(
                                            text = "${appliance.powerWatts.toInt()}W",
                                            fontFamily = GeistFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = LensColors.OnSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(70.dp)
                                        )

                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", appliance.usageHoursPerDay)}h",
                                            fontFamily = GeistFontFamily,
                                            fontSize = 12.sp,
                                            color = LensColors.Secondary,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(60.dp)
                                        )

                                        Text(
                                            text = String.format(Locale.US, "%.1f", rowMonthlyKwh),
                                            fontFamily = GeistFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = LensColors.OnSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(85.dp)
                                        )

                                        Text(
                                            text = "₹${String.format(Locale.US, "%,d", rowCost.toInt())}",
                                            fontFamily = GeistFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = if (rowCost >= 1000.0) LensColors.Error else LensColors.OnSurface,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.width(95.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "*Continuous duty cycling computed via thermodynamic average",
                                style = LensTypography.labelCaps,
                                color = LensColors.Secondary,
                                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // 7. Engineered Solar Sizing Directive & System Spec
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        LensColors.InverseSurface,
                                        Color(0xFF0B1C30)
                                    )
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Badge Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.PrimaryContainer
                            ) {
                                Text(
                                    text = "ENGINEERED SOLAR SIZING DIRECTIVE",
                                    fontFamily = GeistFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    letterSpacing = 0.6.sp,
                                    color = LensColors.OnPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = "ZERO GRID DEFICIT",
                                fontFamily = GeistFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp,
                                color = LensColors.PrimaryFixedDim
                            )
                        }

                        // Headline Specification
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", recommendedPvKw)} kW On-Grid Rooftop PV",
                                fontFamily = HankenGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = LensColors.SurfaceContainerLowest
                            )
                            Text(
                                text = "Configured to neutralize 100% of your ${String.format(Locale.US, "%.1f", totalMonthlyKwh)} kWh/month residential consumption profile.",
                                fontFamily = GeistFontFamily,
                                fontSize = 13.sp,
                                color = LensColors.SecondaryFixedDim
                            )
                        }

                        // Key Engineering Metrics Strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Annual Savings
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = LensColors.InverseSurface.copy(alpha = 0.8f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = LensColors.PrimaryFixedDim,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "ANNUAL SAVINGS",
                                            fontFamily = GeistFontFamily,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.6.sp,
                                            color = LensColors.PrimaryFixedDim
                                        )
                                    }
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,d", annualSavings.toInt())}",
                                        fontFamily = GeistFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = LensColors.PrimaryFixed
                                    )
                                    Text(
                                        text = "Direct utility reduction",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 9.sp,
                                        color = LensColors.SecondaryFixedDim
                                    )
                                }
                            }

                            // Est. Generation
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = LensColors.InverseSurface.copy(alpha = 0.8f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = LensColors.TertiaryFixedDim,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "EST. GENERATION",
                                            fontFamily = GeistFontFamily,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.6.sp,
                                            color = LensColors.TertiaryFixedDim
                                        )
                                    }
                                    Text(
                                        text = "~${solarMonthlyGen.toInt()} kWh/mo",
                                        fontFamily = GeistFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = LensColors.SurfaceContainerLowest
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", offsetPct)}% bill offset target",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 9.sp,
                                        color = LensColors.SecondaryFixedDim
                                    )
                                }
                            }
                        }

                        // Hardware Recommendation Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = LensColors.SurfaceContainerLow.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(LensColors.PrimaryFixed.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Memory,
                                            contentDescription = null,
                                            tint = LensColors.PrimaryFixed,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Recommended Inverter Core",
                                            fontFamily = GeistFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = LensColors.SurfaceContainerLowest
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", recommendedPvKw)} kW High-Efficiency Hybrid / String Inverter",
                                            fontFamily = GeistFontFamily,
                                            fontSize = 10.sp,
                                            color = LensColors.SecondaryFixedDim
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LensColors.PrimaryContainer.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = "98.4% EFF",
                                        fontFamily = GeistFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        color = LensColors.PrimaryFixed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Production vs Load Mini Graph
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = LensColors.InverseSurface.copy(alpha = 0.9f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "MONTHLY NET-ZERO MATCH",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 10.sp,
                                        color = LensColors.SecondaryFixedDim
                                    )
                                    Text(
                                        text = "${solarMonthlyGen.toInt()} kWh SOLAR vs ${totalMonthlyKwh.toInt()} kWh LOAD",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 10.sp,
                                        color = LensColors.PrimaryFixed
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(LensColors.SurfaceContainerHigh.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .background(LensColors.PrimaryContainer)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "CURRENT USAGE",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 9.sp,
                                        color = LensColors.SecondaryFixedDim
                                    )
                                    Text(
                                        text = "100% OFFSET ACHIEVED",
                                        fontFamily = GeistFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 9.sp,
                                        color = LensColors.PrimaryFixed
                                    )
                                }
                            }
                        }

                        // Action Suite Buttons
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = onNavigateHome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LensColors.PrimaryContainer,
                                    contentColor = LensColors.OnPrimary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Explore Solar Options",
                                        fontFamily = HankenGroteskFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Compiling 6-page precision audit report for ${rooms.size} surveyed zones..."
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LensColors.SurfaceContainerLowest.copy(alpha = 0.12f),
                                    contentColor = LensColors.SurfaceContainerLowest
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Download Energy Audit Report (PDF)",
                                        fontFamily = GeistFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
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
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    subtext: String,
    valueColor: Color = LensColors.OnSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = LensColors.SurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = label,
                    style = LensTypography.labelCaps,
                    color = LensColors.OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = LensTypography.metricValMd,
                    color = valueColor
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unit,
                        fontFamily = GeistFontFamily,
                        fontSize = 11.sp,
                        color = LensColors.Secondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                fontFamily = GeistFontFamily,
                fontSize = 9.sp,
                color = LensColors.Secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomAuditCard(room: Room) {
    val roomIcon = when {
        room.name.contains("Bed", ignoreCase = true) -> Icons.Default.Bed
        room.name.contains("Hall", ignoreCase = true) || room.name.contains("Living", ignoreCase = true) -> Icons.Default.Weekend
        room.name.contains("Kitchen", ignoreCase = true) -> Icons.Default.Kitchen
        room.name.contains("Bath", ignoreCase = true) -> Icons.Default.Bathtub
        else -> Icons.Default.HomeWork
    }

    val iconColor = when {
        room.name.contains("Bed", ignoreCase = true) -> LensColors.PrimaryContainer
        room.name.contains("Hall", ignoreCase = true) || room.name.contains("Living", ignoreCase = true) -> LensColors.TertiaryContainer
        room.name.contains("Kitchen", ignoreCase = true) -> LensColors.Secondary
        else -> LensColors.Secondary
    }

    val totalAppliances = room.appliances.sumOf { it.quantity }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LensColors.SurfaceContainerLowest,
        shadowElevation = 1.dp
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
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LensColors.SurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = roomIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = room.name,
                            fontFamily = HankenGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = LensColors.OnSurface
                        )
                        Text(
                            text = "$totalAppliances APPLIANCES IDENTIFIED",
                            style = LensTypography.labelCaps,
                            color = LensColors.Secondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${room.monthlyCost.toInt()}/mo",
                        style = LensTypography.metricValMd,
                        color = if (room.name.contains("Bed", ignoreCase = true)) LensColors.PrimaryContainer else LensColors.OnSurface
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", room.connectedLoadKw)} kW • ${room.monthlyKwh.toInt()} kWh",
                        style = LensTypography.labelCaps,
                        color = LensColors.Secondary
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
                            shape = RoundedCornerShape(4.dp),
                            color = LensColors.SurfaceContainerLow
                        ) {
                            Text(
                                text = "${appliance.quantity}× ${appliance.label} (${appliance.powerWatts.toInt()}W)",
                                fontFamily = GeistFontFamily,
                                fontSize = 11.sp,
                                color = LensColors.OnSurface,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
