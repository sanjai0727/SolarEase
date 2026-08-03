import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/InstallationTrackingScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# I will completely rewrite the file.
new_content = """package com.example.feature.order.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.order.domain.model.MilestoneStatus
import com.example.feature.order.domain.model.Milestone

@Composable
fun InstallationTrackingRoute(
    onNavigateNext: () -> Unit,
    viewModel: InstallationTrackingViewModel = viewModel()
) {
    val milestones by viewModel.milestones.collectAsState()
    var expandedStep by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(milestones) {
        if (milestones.isNotEmpty()) {
            isLoading = false
            // Find current step and expand it if nothing is expanded
            if (expandedStep == null) {
                val current = milestones.indexOfFirst { it.status == MilestoneStatus.IN_PROGRESS }
                if (current != -1) {
                    expandedStep = current
                }
            }
        }
    }
    
    val completedCount = milestones.count { it.status == MilestoneStatus.COMPLETED }
    val progressPercentage = if (milestones.isNotEmpty()) completedCount.toFloat() / milestones.size else 0f
    
    if (isLoading) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Box(modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.width(220.dp).height(32.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
            
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    
        Spacer(modifier = Modifier.height(16.dp))
    
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            val userName = com.example.core.session.UserSession.currentUser?.customerName
            Text(
                text = if (userName != null) "$userName's Installation" else "Installation Tracking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("3 New Updates", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Overall Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${(progressPercentage * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progressPercentage },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(MaterialTheme.shapes.extraLarge),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
                milestones.forEachIndexed { index, milestone ->
                    val isComplete = milestone.status == MilestoneStatus.COMPLETED
                    val isCurrent = milestone.status == MilestoneStatus.IN_PROGRESS
                    val isPending = milestone.status == MilestoneStatus.PENDING
                    val isExpanded = expandedStep == index
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedStep = if (isExpanded) null else index }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            color = when {
                                                isComplete -> MaterialTheme.colorScheme.primary
                                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = MaterialTheme.shapes.extraLarge
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isComplete) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                    } else if (isCurrent) {
                                        Box(modifier = Modifier.size(12.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.primary))
                                    } else {
                                        Box(modifier = Modifier.size(8.dp).clip(MaterialTheme.shapes.extraLarge).background(MaterialTheme.colorScheme.onSurfaceVariant))
                                    }
                                }
                                if (index < milestones.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(if (isExpanded) {
                                                if (isCurrent && milestone.key == "INSTALLATION_IN_PROGRESS") 220.dp else 120.dp
                                            } else 40.dp)
                                            .background(if (isComplete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f).padding(top = 4.dp).animateContentSize()) {
                                Text(
                                    text = milestone.title,
                                    style = if (isComplete || isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                    color = if (isComplete || isCurrent) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = milestone.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (isComplete) {
                                        Text(text = "Status: Completed", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        milestone.completedAt?.let {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "Date: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else if (isCurrent) {
                                        Text(text = "Status: In Progress", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        
                                        if (milestone.key == "INSTALLATION_IN_PROGRESS" && milestone.progress != null) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(text = milestone.currentActivity ?: "In Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                Text(text = "${milestone.progress}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { milestone.progress.toFloat() / 100f },
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.extraLarge),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                        
                                        milestone.expectedCompletion?.let {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(text = "Expected Completion: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    } else {
                                        Text(text = "Status: Pending", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (completedCount == milestones.size && milestones.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp).padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Installation Successfully Completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your solar power system is now active and generating clean energy. You can now monitor your live energy generation, savings, performance, documents, and warranty from your Customer Dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Button(
                onClick = onNavigateNext,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "Go to My Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
"""

with open(file_path, "w") as f:
    f.write(new_content)

print("Patched Tracking Screen")
