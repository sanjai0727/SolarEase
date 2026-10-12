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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun PanelSelectionRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    PanelSelectionScreen(
        selectedPanelType = state.panelType,
        onSelectPanelType = { vendorViewModel.selectPanelType(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun PanelSelectionScreen(
    selectedPanelType: String,
    onSelectPanelType: (String) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val isDcr = selectedPanelType == "DCR Panels" || selectedPanelType == "DCR"

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "Vendor Discovery",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 8,
                    totalSteps = 8,
                    stepTitle = "Panel Specification"
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StitchPrimaryButton(
                    text = "Continue to Find Vendors",
                    onClick = onNavigateNext
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StitchSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Preference saved to vendor quote generator",
                        fontSize = 11.5.sp,
                        color = StitchSecondary
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Hero: Faithful Stitch Illustration
            StitchHeroIllustration(
                drawableRes = R.drawable.ic_stitch_panel_type,
                contentDescription = "Solar PV Panel Type Illustration"
            )

            // Header titles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Choose your panel type",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Select your solar module manufacturing compliance category.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // 1. DCR Panels Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPanelType("DCR Panels") },
                shape = RoundedCornerShape(16.dp),
                color = if (isDcr) StitchSurfaceContainerLowest else StitchSurfaceContainerLow,
                border = BorderStroke(
                    width = if (isDcr) 1.5.dp else 1.dp,
                    color = if (isDcr) StitchPrimaryContainer else StitchOutline.copy(alpha = 0.4f)
                ),
                shadowElevation = if (isDcr) 1.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (isDcr) StitchPrimaryContainer else StitchSurfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDcr) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(StitchOnPrimary)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "DCR Panels",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "(Domestic Content Requirement)",
                                    fontSize = 12.sp,
                                    color = StitchSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchPrimaryContainer.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Subsidy Eligible",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "May be eligible for applicable government subsidy schemes, subject to current rules.",
                        fontSize = 13.sp,
                        color = StitchSecondary,
                        modifier = Modifier.padding(start = 30.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            // 2. Non-DCR Panels Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPanelType("Non-DCR Panels") },
                shape = RoundedCornerShape(16.dp),
                color = if (!isDcr) StitchSurfaceContainerLowest else StitchSurfaceContainerLow,
                border = BorderStroke(
                    width = if (!isDcr) 1.5.dp else 1.dp,
                    color = if (!isDcr) StitchPrimaryContainer else StitchOutline.copy(alpha = 0.4f)
                ),
                shadowElevation = if (!isDcr) 1.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (!isDcr) StitchPrimaryContainer else StitchSurfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!isDcr) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(StitchOnPrimary)
                                    )
                            }
                        }

                        Text(
                            text = "Non-DCR Panels",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }

                    Text(
                        text = "Offers additional panel choices, but subsidy eligibility may differ.",
                        fontSize = 13.sp,
                        color = StitchSecondary,
                        modifier = Modifier.padding(start = 30.dp),
                        lineHeight = 18.sp
                    )
                }
            }

            // Why does panel type matter? Card
            StitchInfoCard(
                icon = Icons.Default.HelpOutline,
                title = "Why does panel type matter?",
                description = "Government subsidies under PM Surya Ghar require DCR (Made in India) cells and modules. Non-DCR panels can be imported and might offer different pricing."
            )
        }
    }
}

