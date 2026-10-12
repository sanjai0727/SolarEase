package com.example.feature.home.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

// Blue accent theme for Consumption Analytics as required
private val BluePrimary = Color(0xFF2563EB) // Energy Blue
private val BluePrimaryLight = Color(0xFF3B82F6)
private val BlueContainer = Color(0xFFEFF6FF)
private val BlueBorder = Color(0xFFDBEAFE)
private val TextDark = Color(0xFF1E222D)
private val TextMuted = Color(0xFF64748B)
private val GridLineColor = Color(0xFFE2E8F0)

enum class ConsumptionRange(val label: String, val days: Int) {
    D7("7D", 7),
    D30("30D", 30),
    D90("90D", 90),
    M6("6M", 180),
    Y1("1Y", 365)
}

/**
 * Modern compact daily energy-consumption graph for the Home dashboard.
 * - Uses daily consumption calculated from consecutive meter readings.
 * - Range selectors: 7D, 30D, 90D (and 6M, 1Y if historical data allows).
 * - Smooth line chart with data points, kWh Y-axis, dates X-axis, and interactive tap tooltip.
 * - Clean empty / insufficient data state with zero fabricated data.
 */
@Composable
fun DailyConsumptionGraph(
    points: List<DailyConsumptionPoint>,
    modifier: Modifier = Modifier
) {
    // Determine which range selectors to show based on available historical data span
    val totalSpanDays = remember(points) {
        if (points.size < 2) 0
        else {
            val first = parseIsoDate(points.first().dateIso)
            val last = parseIsoDate(points.last().dateIso)
            if (first != null && last != null) {
                ((last.time - first.time) / (1000 * 60 * 60 * 24)).toInt()
            } else 0
        }
    }

    val availableRanges = remember(totalSpanDays) {
        val list = mutableListOf(ConsumptionRange.D7, ConsumptionRange.D30, ConsumptionRange.D90)
        if (totalSpanDays >= 120) list.add(ConsumptionRange.M6)
        if (totalSpanDays >= 300) list.add(ConsumptionRange.Y1)
        list
    }

    var selectedRange by remember { mutableStateOf(ConsumptionRange.D7) }

    // Filter points to selected period
    val filteredPoints = remember(points, selectedRange) {
        if (points.isEmpty()) emptyList()
        else {
            val lastDate = parseIsoDate(points.last().dateIso) ?: Date()
            val cutoffMs = lastDate.time - (selectedRange.days.toLong() * 24L * 60L * 60L * 1000L)
            val inRange = points.filter { pt ->
                val d = parseIsoDate(pt.dateIso)
                d == null || d.time >= cutoffMs
            }
            // If filtering results in fewer than 2 points, take up to selectedRange.days points if available
            if (inRange.isEmpty()) {
                points.takeLast(selectedRange.days)
            } else inRange
        }
    }

    var selectedPointIndex by remember(filteredPoints) { mutableStateOf<Int?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BlueBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // ── Top Header with Title and Range Selectors ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = "DAILY CONSUMPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary,
                        letterSpacing = 0.6.sp
                    )
                }

                // Range Selector Pills: 7D | 30D | 90D | (6M | 1Y)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    availableRanges.forEach { range ->
                        val isSelected = range == selectedRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) BluePrimary else Color.Transparent)
                                .clickable {
                                    selectedRange = range
                                    selectedPointIndex = null
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Check if sufficient points exist ──
            if (filteredPoints.size < 2) {
                // Clean empty/insufficient data state (zero fabricated data)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueContainer.copy(alpha = 0.5f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = BluePrimaryLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (filteredPoints.isEmpty()) {
                                "No daily readings in this period"
                            } else {
                                "1 Reading Logged (${String.format(Locale.getDefault(), "%.1f", filteredPoints[0].dailyKwh)} kWh on ${filteredPoints[0].displayDate})"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Log consecutive meter readings to reveal your daily consumption curve.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // ── Period Stats Bar ──
                val avgDaily = filteredPoints.map { it.dailyKwh }.average()
                val peakDaily = filteredPoints.maxOf { it.dailyKwh }
                val totalPeriodUnits = filteredPoints.sumOf { it.dailyKwh }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text(text = "AVERAGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", avgDaily)} kWh/d",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Column {
                            Text(text = "PEAK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", peakDaily)} kWh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Column {
                            Text(text = "TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.0f", totalPeriodUnits)} kWh",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }

                    // Active selected point indicator or helper text
                    if (selectedPointIndex != null && selectedPointIndex!! in filteredPoints.indices) {
                        val pt = filteredPoints[selectedPointIndex!!]
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BlueContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", pt.dailyKwh)} kWh · ${pt.displayDate}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                        }
                    } else {
                        Text(
                            text = "Tap point for details",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── Interactive Smooth Line Chart with Canvas ──
                val rawMax = filteredPoints.maxOf { it.dailyKwh }
                val maxY = remember(rawMax) {
                    val rounded = ceil(rawMax * 1.25)
                    if (rounded <= 0.0) 5.0 else max(5.0, rounded)
                }

                val density = LocalDensity.current
                var pointCoordinates by remember { mutableStateOf<List<Offset>>(emptyList()) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(filteredPoints) {
                                detectTapGestures { tapOffset ->
                                    if (pointCoordinates.isNotEmpty()) {
                                        val closest = pointCoordinates.indices.minByOrNull { i ->
                                            kotlin.math.abs(pointCoordinates[i].x - tapOffset.x)
                                        }
                                        selectedPointIndex = if (selectedPointIndex == closest) null else closest
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val leftPadding = 38.dp.toPx() // Room for Y-axis labels
                        val rightPadding = 14.dp.toPx()
                        val topPadding = 10.dp.toPx()
                        val bottomPadding = 24.dp.toPx() // Room for X-axis labels

                        val chartWidth = canvasWidth - leftPadding - rightPadding
                        val chartHeight = canvasHeight - topPadding - bottomPadding

                        // 1. Draw subtle horizontal grid lines (0%, 50%, 100%)
                        val gridSteps = 2
                        for (i in 0..gridSteps) {
                            val y = topPadding + (chartHeight / gridSteps) * i
                            drawLine(
                                color = GridLineColor.copy(alpha = 0.7f),
                                start = Offset(leftPadding, y),
                                end = Offset(canvasWidth - rightPadding, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val n = filteredPoints.size
                        val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

                        val coords = mutableListOf<Offset>()
                        val path = Path()
                        val fillPath = Path()

                        for (i in 0 until n) {
                            val pt = filteredPoints[i]
                            val x = leftPadding + (i * stepX)
                            val normalizedY = (pt.dailyKwh / maxY).toFloat().coerceIn(0f, 1f)
                            val y = topPadding + chartHeight - (normalizedY * chartHeight)
                            coords.add(Offset(x, y))

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, topPadding + chartHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                val prev = coords[i - 1]
                                val cx = (prev.x + x) / 2f
                                path.cubicTo(cx, prev.y, cx, y, x, y)
                                fillPath.cubicTo(cx, prev.y, cx, y, x, y)
                            }
                        }

                        val lastCoord = coords.last()
                        fillPath.lineTo(lastCoord.x, topPadding + chartHeight)
                        fillPath.close()

                        pointCoordinates = coords

                        // 2. Draw smooth vertical gradient under line
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    BluePrimaryLight.copy(alpha = 0.25f),
                                    BluePrimaryLight.copy(alpha = 0.01f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // 3. Draw smooth line
                        drawPath(
                            path = path,
                            color = BluePrimary,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // 4. Draw data points
                        for (i in 0 until n) {
                            val c = coords[i]
                            val isSelected = selectedPointIndex == i

                            if (isSelected) {
                                // Vertical highlight guideline
                                drawLine(
                                    color = BluePrimary.copy(alpha = 0.5f),
                                    start = Offset(c.x, topPadding),
                                    end = Offset(c.x, topPadding + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                // Halo circle
                                drawCircle(
                                    color = BluePrimary.copy(alpha = 0.2f),
                                    radius = 8.dp.toPx(),
                                    center = c
                                )
                            }

                            // Outer point border
                            drawCircle(
                                color = BluePrimary,
                                radius = if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx(),
                                center = c
                            )
                            // Inner white core
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = c
                            )
                        }
                    }

                    // Y-Axis labels (placed along the left side)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 2.dp, bottom = 20.dp, start = 2.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${maxY.toInt()}k",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(maxY / 2).toInt()}k",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "0",
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // X-Axis Date labels (evenly placed along the bottom)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(start = 36.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val sampleIndices = computeSampleIndices(filteredPoints.size)
                        sampleIndices.forEach { idx ->
                            val pt = filteredPoints[idx]
                            Text(
                                text = pt.displayDate,
                                fontSize = 9.sp,
                                color = if (selectedPointIndex == idx) BluePrimary else TextMuted,
                                fontWeight = if (selectedPointIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Helper to sample dates evenly across the X-axis so labels never crowd together.
 */
private fun computeSampleIndices(total: Int): List<Int> {
    if (total <= 1) return emptyList()
    if (total <= 5) return (0 until total).toList()
    return listOf(0, total / 3, (2 * total) / 3, total - 1)
}

private fun parseIsoDate(iso: String): Date? {
    return try {
        val formats = listOf("yyyy-MM-dd", "dd MMM yyyy", "dd/MM/yyyy", "dd-MM-yyyy")
        for (f in formats) {
            try {
                val d = SimpleDateFormat(f, Locale.US).parse(iso)
                if (d != null) return d
            } catch (_: Exception) {}
        }
        null
    } catch (_: Exception) {
        null
    }
}
// SolarEase Energy Blue Palette for Weekly Meter Consumption Graph
private val GraphBluePrimary = Color(0xFF2563EB)
private val GraphBluePrimaryDark = Color(0xFF1D4ED8)
private val GraphBlueContainer = Color(0xFFEFF6FF)
private val GraphBlueBorder = Color(0xFFDBEAFE)
private val AmberWarning = Color(0xFFD97706)
private val AmberContainer = Color(0xFFFFFBEB)
private val BorderColor = Color(0xFFE2E8F0)

/**
 * Compact Modern Sunday–Saturday Weekly Meter Reading Bar Chart for the Home screen.
 * - Energy Blue accent theme matching SolarEase design system.
 * - Displays 7 days (Sunday through Saturday) with horizontal swipe navigation and < > arrows.
 * - Disables navigation into future weeks (defaults to current week).
 * - Y-Axis displays actual cumulative meter reading labels.
 * - Bar heights are scaled relative to the weekly baseline.
 * - Floating labels above bars show interval consumption (+X kWh), "Base", or amber warning.
 * - Decreased readings: Displays amber warning badge "Review required" without computing negative consumption.
 * - Missing days: Kept empty without fabricating readings.
 * - Compact selection detail card with only the 3 agreed rows and "View Reading Details →" link.
 * - Integrates ReadingDetailsBottomSheet for complete history with 7D, 30D, All reading-count filters.
 */
@Composable
fun MeterReadingProgressionGraph(
    points: List<MeterGraphPoint> = emptyList(),
    modifier: Modifier = Modifier,
    billData: TnebConsumerBillData? = null,
    onEditReading: (String) -> Unit = {},
    onViewReadingDetails: (() -> Unit)? = null,
    anchorDate: Date? = null
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var showInternalBottomSheet by remember { mutableStateOf(false) }

    val weeklyData = remember(weekOffset, points, billData, anchorDate) {
        MeterReadingCache.getWeeklyGraphData(weekOffset, anchorDate)
    }

    val days = weeklyData.days
    val selectedDay = if (selectedDayIndex != null && selectedDayIndex!! in days.indices) {
        days[selectedDayIndex!!]
    } else null

    // Swipe gestures: Drag right -> older week (weekOffset + 1), Drag left -> newer week (weekOffset - 1)
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(weeklyData.hasNewer, weeklyData.hasOlder) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator > 50f) {
                            if (weeklyData.hasOlder) {
                                weekOffset += 1
                                selectedDayIndex = null
                            }
                        } else if (dragAccumulator < -50f) {
                            if (weeklyData.hasNewer) {
                                weekOffset = max(0, weekOffset - 1)
                                selectedDayIndex = null
                            }
                        }
                        dragAccumulator = 0f
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, GraphBlueBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .animateContentSize()
        ) {
            // ── 1. Top Header: Title and Prominently Displayed Latest Reading ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GraphBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = GraphBluePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "WEEKLY METER CONSUMPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraphBluePrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                if (weeklyData.latestReading != null && weeklyData.latestReading > 0.0) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", weeklyData.latestReading)} kWh",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraphBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 2. Navigation Header & View Reading Details Link ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation Controls: [ < ]  periodTitle  [ > ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (weeklyData.hasOlder) {
                                weekOffset += 1
                                selectedDayIndex = null
                            }
                        },
                        enabled = weeklyData.hasOlder,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("prev_week_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Week",
                            tint = if (weeklyData.hasOlder) GraphBluePrimary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = weeklyData.periodTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            if (weeklyData.hasNewer) {
                                weekOffset = max(0, weekOffset - 1)
                                selectedDayIndex = null
                            }
                        },
                        enabled = weeklyData.hasNewer,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("next_week_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Week",
                            tint = if (weeklyData.hasNewer) GraphBluePrimary else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // View Reading Details Action Link
                TextButton(
                    onClick = {
                        if (onViewReadingDetails != null) onViewReadingDetails()
                        else showInternalBottomSheet = true
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("view_reading_details_header_button")
                ) {
                    Text(
                        text = "View Details",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraphBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── 3. Weekly Bar Chart or Empty State ──
            if (!weeklyData.hasAnyReadingsInWeek && weeklyData.weeklyBaselineReading == null) {
                // Empty state: No readings recorded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GraphBlueContainer.copy(alpha = 0.4f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GraphBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "No meter readings recorded for this week.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Log your daily meter reading to track your weekly consumption.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Determine cumulative Y-axis scale and weekly baseline
                val recordedValues = days.mapNotNull { it.cumulativeKwh }
                val baselineVal = weeklyData.weeklyBaselineReading ?: (recordedValues.minOrNull() ?: 0.0)
                val peakVal = recordedValues.maxOrNull() ?: (baselineVal + 5.0)
                val span = max(5.0, peakVal - baselineVal)
                val maxVal = baselineVal + span
                val midVal = baselineVal + (span / 2.0)

                // Weekly Bar Chart Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(days) {
                                detectTapGestures { tapOffset ->
                                    val leftPad = 44.dp.toPx()
                                    val rightPad = 10.dp.toPx()
                                    val chartW = size.width - leftPad - rightPad
                                    val colW = chartW / 7f

                                    if (tapOffset.x >= leftPad && tapOffset.x <= size.width - rightPad) {
                                        val colIdx = ((tapOffset.x - leftPad) / colW).toInt().coerceIn(0, 6)
                                        selectedDayIndex = if (selectedDayIndex == colIdx) null else colIdx
                                    }
                                }
                            }
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height
                        val leftPad = 44.dp.toPx()
                        val rightPad = 10.dp.toPx()
                        val topPad = 18.dp.toPx()
                        val bottomPad = 24.dp.toPx()

                        val chartW = canvasW - leftPad - rightPad
                        val chartH = canvasH - topPad - bottomPad

                        // 1. Horizontal dashed grid lines (Top: maxVal, Mid: midVal, Bottom: baselineVal)
                        val gridSteps = 2
                        for (i in 0..gridSteps) {
                            val y = topPad + (chartH / gridSteps) * i
                            drawLine(
                                color = GridLineColor.copy(alpha = 0.7f),
                                start = Offset(leftPad, y),
                                end = Offset(canvasW - rightPad, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                            )
                        }

                        // 2. Bars for Sunday through Saturday
                        val colW = chartW / 7f
                        val barW = (colW * 0.44f).coerceAtLeast(13.dp.toPx())

                        for (i in 0..6) {
                            val day = days[i]
                            val centerX = leftPad + (i * colW) + (colW / 2f)
                            val isSelected = (selectedDayIndex == i)

                            if (isSelected) {
                                drawRoundRect(
                                    color = GraphBlueContainer.copy(alpha = 0.6f),
                                    topLeft = Offset(centerX - (colW / 2f) + 1.dp.toPx(), topPad - 2.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(colW - 2.dp.toPx(), chartH + 4.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                )
                            }

                            if (day.reading != null) {
                                if (day.isDecreasedReading) {
                                    val barH = (chartH * 0.22f).coerceAtLeast(8.dp.toPx())
                                    val barTop = topPad + chartH - barH
                                    drawRoundRect(
                                        color = AmberWarning,
                                        topLeft = Offset(centerX - (barW / 2f), barTop),
                                        size = androidx.compose.ui.geometry.Size(barW, barH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                } else if (day.isBaselinePoint) {
                                    val anchorH = 6.dp.toPx()
                                    val barTop = topPad + chartH - anchorH
                                    drawRoundRect(
                                        color = GraphBluePrimary,
                                        topLeft = Offset(centerX - (barW / 2f), barTop),
                                        size = androidx.compose.ui.geometry.Size(barW, anchorH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                } else if (day.cumulativeKwh != null) {
                                    val rel = (day.cumulativeKwh - baselineVal).coerceAtLeast(0.0)
                                    val fraction = if (span > 0) (rel / span).toFloat().coerceIn(0.04f, 1f) else 0.04f
                                    val barH = (chartH * fraction).coerceAtLeast(5.dp.toPx())
                                    val barTop = topPad + chartH - barH

                                    drawRoundRect(
                                        color = if (isSelected) GraphBluePrimaryDark else GraphBluePrimary,
                                        topLeft = Offset(centerX - (barW / 2f), barTop),
                                        size = androidx.compose.ui.geometry.Size(barW, barH),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // Y-Axis Labels (Actual Cumulative Meter Readings)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 11.dp, bottom = 24.dp, start = 1.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", maxVal),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", midVal),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", baselineVal),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    // Floating Labels Above Bars (Interval consumption +X kWh, Base, or ⚠)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 44.dp, end = 10.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0..6) {
                            val day = days[i]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.isDecreasedReading) {
                                    Text(
                                        text = "⚠",
                                        fontSize = 10.sp,
                                        color = AmberWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (day.isBaselinePoint) {
                                    Text(
                                        text = "Base",
                                        fontSize = 8.sp,
                                        color = GraphBluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else if (day.intervalConsumption != null) {
                                    val kwhVal = day.intervalConsumption.toInt()
                                    Text(
                                        text = "+$kwhVal",
                                        fontSize = 8.sp,
                                        color = GraphBluePrimaryDark,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // X-Axis Day of Week & Date Labels (Sun .. Sat)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(start = 44.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0..6) {
                            val day = days[i]
                            val isSel = (selectedDayIndex == i)
                            val hasReading = day.reading != null

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedDayIndex = if (isSel) null else i }
                            ) {
                                Text(
                                    text = day.dayLabel,
                                    fontSize = 10.sp,
                                    color = if (isSel) GraphBluePrimary else if (hasReading) TextDark else TextMuted.copy(alpha = 0.5f),
                                    fontWeight = if (isSel || hasReading) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = day.displayDate.split(" ").firstOrNull() ?: "",
                                    fontSize = 9.sp,
                                    color = if (isSel) GraphBluePrimary else TextMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ── 4. Compact Selection Detail Card (When Day is Selected) ──
                if (selectedDay != null && selectedDay.reading != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .animateContentSize()
                            .testTag("selected_day_tooltip"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedDay.isDecreasedReading) AmberContainer else GraphBlueContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, if (selectedDay.isDecreasedReading) AmberWarning.copy(alpha = 0.5f) else GraphBlueBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Header: Day, date, and time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedDay.fullDate}${if (selectedDay.formattedTime != null) " · " + selectedDay.formattedTime else ""}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )

                                TextButton(
                                    onClick = { onEditReading(selectedDay.dateIso) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                    modifier = Modifier.height(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit reading",
                                        tint = GraphBluePrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Edit", fontSize = 10.sp, color = GraphBluePrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            HorizontalDivider(color = (if (selectedDay.isDecreasedReading) AmberWarning else GraphBluePrimary).copy(alpha = 0.15f))

                            // Row 1: Actual meter reading
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Actual meter reading:",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%,.0f", selectedDay.cumulativeKwh)} kWh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }

                            // Row 2: Consumption since previous reading
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Consumption since previous reading:",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                if (selectedDay.isDecreasedReading) {
                                    Text(
                                        text = "Review required (lower than previous)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AmberWarning
                                    )
                                } else if (selectedDay.isBaselinePoint) {
                                    Text(
                                        text = "Baseline anchor reading",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GraphBluePrimary
                                    )
                                } else if (selectedDay.intervalConsumption != null) {
                                    Text(
                                        text = "+${String.format(Locale.getDefault(), "%,.0f", selectedDay.intervalConsumption)} kWh",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GraphBluePrimary
                                    )
                                } else {
                                    Text(
                                        text = "0 kWh",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Row 3: Weekly baseline
                            if (weeklyData.weeklyBaselineReading != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Weekly baseline:",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.0f", weeklyData.weeklyBaselineReading)} kWh (${weeklyData.weeklyBaselineDate ?: "start of week"})",
                                        fontSize = 11.sp,
                                        color = TextDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Bottom Link: View Reading Details →
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        if (onViewReadingDetails != null) onViewReadingDetails()
                                        else showInternalBottomSheet = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                    modifier = Modifier
                                        .height(24.dp)
                                        .testTag("view_reading_details_tooltip_button")
                                ) {
                                    Text(
                                        text = "View Reading Details →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GraphBluePrimary
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedDay != null && selectedDay.reading == null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDay.fullDate} · No meter reading recorded",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            TextButton(
                                onClick = { onEditReading(selectedDay.dateIso) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(
                                    text = "Log Reading",
                                    fontSize = 11.sp,
                                    color = GraphBluePrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap day for details",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Swipe ← / → to change week",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }

    if (showInternalBottomSheet) {
        ReadingDetailsBottomSheet(
            onDismiss = { showInternalBottomSheet = false },
            onEditReading = onEditReading,
            initialSelectedReadingId = selectedDay?.reading?.id,
            anchorDate = anchorDate
        )
    }
}

/**
 * Delegator for backwards-compatibility with existing code.
 */
@Composable
fun MeterReadingGraph(
    points: List<MeterGraphPoint>,
    modifier: Modifier = Modifier,
    billData: TnebConsumerBillData? = null
) {
    MeterReadingProgressionGraph(
        points = points,
        modifier = modifier,
        billData = billData
    )
}

