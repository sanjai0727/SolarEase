package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RecommendationRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    RecommendationScreen(
        recommendedCapacityKw = state.recommendedCapacityKw,
        monthlyConsumption = state.monthlyConsumptionKwh,
        mountType = state.mountType,
        systemType = state.systemType,
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun RecommendationScreen(
    recommendedCapacityKw: Double,
    monthlyConsumption: Double,
    mountType: String,
    systemType: String,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "Solar Recommendation",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 7,
                    totalSteps = 8,
                    stepTitle = "Your Solar Recommendation"
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StitchPrimaryButton(
                    text = "Continue to Panel Selection",
                    onClick = onNavigateNext
                )

                Text(
                    text = "ZERO COMMITMENT • VENDOR DISCOVERY STAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchSecondary,
                    letterSpacing = 0.8.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header block
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Your solar recommendation",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Based on your electricity usage and available installation space.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // Card 1: Recommended Plant Capacity
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "RECOMMENDED PLANT CAPACITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format("%.1f", recommendedCapacityKw),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StitchPrimaryContainer,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kW",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Optimal $systemType System",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Card 2: How It Works Flowchart
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "HOW IT WORKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowStepItem(icon = Icons.Default.WbSunny, label = "Sunlight")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StitchSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        FlowStepItem(icon = Icons.Default.SolarPower, label = "Solar Panels")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StitchSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        FlowStepItem(icon = Icons.Default.Home, label = "Home Inverter")
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StitchSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        FlowStepItem(icon = Icons.Default.Bolt, label = "Clean Power")
                    }
                }
            }

            // Card 3: Summary of Inputs
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "SUMMARY OF INPUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchSecondary,
                        letterSpacing = 0.8.sp
                    )

                    SummaryInputRow(
                        icon = Icons.Default.ElectricMeter,
                        title = "Monthly usage",
                        value = "${monthlyConsumption.toInt()} kWh"
                    )
                    SummaryInputRow(
                        icon = Icons.Default.Roofing,
                        title = "Installation",
                        value = mountType
                    )
                    SummaryInputRow(
                        icon = Icons.Default.Bolt,
                        title = "Recommended system",
                        value = "${String.format("%.1f", recommendedCapacityKw)} kW",
                        isHighlight = true
                    )
                }
            }

            // Card 4: Disclaimer Info Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "This is an estimated recommendation based on the information you provided. The final proposal will be verified by your chosen installer.",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FlowStepItem(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(StitchSurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = StitchPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = StitchOnSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun SummaryInputRow(
    icon: ImageVector,
    title: String,
    value: String,
    isHighlight: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = StitchSurfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StitchSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = StitchSecondary
                )
            }
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) StitchPrimaryContainer else StitchOnSurface
            )
        }
    }
}

