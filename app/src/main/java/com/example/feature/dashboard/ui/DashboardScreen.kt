package com.example.feature.dashboard.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.dashboard.domain.model.CustomerDashboard
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardRoute(
    onNavigateToMySolarSystem: () -> Unit = {},
    onNavigateToPerformance: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToEnvironmental: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = { DashboardBottomNavigation() }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            androidx.compose.animation.Crossfade(targetState = uiState, label = "DashboardStateTransition") { state ->
                when (state) {
                    is DashboardUiState.Loading -> {
                        DashboardLoadingSkeleton()
                    }
                    is DashboardUiState.Success -> {
                        DashboardContent(
                            dashboardData = state.dashboardData,
                            onNavigateToMySolarSystem = onNavigateToMySolarSystem,
                            onNavigateToPerformance = onNavigateToPerformance,
                            onNavigateToFinancial = onNavigateToFinancial,
                            onNavigateToEnvironmental = onNavigateToEnvironmental
                        )
                    }
                    is DashboardUiState.Empty -> {
                        DashboardEmptyState(message = state.message)
                    }
                    is DashboardUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(modifier = Modifier.width(150.dp).height(24.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.width(100.dp).height(32.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.width(80.dp).height(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // System Summary
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Today's summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(3) {
                Box(modifier = Modifier.width(120.dp).height(100.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Lifetime summary
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
            Box(modifier = Modifier.weight(1f).height(120.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
        }
    }
}

@Composable
fun DashboardEmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SolarPower,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "System Successfully Installed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Live monitoring will begin once the inverter is connected.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun DashboardContent(dashboardData: CustomerDashboard, onNavigateToMySolarSystem: () -> Unit = {}, onNavigateToPerformance: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToEnvironmental: () -> Unit = {}) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 } }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(modifier = Modifier.padding(24.dp)) {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = if (currentHour < 12) "Good Morning" else if (currentHour < 18) "Good Afternoon" else "Good Evening"
            
            Text(text = greeting, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = dashboardData.customerName, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Your solar system is generating clean energy.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            
            Surface(
                shape = CircleShape,
                color = if (dashboardData.systemStatus == "Online") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (dashboardData.systemStatus == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dashboardData.systemStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dashboardData.systemStatus == "Online") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // System Summary Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("System Summary", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("${dashboardData.capacity} kW", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Capacity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(dashboardData.systemType, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        Text("Type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Installed On", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Text(dashboardData.installationDate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Text(if (dashboardData.systemStatus == "Online") "Generating" else "Standby", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Today's Summary
        Text(
            text = "Today's Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SummaryCard(
                    title = "Generation",
                    value = "${dashboardData.todayGeneration} kWh",
                    icon = Icons.Default.Bolt,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            item {
                SummaryCard(
                    title = "Savings",
                    value = currencyFormat.format(dashboardData.todaySavings),
                    icon = Icons.Default.Savings,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            item {
                SummaryCard(
                    title = "CO₂ Saved",
                    value = "${dashboardData.todayCo2} kg",
                    icon = Icons.Default.Eco,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Lifetime Summary
        Text(
            text = "Lifetime Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Generation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${String.format(Locale.US, "%,.0f", dashboardData.lifetimeGeneration)} kWh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Savings", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = currencyFormat.format(dashboardData.lifetimeSavings), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "CO₂ Saved", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${String.format(Locale.US, "%,.1f", dashboardData.lifetimeCo2 / 1000)} Tons", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Plant Health", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${dashboardData.plantHealth}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Quick Actions (2-column pseudo-grid since LazyVerticalGrid can't be easily inside verticalScroll without fixed height)
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val actions = listOf(
                QuickAction("My Solar System", "View layout & equipment", Icons.Default.SolarPower, onClick = onNavigateToMySolarSystem),
                QuickAction("Performance", "Detailed analytics", Icons.Default.Analytics, onClick = onNavigateToPerformance),
                QuickAction("Financial", "Savings & reports", Icons.Default.AccountBalanceWallet, onClick = onNavigateToFinancial),
                QuickAction("Environmental", "Green impact", Icons.Default.EnergySavingsLeaf, onClick = onNavigateToEnvironmental),
                QuickAction("Documents", "Invoices & manuals", Icons.Default.Description),
                QuickAction("Warranty", "Coverage details", Icons.Default.VerifiedUser),
                QuickAction("Support", "Get help", Icons.Default.SupportAgent),
                QuickAction("Settings", "App preferences", Icons.Default.Settings)
            )
            
            for (i in actions.indices step 2) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(modifier = Modifier.weight(1f), action = actions[i])
                    if (i + 1 < actions.size) {
                        QuickActionCard(modifier = Modifier.weight(1f), action = actions[i + 1])
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Notification Preview
        if (dashboardData.notifications.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { /* View all */ }) {
                    Text("View All")
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    dashboardData.notifications.take(2).forEachIndexed { index, notification ->
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = notification.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text(text = formatTimeAgo(notification.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                        }
                        if (index == 0 && dashboardData.notifications.size > 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Recent Activity
        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                dashboardData.recentActivities.take(4).forEachIndexed { index, activity ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            if (index < dashboardData.recentActivities.size - 1 && index < 3) {
                                Box(modifier = Modifier.width(2.dp).height(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = activity.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = formatTimeAgo(activity.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.width(140.dp).height(120.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
                Text(text = title, style = MaterialTheme.typography.labelMedium, color = contentColor.copy(alpha = 0.8f))
            }
        }
    }
}

data class QuickAction(val title: String, val description: String, val icon: ImageVector, val onClick: () -> Unit = {})

@Composable
fun QuickActionCard(modifier: Modifier = Modifier, action: QuickAction) {
    Card(
        modifier = modifier.height(130.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = action.onClick
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Icon(imageVector = action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = action.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = action.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun DashboardBottomNavigation() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf(
        Triple("Home", Icons.Default.Home, Icons.Default.Home),
        Triple("Performance", Icons.Default.Analytics, Icons.Default.Analytics),
        Triple("Documents", Icons.Default.Description, Icons.Default.Description),
        Triple("Support", Icons.Default.SupportAgent, Icons.Default.SupportAgent),
        Triple("Profile", Icons.Default.Person, Icons.Default.Person)
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.second, contentDescription = item.first) },
                label = { Text(text = item.first, style = MaterialTheme.typography.labelSmall) },
                selected = selectedItem == index,
                onClick = { selectedItem = index },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / 86400000
    val hours = diff / 3600000
    val minutes = diff / 60000
    
    return when {
        days > 0 -> "$days days ago"
        hours > 0 -> "$hours hours ago"
        minutes > 0 -> "$minutes minutes ago"
        else -> "Just now"
    }
}
