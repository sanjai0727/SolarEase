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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun RequestConfirmationRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateHome: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    RequestConfirmationScreen(
        docketId = state.docketId,
        vendorName = state.selectedVendorName,
        capacityKw = state.recommendedCapacityKw,
        systemType = state.systemType,
        panelType = if (state.panelType.contains("Non-DCR")) "Non-DCR" else "DCR",
        dateTime = state.submissionTimestamp,
        onNavigateHome = onNavigateHome
    )
}

@Composable
fun RequestConfirmationScreen(
    docketId: String,
    vendorName: String,
    capacityKw: Double,
    systemType: String,
    panelType: String,
    dateTime: String = "Today, 14:40 IST",
    onNavigateHome: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            StitchVendorTopBar(
                title = "Request Sent",
                onNavigateBack = onNavigateHome
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View My Requests
                Button(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimaryContainer,
                        contentColor = StitchOnPrimary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "VIEW MY REQUESTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Back to Recommendations
                OutlinedButton(
                    onClick = onNavigateHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = StitchOnSurface
                    )
                ) {
                    Text(
                        text = "Back to Recommendations",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Visual & Badges
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                StitchHeroIllustration(
                    drawableRes = R.drawable.ic_stitch_request_submitted,
                    contentDescription = "Solar installation confirmed"
                )

                // Check circle badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-10).dp, y = (-2).dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = StitchOnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Submission Confirmed pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StitchPrimaryFixed.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "SUBMISSION CONFIRMED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimaryContainer,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Main Headings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Request sent",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Your request has been sent to $vendorName. You can check the status from My Requests.",
                    fontSize = 14.sp,
                    color = StitchSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Telemetry Details Card
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
                    // Docket ID Highlight Row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = StitchSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "DOCKET ID",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSecondary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Text(
                                text = docketId,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = StitchOnSurface
                            )
                        }
                    }

                    // Vendor Name
                    TelemetryRow(label = "Vendor Name", value = vendorName, isBold = true)

                    // Recommended System
                    TelemetryRow(
                        label = "Recommended System",
                        value = "${String.format("%.1f", capacityKw)} kW $systemType ($panelType)",
                        isBold = true
                    )

                    // Request status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Request status",
                            fontSize = 13.sp,
                            color = StitchSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StitchSurfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(StitchPrimaryContainer)
                                )
                                Text(
                                    text = "Waiting for vendor response",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchOnSurface
                                )
                            }
                        }
                    }

                    // Date & Time
                    TelemetryRow(label = "Date & Time", value = dateTime)
                }
            }

            // Next Steps Info Box ("What happens next?")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = StitchTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "What happens next?",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }

                    // Step 1
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StitchSurfaceContainerHigh,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                            }
                        }

                        Text(
                            text = "$vendorName reviews your electricity usage and rooftop space.",
                            fontSize = 12.5.sp,
                            color = StitchSecondary,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Step 2
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StitchSurfaceContainerHigh,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "2",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                            }
                        }

                        Text(
                            text = "You will receive an itemized proposal with panel models, inverter ratings, and subsidy breakdown.",
                            fontSize = 12.5.sp,
                            color = StitchSecondary,
                            lineHeight = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = StitchSecondary
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = StitchOnSurface
        )
    }
}

