package com.example.feature.home.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * METER READING PROGRESSION Graph
 * - Represents actual meter reading progression on Y-axis (e.g. 42,000 kWh -> 42,440 kWh).
 * - NOT a "consumption from zero" graph.
 * - Authoritative starting point for CURRENT: Last paid bill date + last paid bill meter reading.
 * - Ending point: Latest available current meter reading.
 * - Range selectors: CURRENT, 2M, 4M, 6M (default CURRENT).
 * - Left/Right navigation arrows for 2M, 4M, 6M historical windows.
 * - Prominently displays current meter reading.
 * - Interactive tap tooltip for Date and Meter Reading.
 * - Clean empty state when insufficient data exists without fabricating data.
 */
@Composable
fun MeterReadingProgressionGraph(
    points: List<MeterGraphPoint>,
    modifier: Modifier = Modifier,
    billData: TnebConsumerBillData? = null
) {
    var selectedRange by remember { mutableStateOf(MeterGraphRange.CURRENT) }
    var windowOffset by remember(selectedRange) { mutableStateOf(0) }

    val windowData = remember(selectedRange, windowOffset, points, billData) {
        MeterReadingCache.getProgressionWindow(selectedRange, windowOffset, billData)
    }

    val activePoints = windowData.points
    var selectedPointIndex by remember(activePoints) { mutableStateOf<Int?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7FBF8),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color(0xFF1E7038),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = "METER READING PROGRESSION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E7038),
                        letterSpacing = 0.6.sp
                    )
                }

                if (windowData.latestReading > 0.0) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", windowData.latestReading)} kWh",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E7038)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 2. Range Category Selectors & Left/Right Navigation ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Range Category Selectors: CURRENT | 2M | 4M | 6M
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE2E8F0).copy(alpha = 0.6f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MeterGraphRange.values().forEach { range ->
                        val isSelected = range == selectedRange
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF1E7038) else Color.Transparent)
                                .clickable {
                                    selectedRange = range
                                    windowOffset = 0
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

                // Left and Right Navigation Controls: [ ← ]  periodTitle  [ → ]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (windowData.hasOlder) {
                                windowOffset += 1
                                selectedPointIndex = null
                            }
                        },
                        enabled = windowData.hasOlder,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Older Period",
                            tint = if (windowData.hasOlder) Color(0xFF1E7038) else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = windowData.periodTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    IconButton(
                        onClick = {
                            if (windowData.hasNewer) {
                                windowOffset = max(0, windowOffset - 1)
                                selectedPointIndex = null
                            }
                        },
                        enabled = windowData.hasNewer,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Newer Period",
                            tint = if (windowData.hasNewer) Color(0xFF1E7038) else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 3. Graph Curve / Points / Empty State ──
            if (windowData.isInsufficientData || activePoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6F0).copy(alpha = 0.7f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF1E7038).copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "More meter or bill history is needed to show this period.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (activePoints.size < 2) {
                // Baseline only logged, waiting for first current reading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6F0).copy(alpha = 0.7f))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Baseline logged (${String.format(Locale.getDefault(), "%,.0f", activePoints.first().readingValue)} kWh)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Record today's meter reading to visualize your cycle progression.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Interactive Status / Tooltip Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedPointIndex != null && selectedPointIndex!! in activePoints.indices) {
                        val pt = activePoints[selectedPointIndex!!]
                        val subText = if (pt.subLabel.isNotEmpty()) " (${pt.subLabel})" else ""
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${pt.dateLabel}$subText · ${String.format(Locale.getDefault(), "%,.0f", pt.readingValue)} kWh",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E7038)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Canvas Graph
                val minVal = activePoints.minOf { it.readingValue }
                val maxVal = activePoints.maxOf { it.readingValue }
                val valueRange = if (maxVal - minVal <= 0.0) 10.0 else maxVal - minVal

                var pointCoordinates by remember { mutableStateOf<List<Offset>>(emptyList()) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(activePoints) {
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
                        val leftPadding = 52.dp.toPx() // Room for readable meter values
                        val rightPadding = 16.dp.toPx()
                        val topPadding = 12.dp.toPx()
                        val bottomPadding = 26.dp.toPx() // Room for date & subLabel

                        val chartWidth = canvasWidth - leftPadding - rightPadding
                        val chartHeight = canvasHeight - topPadding - bottomPadding

                        // 1. Subtle horizontal grid lines (0%, 50%, 100%)
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

                        val n = activePoints.size
                        val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

                        val coords = mutableListOf<Offset>()
                        val path = Path()
                        val fillPath = Path()

                        for (i in 0 until n) {
                            val pt = activePoints[i]
                            val x = leftPadding + (i * stepX)
                            val normalizedY = ((pt.readingValue - minVal) / valueRange).toFloat().coerceIn(0f, 1f)
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

                        // 2. Draw subtle filled area under line
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.25f),
                                    Color(0xFF4CAF50).copy(alpha = 0.01f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        // 3. Draw smooth curve
                        drawPath(
                            path = path,
                            color = Color(0xFF1E7038),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // 4. Draw data points
                        for (i in 0 until n) {
                            val c = coords[i]
                            val isSelected = (selectedPointIndex == i)

                            if (isSelected) {
                                // Vertical guideline
                                drawLine(
                                    color = Color(0xFF1E7038).copy(alpha = 0.5f),
                                    start = Offset(c.x, topPadding),
                                    end = Offset(c.x, topPadding + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                // Halo circle
                                drawCircle(
                                    color = Color(0xFF1E7038).copy(alpha = 0.2f),
                                    radius = 8.dp.toPx(),
                                    center = c
                                )
                            }

                            // Outer point border
                            drawCircle(
                                color = Color(0xFF1E7038),
                                radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
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

                    // Y-Axis labels (readable meter values along the left)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 2.dp, bottom = 24.dp, start = 2.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", maxVal),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", (minVal + maxVal) / 2),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%,.0f", minVal),
                            fontSize = 9.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // X-Axis starting and ending labels along bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(start = 50.dp, end = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val firstPt = activePoints.first()
                        val lastPt = activePoints.last()

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = firstPt.dateLabel,
                                fontSize = 9.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (firstPt.subLabel.isNotEmpty()) {
                                Text(
                                    text = firstPt.subLabel,
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = lastPt.dateLabel,
                                fontSize = 9.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Bold
                            )
                            if (lastPt.subLabel.isNotEmpty()) {
                                Text(
                                    text = lastPt.subLabel,
                                    fontSize = 8.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
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
