package com.example.feature.systemdetails.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.systemdetails.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemDetailsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SystemDetailsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        val userName = com.example.core.session.UserSession.currentUser?.customerName
                        Text(if (userName != null) "$userName's Solar System" else "My Solar System", style = MaterialTheme.typography.titleLarge)
                        Text("Your installed solar plant information", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Download */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download Summary")
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
            Crossfade(targetState = uiState, label = "SystemDetailsTransition") { state ->
                when (state) {
                    is SystemDetailsUiState.Loading -> SystemDetailsLoadingSkeleton()
                    is SystemDetailsUiState.Success -> SystemDetailsContent(state.details)
                    is SystemDetailsUiState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
fun SystemDetailsLoadingSkeleton() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
fun SystemDetailsContent(details: SolarSystemDetails) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm a", Locale.getDefault()) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: System Overview
        item {
            OverviewCard(details, dateFormat)
        }
        
        // Section 2: Live Status
        item {
            LiveStatusCard(details.liveStatus, timeFormat)
        }
        
        // Section 12: Smart Insights
        if (details.insights.isNotEmpty()) {
            item {
                InsightsSection(details.insights)
            }
        }
        
        // Section 3: Solar Panel Details
        item {
            ExpandableSection(title = "Solar Panel Details", icon = Icons.Default.GridOn) {
                PanelDetailsContent(details.panelInfo)
            }
        }
        
        // Section 4: Inverter Details
        item {
            ExpandableSection(title = "Inverter Details", icon = Icons.Default.ElectricMeter) {
                InverterDetailsContent(details.inverterInfo)
            }
        }
        
        // Section 7: System Specifications
        item {
            ExpandableSection(title = "System Specifications", icon = Icons.Default.SettingsSuggest) {
                SpecificationsContent(details.specifications)
            }
        }
        
        // Section 5: Installation Information
        item {
            ExpandableSection(title = "Installation Information", icon = Icons.Default.Handyman) {
                InstallationContent(details.installationInfo, dateFormat)
            }
        }
        
        // Section 6: Roof Layout
        item {
            ExpandableSection(title = "Roof Layout", icon = Icons.Default.Home) {
                RoofLayoutContent(details.roofLayout)
            }
        }
        
        // Section 8: Warranty Summary
        item {
            ExpandableSection(title = "Warranty Summary", icon = Icons.Default.VerifiedUser) {
                WarrantyContent(details.warranty, dateFormat)
            }
        }
        
        // Section 10: System Health
        item {
            ExpandableSection(title = "System Health", icon = Icons.Default.MonitorHeart) {
                HealthContent(details.health, dateFormat)
            }
        }
        
        // Section 9: Document Shortcuts
        item {
            ExpandableSection(title = "Documents", icon = Icons.Default.Description) {
                DocumentsContent(details.documents)
            }
        }
        
        // Section 11: Service History
        item {
            ExpandableSection(title = "Service History", icon = Icons.Default.History) {
                ServiceHistoryContent(details.serviceHistory, dateFormat)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun OverviewCard(details: SolarSystemDetails, dateFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("System Capacity", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    Text("${details.systemCapacity} kW", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
                Surface(
                    shape = CircleShape,
                    color = if (details.plantStatus == "Online") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (details.plantStatus == "Online") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = details.plantStatus,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (details.plantStatus == "Online") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoItem("Type", details.systemType, MaterialTheme.colorScheme.onPrimary)
                InfoItem("Installed", dateFormat.format(Date(details.installationDate)), MaterialTheme.colorScheme.onPrimary)
                InfoItem("Health", "${details.plantHealth}%", MaterialTheme.colorScheme.onPrimary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.size(18.dp).padding(top = 2.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(details.installationAddress, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun LiveStatusCard(liveStatus: LiveStatus?, timeFormat: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Live Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                if (liveStatus != null) {
                    Text("Updated ${timeFormat.format(Date(liveStatus.lastUpdated))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (liveStatus != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("⚡", style = MaterialTheme.typography.headlineMedium)
                        Text("${liveStatus.currentPowerOutput} kW", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Current Output", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text("${liveStatus.todayGeneration} kWh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Today's Gen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                        Text(liveStatus.weatherCondition, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("Weather", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Live Monitoring Coming Soon", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun InsightsSection(insights: List<Insight>) {
    Column {
        Text("Smart Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
        insights.forEach { insight ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = when(insight.type) {
                        "positive" -> Icons.Default.CheckCircle
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
fun ExpandableSection(
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
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        }
                    }
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
fun PanelDetailsContent(info: PanelInformation) {
    Column {
        DetailRow("Manufacturer", info.manufacturer)
        DetailRow("Model", info.model)
        DetailRow("Technology", info.technology)
        DetailRow("Wattage", "${info.wattage} W")
        DetailRow("Number of Panels", "${info.count}")
        DetailRow("Efficiency", "${info.efficiency}%")
        DetailRow("Orientation", info.orientation)
        DetailRow("Tilt Angle", info.tiltAngle)
        DetailRow("Mount Type", info.mountType)
        DetailRow("Warranty", info.warranty)
    }
}

@Composable
fun InverterDetailsContent(info: InverterInformation) {
    Column {
        DetailRow("Manufacturer", info.manufacturer)
        DetailRow("Model", info.model)
        DetailRow("Capacity", "${info.capacity} kW")
        DetailRow("Serial Number", info.serialNumber)
        DetailRow("Firmware Version", info.firmwareVersion)
        DetailRow("Warranty", info.warranty)
        DetailRow("Current Status", info.status)
    }
}

@Composable
fun SpecificationsContent(info: SystemSpecification) {
    Column {
        DetailRow("Est. Annual Generation", "${info.estimatedAnnualGeneration} kWh")
        DetailRow("Performance Ratio", "${info.performanceRatio}")
        DetailRow("DC Capacity", "${info.dcCapacity} kW")
        DetailRow("AC Capacity", "${info.acCapacity} kW")
        DetailRow("Expected Lifetime", "${info.expectedLifetime} Years")
        DetailRow("System Voltage", info.systemVoltage)
        DetailRow("Net Meter Number", info.netMeterNumber)
        DetailRow("Electricity Board", info.electricityBoard)
        DetailRow("Connection Type", info.connectionType)
    }
}

@Composable
fun InstallationContent(info: InstallationInformation, dateFormat: SimpleDateFormat) {
    Column {
        DetailRow("Installation Company", info.company)
        DetailRow("Lead Engineer", info.engineerName)
        DetailRow("Survey Date", dateFormat.format(Date(info.surveyDate)))
        DetailRow("Installation Date", dateFormat.format(Date(info.installationDate)))
        DetailRow("Commissioning Date", dateFormat.format(Date(info.commissioningDate)))
        DetailRow("Grid Sync Date", dateFormat.format(Date(info.gridSyncDate)))
        DetailRow("Completion Date", dateFormat.format(Date(info.completionDate)))
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* View Photos */ }, modifier = Modifier.fillMaxWidth()) {
            Text("View Installation Photos")
        }
    }
}

@Composable
fun RoofLayoutContent(info: RoofLayout) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().height(160.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Roofing, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Text("Roof Layout Schematic", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow("Roof Orientation", info.roofOrientation)
        DetailRow("Area Used", "${info.areaUsed} sq.m")
        DetailRow("Area Available", "${info.areaAvailable} sq.m")
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { /* View Layout */ }, modifier = Modifier.fillMaxWidth()) {
            Text("View Full Layout PDF")
        }
    }
}

@Composable
fun WarrantyContent(info: WarrantySummary, dateFormat: SimpleDateFormat) {
    Column {
        DetailRow("Panel Warranty", "${info.panelWarranty} (Expires ${dateFormat.format(Date(info.panelExpiryDate))})")
        DetailRow("Inverter Warranty", "${info.inverterWarranty} (Expires ${dateFormat.format(Date(info.inverterExpiryDate))})")
        DetailRow("Installation Warranty", "${info.installationWarranty} (Expires ${dateFormat.format(Date(info.installationExpiryDate))})")
        DetailRow("AMC Status", info.amcStatus)
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(onClick = { /* View Full */ }, modifier = Modifier.weight(1f)) {
                Text("View Full")
            }
            Button(onClick = { /* Book Service */ }, modifier = Modifier.weight(1f)) {
                Text("Book Service")
            }
        }
    }
}

@Composable
fun HealthContent(info: SystemHealth, dateFormat: SimpleDateFormat) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Overall Health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (info.overallHealth == "Excellent") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(info.overallHealth, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = if (info.overallHealth == "Excellent") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        DetailRow("Generation Efficiency", "${info.generationEfficiency}%")
        DetailRow("Last Inspection", dateFormat.format(Date(info.lastInspectionDate)))
        DetailRow("Dust & Cleaning", info.dustCleaningStatus)
        DetailRow("Next Maintenance", dateFormat.format(Date(info.nextMaintenanceDate)))
    }
}

@Composable
fun DocumentsContent(docs: List<DocumentShortcut>) {
    Column {
        docs.forEach { doc ->
            Row(modifier = Modifier.fillMaxWidth().clickable {}.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when(doc.type) {
                            "proposal" -> Icons.Default.Description
                            "invoice" -> Icons.Default.Receipt
                            "warranty" -> Icons.Default.VerifiedUser
                            "report" -> Icons.Default.Assessment
                            "approval" -> Icons.Default.ThumbUp
                            else -> Icons.Default.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(doc.title, style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Default.Download, contentDescription = "Download", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (doc != docs.last()) {
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            }
        }
    }
}

@Composable
fun ServiceHistoryContent(history: List<ServiceHistoryItem>, dateFormat: SimpleDateFormat) {
    Column {
        history.forEachIndexed { index, item ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (item.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    if (index < history.size - 1) {
                        Box(modifier = Modifier.width(2.dp).height(48.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(dateFormat.format(Date(item.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
