package com.example.feature.performance.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.performance.domain.model.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceRoute(
    onNavigateBack: () -> Unit,
    viewModel: PerformanceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        val userName = com.example.core.session.UserSession.currentUser?.username
                        Text(if (userName != null) "$userName's Performance" else "Performance", style = MaterialTheme.typography.titleLarge)
                        Text("Monitor your solar energy production", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Report")
                    }
                    IconButton(onClick = { /* Download */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download Report")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(targetState = uiState, label = "PerformanceTransition") { state ->
                when (state) {
                    is PerformanceUiState.Loading -> PerformanceLoadingSkeleton()
                    is PerformanceUiState.Success -> {
                        if (!state.data.isSystemActive) {
                            EmptyPerformanceState()
                        } else {
                            PerformanceContent(state.data)
                        }
                    }
                    is PerformanceUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun PerformanceLoadingSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
fun EmptyPerformanceState() {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.SolarPower, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(24.dp))
        Text("No Data Yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Performance data will appear once your system starts generating electricity.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun PerformanceContent(data: PerformanceDashboard) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            LiveEnergyCard(data.liveGeneration)
        }
        
        if (data.insights.isNotEmpty()) {
            item {
                PerformanceInsightsSection(data.insights)
            }
        }
        
        item {
            GenerationChartsSection(data.generationHistory)
        }
        
        item {
            EnergyFlowSection(data.energyFlow)
        }
        
        item {
            ExpandableDataSection("Today's Performance", Icons.Default.Today) {
                TodayPerformanceContent(data.todayPerformance)
            }
        }
        
        item {
            ExpandableDataSection("This Month", Icons.Default.CalendarMonth) {
                MonthPerformanceContent(data.monthPerformance, currencyFormat)
            }
        }
        
        item {
            ExpandableDataSection("Yearly Performance", Icons.Default.DateRange) {
                YearPerformanceContent(data.yearPerformance, currencyFormat)
            }
        }
        
        item {
            ExpandableDataSection("System Efficiency", Icons.Default.Speed) {
                SystemEfficiencyContent(data.systemEfficiency)
            }
        }
        
        item {
            ExpandableDataSection("Production Forecast", Icons.Default.Insights) {
                ForecastContent(data.forecast, currencyFormat)
            }
        }
        
        item {
            ReportsSection()
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LiveEnergyCard(liveData: LiveGeneration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!liveData.hasLiveMonitoring) {
                Text("Live monitoring will begin after inverter integration.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Current Output", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Text("${liveData.currentOutput} kW", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Today's Generation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text("${liveData.todayGeneration} kWh", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricItem("Grid", liveData.gridStatus, MaterialTheme.colorScheme.onPrimaryContainer)
                    MetricItem("Status", liveData.systemStatus, MaterialTheme.colorScheme.onPrimaryContainer)
                    MetricItem("Weather", liveData.weather, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                Text("Updated ${timeFormat.format(Date(liveData.lastUpdated))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun PerformanceInsightsSection(insights: List<PerformanceInsight>) {
    Column {
        Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        insights.forEach { insight ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = when(insight.type) {
                        "positive" -> Icons.Default.TrendingUp
                        "warning" -> Icons.Default.Warning
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when(insight.type) {
                        "positive" -> MaterialTheme.colorScheme.primary
                        "warning" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    },
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(insight.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

@Composable
fun GenerationChartsSection(history: GenerationHistory) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Day", "Week", "Month", "Year")
    val chartData = when (selectedTab) {
        0 -> history.daily
        1 -> history.weekly
        2 -> history.monthly
        else -> history.yearly
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Generation Charts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            SimpleBarChart(data = chartData, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SimpleBarChart(data: List<ChartDataPoint>, color: Color) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOf { it.value }
    
    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / (data.size * 2f)
            val spacing = barWidth
            
            data.forEachIndexed { index, point ->
                val barHeight = if (maxValue > 0) ((point.value / maxValue) * height * 0.8f).toFloat() else 0f
                val x = (index * (barWidth + spacing)) + (spacing / 2f)
                val y = height - barHeight - 20.dp.toPx()
                
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight.toFloat()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EnergyFlowSection(flow: EnergyFlow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Energy Flow", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(16.dp))
            
            // A simple visual representation using standard compose layout
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FlowNode(title = "Solar", value = "${flow.solarGenerated} kWh", icon = Icons.Default.SolarPower, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowNode(title = "Home", value = "${flow.consumed} kWh", icon = Icons.Default.Home, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                FlowNode(title = "Grid Export", value = "${flow.exportedToGrid} kWh", icon = Icons.Default.Output, color = MaterialTheme.colorScheme.tertiary)
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowNode(title = "Grid Import", value = "${flow.importedFromGrid} kWh", icon = Icons.Default.Input, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun FlowNode(title: String, value: String, icon: ImageVector, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(56.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExpandableDataSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "ExpandRotation")
    
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotationState)
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f, fill = false), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
fun TodayPerformanceContent(data: TodayPerformance) {
    Column {
        DetailRow("Energy Generated", "${data.energyGenerated} kWh")
        DetailRow("Peak Power", "${data.peakPower} kW")
        DetailRow("Operating Hours", "${data.operatingHours} hrs")
        DetailRow("Average Efficiency", "${data.averageEfficiency}%")
        
        val compColor = if (data.comparedToYesterday >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        val compText = if (data.comparedToYesterday >= 0) "+${data.comparedToYesterday}%" else "${data.comparedToYesterday}%"
        
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Compared to Yesterday", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(compText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = compColor)
        }
    }
}

@Composable
fun MonthPerformanceContent(data: MonthPerformance, format: NumberFormat) {
    Column {
        DetailRow("Monthly Generation", "${data.monthlyGeneration} kWh")
        DetailRow("Monthly Savings", format.format(data.monthlySavings))
        DetailRow("Average Daily Generation", "${data.averageDailyGeneration} kWh")
        DetailRow("Peak Production Day", data.peakProductionDay)
        DetailRow("Best Performing Week", data.bestPerformingWeek)
    }
}

@Composable
fun YearPerformanceContent(data: YearPerformance, format: NumberFormat) {
    Column {
        DetailRow("Annual Generation", "${data.annualGeneration} kWh")
        DetailRow("Annual Savings", format.format(data.annualSavings))
        DetailRow("Capacity Utilization", "${data.capacityUtilization}%")
        DetailRow("Total Runtime", "${data.totalRuntime} hrs")
        DetailRow("Expected vs Actual", "${data.expectedGeneration} / ${data.actualGeneration} kWh")
    }
}

@Composable
fun SystemEfficiencyContent(data: SystemEfficiency) {
    Column {
        DetailRow("Performance Ratio", "${data.performanceRatio}%")
        DetailRow("Panel Efficiency", "${data.panelEfficiency}%")
        DetailRow("Inverter Efficiency", "${data.inverterEfficiency}%")
        DetailRow("Temperature Effect Loss", "${data.temperatureEffectLoss}%")
        DetailRow("Shading Loss", "${data.shadingLoss}%")
        DetailRow("Dust Loss", "${data.dustLoss}%")
    }
}

@Composable
fun ForecastContent(data: ProductionForecast, format: NumberFormat) {
    Column {
        DetailRow("Tomorrow's Expected Generation", "${data.expectedGeneration} kWh")
        DetailRow("Expected Savings", format.format(data.expectedSavings))
        DetailRow("Weather Impact", data.weatherImpact)
        DetailRow("Confidence", data.confidence)
    }
}

@Composable
fun ReportsSection() {
    Column {
        Text("Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                Text("Daily")
            }
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                Text("Monthly")
            }
            OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                Text("Annual")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Full Report (PDF)")
        }
    }
}
