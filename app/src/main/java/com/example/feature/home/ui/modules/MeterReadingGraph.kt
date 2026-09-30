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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
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
 * Cumulative progression graph retained for backwards compatibility or detailed meter views.
 */
@Composable
fun MeterReadingGraph(
    points: List<MeterGraphPoint>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF7FBF8)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
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
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = Color(0xFF1E7038),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "METER READING PROGRESSION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                if (points.isNotEmpty()) {
                    val latest = points.last()
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", latest.readingValue)} kWh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E7038)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (points.isEmpty()) "Connect bill to begin tracking" else "Baseline logged (${String.format(Locale.getDefault(), "%,.0f", points.first().readingValue)} kWh)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Record daily meter readings to visualize your cycle consumption curve.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val minVal = remember(points) { points.minOf { it.readingValue } }
                val maxVal = remember(points) { points.maxOf { it.readingValue } }
                val valueRange = remember(minVal, maxVal) {
                    val diff = maxVal - minVal
                    if (diff <= 0.0) 10.0 else diff
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val topPadding = 12.dp.toPx()
                        val bottomPadding = 22.dp.toPx()
                        val chartHeight = canvasHeight - topPadding - bottomPadding
                        val leftPadding = 12.dp.toPx()
                        val rightPadding = 12.dp.toPx()
                        val chartWidth = canvasWidth - leftPadding - rightPadding

                        val gridSteps = 3
                        for (i in 0..gridSteps) {
                            val y = topPadding + (chartHeight / gridSteps) * i
                            drawLine(
                                color = GridLineColor.copy(alpha = 0.6f),
                                start = Offset(leftPadding, y),
                                end = Offset(canvasWidth - rightPadding, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        val n = points.size
                        val stepX = if (n > 1) chartWidth / (n - 1) else chartWidth

                        val path = Path()
                        val fillPath = Path()
                        val pointCoordinates = mutableListOf<Offset>()

                        for (i in 0 until n) {
                            val pt = points[i]
                            val x = leftPadding + (i * stepX)
                            val normalizedY = ((pt.readingValue - minVal) / valueRange).toFloat()
                            val y = topPadding + chartHeight - (normalizedY * chartHeight)
                            pointCoordinates.add(Offset(x, y))

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, topPadding + chartHeight)
                                fillPath.lineTo(x, y)
                            } else {
                                val prev = pointCoordinates[i - 1]
                                val cx = (prev.x + x) / 2f
                                path.cubicTo(cx, prev.y, cx, y, x, y)
                                fillPath.cubicTo(cx, prev.y, cx, y, x, y)
                            }
                        }

                        val lastCoord = pointCoordinates.last()
                        fillPath.lineTo(lastCoord.x, topPadding + chartHeight)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50).copy(alpha = 0.28f),
                                    Color(0xFF4CAF50).copy(alpha = 0.02f)
                                ),
                                startY = topPadding,
                                endY = topPadding + chartHeight
                            )
                        )

                        drawPath(
                            path = path,
                            color = Color(0xFF1E7038),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        for (i in 0 until n) {
                            val coord = pointCoordinates[i]
                            drawCircle(
                                color = Color(0xFF1E7038),
                                radius = 4.dp.toPx(),
                                center = coord
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = coord
                            )
                        }
                    }
                }
            }
        }
    }
}
