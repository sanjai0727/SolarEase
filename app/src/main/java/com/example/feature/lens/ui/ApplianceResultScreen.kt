package com.example.feature.lens.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.feature.lens.utils.AuditReportExporter
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var expandedRoomId by remember { mutableStateOf<String?>(null) }

    BackHandler {
        viewModel.popUiState()
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
    val hasValidInventory = totalMonthlyKwh > 0.0 && totalAppliancesCount > 0
    val rawPvSize: Double = if (hasValidInventory) dailyAvgKwh / (4.5 * 0.79) else 0.0
    val recommendedPvKw: Double = if (!hasValidInventory) 0.0 else (ceil(rawPvSize * 2.0) / 2.0).coerceAtLeast(1.0)
    val solarMonthlyGen: Double = if (hasValidInventory) recommendedPvKw * 4.33 * 30.0 * 0.79 else 0.0
    val annualSavings: Double = if (hasValidInventory) solarMonthlyGen * 12.0 * EstimateConstants.TARIFF_PER_KWH else 0.0
    val offsetPct: Double = if (hasValidInventory && totalMonthlyKwh > 0.0) ((solarMonthlyGen / totalMonthlyKwh) * 100.0).coerceAtMost(150.0) else 0.0

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
                            imageVector = Icons.Default.Description,
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
                    IconButton(onClick = {
                        viewModel.popUiState()
                        onNavigateBack()
                    }) {
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
            // 1. Header Section (Stitch Design)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = LensColors.SurfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "AUDIT VERIFIED",
                                style = LensTypography.labelCaps,
                                color = LensColors.Primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Your Home Energy Analysis",
                        style = LensTypography.headlineLg,
                        color = LensColors.OnSurface
                    )
                    Text(
                        text = "Here's what we found across your ${rooms.size} surveyed rooms.",
                        style = LensTypography.bodyMd,
                        color = LensColors.OnSurfaceVariant
                    )
                }
            }

            // 2. Bento Monthly Footprint Card (Stitch Design)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f)),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = null,
                                    tint = LensColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Monthly Footprint",
                                    style = LensTypography.headlineSm,
                                    color = LensColors.OnSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Text(
                                    text = "TNEB Domestic Tariff",
                                    style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                    color = LensColors.OnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Total Electricity Consumption (Prominent Hero Tile)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = LensColors.SurfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = LensColors.PrimaryFixed,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = LensColors.Primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "TOTAL ELECTRICITY",
                                            style = LensTypography.labelCaps,
                                            color = LensColors.OnSurfaceVariant
                                        )
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = "${totalMonthlyKwh.toInt()}",
                                                style = LensTypography.metricValLg.copy(fontSize = 28.sp),
                                                color = LensColors.OnSurface
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "kWh / mo",
                                                style = LensTypography.bodyMd,
                                                color = LensColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LensColors.SurfaceContainerLowest,
                                    shadowElevation = 1.dp
                                ) {
                                    Text(
                                        text = if (rooms.size <= 2) "1-2 BHK" else if (rooms.size == 3) "Typical 3BHK" else "Large Home",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                        color = LensColors.Primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // 2-Column Split: Estimated Bill & Surveyed Scope
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LensColors.SurfaceContainerHigh,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CurrencyRupee,
                                                contentDescription = null,
                                                tint = LensColors.OnSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "ESTIMATED BILL",
                                        style = LensTypography.labelCaps,
                                        color = LensColors.OnSurfaceVariant
                                    )
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,d", totalMonthlyBill.toInt())}",
                                        style = LensTypography.headlineSm.copy(fontSize = 18.sp),
                                        color = LensColors.OnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = LensColors.SurfaceContainerHigh,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Roofing,
                                                contentDescription = null,
                                                tint = LensColors.OnSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "SURVEYED SCOPE",
                                        style = LensTypography.labelCaps,
                                        color = LensColors.OnSurfaceVariant
                                    )
                                    Text(
                                        text = "${rooms.size} Rooms",
                                        style = LensTypography.headlineSm.copy(fontSize = 16.sp),
                                        color = LensColors.OnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$totalAppliancesCount Appliances",
                                        style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                        color = LensColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Proportional Room Share Bar
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f))
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
                                text = "${totalMonthlyKwh.toInt()} kWh TOTAL",
                                style = LensTypography.labelCaps,
                                color = LensColors.Primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

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

                        // Room Legend Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rooms.take(4).forEachIndexed { index, room ->
                                val pct = if (totalMonthlyKwh > 0.0) (room.monthlyKwh.toDouble() / totalMonthlyKwh * 100.0) else 0.0
                                val color = zonePalette[index % zonePalette.size]
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Text(
                                        text = "${room.name} (${pct.toInt()}%)",
                                        style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                        color = LensColors.OnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Room-by-Room Breakdown Header (Stitch Accordion Design)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Room Consumption Breakdown",
                        style = LensTypography.headlineSm,
                        color = LensColors.OnSurface
                    )
                    Text(
                        text = "ORDERED BY LOAD",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 5. Expandable Room Cards
            val sortedRoomsByLoad = rooms.sortedByDescending { it.monthlyKwh }
            items(sortedRoomsByLoad) { room ->
                val isExpanded = expandedRoomId == room.id
                val pct = if (totalMonthlyKwh > 0.0) (room.monthlyKwh.toDouble() / totalMonthlyKwh * 100.0) else 0.0
                val roomIcon = when (room.name.lowercase()) {
                    "bedroom", "master bedroom", "guest bedroom" -> Icons.Default.Bed
                    "kitchen" -> Icons.Default.Kitchen
                    "bathroom" -> Icons.Default.Shower
                    "hall", "living room", "hall / living room" -> Icons.Default.Chair
                    else -> Icons.Default.MeetingRoom
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f)),
                    shadowElevation = 1.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedRoomId = if (isExpanded) null else room.id
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LensColors.SurfaceContainerHigh,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = roomIcon,
                                            contentDescription = null,
                                            tint = LensColors.Primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = room.name,
                                            style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                                            color = LensColors.OnSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (pct >= 50.0) LensColors.Error.copy(alpha = 0.12f) else LensColors.SurfaceContainerHigh
                                        ) {
                                            Text(
                                                text = "${pct.toInt()}% Load",
                                                style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                                color = if (pct >= 50.0) LensColors.Error else LensColors.OnSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${room.appliances.size} appliances · ~${room.monthlyKwh.toInt()} kWh · ₹${room.monthlyCost.toInt()}/mo",
                                        style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                        color = LensColors.OnSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = LensColors.SurfaceContainerLow,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                                        tint = LensColors.OnSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Expanded Appliance List
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                                    .background(LensColors.SurfaceContainerLow, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (room.appliances.isEmpty()) {
                                    Text(
                                        text = "No appliances recorded in this room.",
                                        style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                        color = LensColors.OnSurfaceVariant
                                    )
                                } else {
                                    room.appliances.forEach { appliance ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${appliance.label} ${if (appliance.quantity > 1) "x${appliance.quantity}" else ""}",
                                                    style = LensTypography.bodyMd.copy(fontWeight = FontWeight.Medium),
                                                    color = LensColors.OnSurface
                                                )
                                                Text(
                                                    text = "${appliance.powerWatts.toInt()}W • ${String.format(Locale.US, "%.1f", appliance.usageHoursPerDay)} hrs/day",
                                                    style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                                    color = LensColors.OnSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "~${appliance.monthlyKwh.toInt()} kWh",
                                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                                color = LensColors.Primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Rooftop Solar Sizing Recommendation Card (Stitch Design)
            item {
                if (!hasValidInventory) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = LensColors.SurfaceContainerLowest,
                        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LensColors.SurfaceContainerHigh,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = LensColors.OnSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "No Appliance Data Added",
                                    style = LensTypography.headlineSm,
                                    color = LensColors.OnSurface
                                )
                                Text(
                                    text = "Add at least one appliance with daily usage to calculate your solar savings and recommended system size.",
                                    style = LensTypography.bodyMd,
                                    color = LensColors.OnSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            OutlinedButton(
                                onClick = onScanAnother,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, LensColors.Primary.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start a New Audit", style = LensTypography.labelSm, color = LensColors.Primary)
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = LensColors.PrimaryFixed.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, LensColors.Primary.copy(alpha = 0.25f)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Header: Optimal Match badge & Grid-Tied tag
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SolarPower,
                                            contentDescription = null,
                                            tint = LensColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "OPTIMAL MATCH",
                                            style = LensTypography.labelCaps,
                                            color = LensColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Recommended Solar System",
                                        style = LensTypography.headlineSm,
                                        color = LensColors.OnSurface
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LensColors.Primary
                                ) {
                                    Text(
                                        text = "Grid-Tied",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                                        color = LensColors.OnPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 3-Column Specs Bento Grid
                            val panelsCount = ((recommendedPvKw * 1000) / 400).toInt().coerceAtLeast(2)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = LensColors.SurfaceContainerLowest,
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Column 1: Capacity
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "CAPACITY",
                                            style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                            color = LensColors.OnSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", recommendedPvKw)} kW",
                                            style = LensTypography.headlineSm,
                                            color = LensColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$panelsCount Mono Panels",
                                            style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                            color = LensColors.OnSurfaceVariant
                                        )
                                    }

                                    // Column 2: Monthly Gen
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        color = LensColors.SurfaceContainerLow
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = "MONTHLY GEN",
                                                style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                                color = LensColors.OnSurfaceVariant
                                            )
                                            Text(
                                                text = "~${solarMonthlyGen.toInt()}",
                                                style = LensTypography.headlineSm,
                                                color = LensColors.OnSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "kWh / month",
                                                style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                                color = LensColors.OnSurfaceVariant
                                            )
                                        }
                                    }

                                    // Column 3: Est. Savings
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "EST. SAVINGS",
                                            style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                            color = LensColors.OnSurfaceVariant
                                        )
                                        val monthlySavings = (annualSavings / 12.0).toInt()
                                        Text(
                                            text = "₹${String.format(Locale.US, "%,d", monthlySavings)}",
                                            style = LensTypography.headlineSm,
                                            color = LensColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "per month",
                                            style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                                            color = LensColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Visual Generation Coverage Bar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Offsets ~${offsetPct.toInt().coerceIn(0, 100)}% of your current usage",
                                        style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                        color = LensColors.OnSurfaceVariant
                                    )
                                    Text(
                                        text = "${solarMonthlyGen.toInt()} of ${totalMonthlyKwh.toInt()} kWh",
                                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                        color = LensColors.Primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(LensColors.SurfaceContainerHigh)
                                ) {
                                    val coverageRatio = if (totalMonthlyKwh > 0.0) (solarMonthlyGen / totalMonthlyKwh).toFloat().coerceIn(0f, 1f) else 0f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = coverageRatio)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(LensColors.Primary)
                                    )
                                }
                            }

                            // Transparent Footnote
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = LensColors.OnSurfaceVariant,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(top = 1.dp)
                                )
                                Text(
                                    text = "Estimates are based on your verified appliances and standard rooftop sunlight in Tamil Nadu. Actual output depends on your shade-free roof space and weather conditions.",
                                    style = LensTypography.bodyMd.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                    color = LensColors.OnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 7. Bottom Action Buttons (Stitch Layout)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action: Explore Solar Options
                    Button(
                        onClick = onNavigateHome,
                        enabled = hasValidInventory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LensColors.PrimaryContainer,
                            contentColor = LensColors.OnPrimary,
                            disabledContainerColor = LensColors.PrimaryContainer.copy(alpha = 0.35f),
                            disabledContentColor = LensColors.OnPrimary.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Explore Solar Options",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Secondary Action: Start a New Audit
                    OutlinedButton(
                        onClick = onScanAnother,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = LensColors.SurfaceContainerLowest,
                            contentColor = LensColors.OnSurface
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = LensColors.OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Start a New Audit",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                color = LensColors.OnSurface
                            )
                        }
                    }

                    // Tertiary Action: Export Energy Report (.md)
                    TextButton(
                        onClick = {
                            val exportResult = AuditReportExporter.exportReport(context, rooms)
                            exportResult.onSuccess { file ->
                                val shareResult = AuditReportExporter.shareReport(context, file)
                                shareResult.onSuccess {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Energy audit report exported: ${file.name}"
                                        )
                                    }
                                }.onFailure { err ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Report created (${file.name}), but share failed: ${err.message ?: "Unknown error"}"
                                        )
                                    }
                                }
                            }.onFailure { err ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Failed to export report: ${err.message ?: "Unknown error"}"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Export Energy Report (.md)",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                color = LensColors.Primary
                            )
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
