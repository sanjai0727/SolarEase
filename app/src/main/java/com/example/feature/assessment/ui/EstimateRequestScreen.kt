package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun EstimateRequestRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    EstimateRequestScreen(
        state = state,
        onNavigateNext = {
            vendorViewModel.submitEstimateRequest()
            onNavigateNext()
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun EstimateRequestScreen(
    state: SolarVendorState,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            StitchVendorTopBar(
                title = "Proposal Request",
                onNavigateBack = onNavigateBack
            )
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
                    text = "Send Request",
                    onClick = onNavigateNext
                )

                Text(
                    text = "Cancel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchSecondary,
                    modifier = Modifier
                        .clickable { onNavigateBack() }
                        .padding(vertical = 4.dp)
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
            // Progress / Step Indicator
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
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryContainer)
                    )
                    Text(
                        text = "STEP 3 OF 3 • FINAL REVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimaryContainer,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "Verified Profile",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchSecondary
                )
            }

            // Header Context with Stitch Illustration
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = StitchSurfaceContainerLow.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLowest,
                        shadowElevation = 0.5.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stitch_request_estimate),
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Request an estimate",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "Send your solar requirement to ${state.selectedVendorName} to prepare an official proposal.",
                            fontSize = 13.sp,
                            color = StitchSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Target Vendor Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = StitchSurfaceContainerHigh.copy(alpha = 0.6f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SolarPower,
                                        contentDescription = null,
                                        tint = StitchPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "RECIPIENT VENDOR",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = state.selectedVendorName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StitchSurfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = StitchPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Tier 1",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchPrimaryContainer
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = StitchSurfaceContainerHigh.copy(alpha = 0.6f), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = StitchSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Average turnaround",
                                fontSize = 12.sp,
                                color = StitchSecondary
                            )
                        }
                        Text(
                            text = "Within 24–48 hours",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurface
                        )
                    }
                }
            }

            // Primary Requirement Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Card header
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
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "REQUIREMENT SUMMARY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSecondary,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StitchSurfaceContainerLow
                        ) {
                            Text(
                                text = "Auto-calibrated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Recommended Capacity Box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RECOMMENDED CAPACITY",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSecondary,
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "Optimal Solar Array",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchOnSurface
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StitchSurfaceContainerHigh.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = String.format("%.1f", state.recommendedCapacityKw),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "kW",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Detailed Specification Rows
                    RequirementDetailRow(
                        icon = Icons.Default.Home,
                        label = "Property",
                        value = "${state.propertyType} (Residential)"
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.GridView,
                        label = "System Type",
                        value = state.systemType,
                        hasDot = true
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.Bolt,
                        label = "Monthly Consumption",
                        value = "${state.monthlyConsumptionKwh.toInt()} kWh / month"
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.Roofing,
                        label = "Mount Type",
                        value = state.mountType
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.Straighten,
                        label = "Available Area",
                        value = "${state.availableAreaSqM.toInt()} sq. m"
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.SolarPower,
                        label = "Panel Type",
                        value = if (state.panelType.contains("DCR")) "DCR (Subsidy Eligible)" else "Non-DCR",
                        isBadge = true
                    )
                    RequirementDetailRow(
                        icon = Icons.Default.Tag,
                        label = "Consumer ID",
                        value = state.formattedConsumerNumber,
                        isMono = true
                    )
                }
            }

            // Reassuring Explanatory Callout Note
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerHigh.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, StitchSurfaceContainerHigh.copy(alpha = 0.4f))
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
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Your requirements will be shared with this vendor so they can prepare an estimate.",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = "Free & Zero-obligation. No payment required at this stage.",
                            fontSize = 12.sp,
                            color = StitchSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RequirementDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    hasDot: Boolean = false,
    isBadge: Boolean = false,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = StitchSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = StitchSecondary
            )
        }

        if (isBadge) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StitchSurfaceContainerHigh
            ) {
                Text(
                    text = value,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (hasDot) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryContainer)
                    )
                }
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = if (isMono) FontWeight.Normal else FontWeight.Medium,
                    color = if (isMono) StitchSecondary else StitchOnSurface
                )
            }
        }
    }
}

