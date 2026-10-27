package com.example.feature.assessment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun MountTypeRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    MountTypeScreen(
        selectedMountType = state.mountType,
        onSelectMountType = { vendorViewModel.selectMountType(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun MountTypeScreen(
    selectedMountType: String,
    onSelectMountType: (String) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "System Requirements",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 5,
                    totalSteps = 8,
                    stepTitle = "Panel Location"
                )
            }
        },
        bottomBar = {
            Surface(
                color = StitchSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                StitchPrimaryButton(
                    text = "Continue",
                    onClick = onNavigateNext
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
            // Visual Hero: Faithful Stitch Vector Illustration
            StitchHeroIllustration(
                drawableRes = R.drawable.ic_stitch_mount_type,
                contentDescription = "Rooftop and ground solar mounting illustration"
            )

            // Header Group
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Where would you like to install the panels?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Choose the location where you plan to install the solar panels.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // 1. Rooftop
            StitchSelectableCard(
                selected = selectedMountType == "Rooftop",
                title = "Rooftop",
                subtitle = "On terrace, flat concrete roof, or sloped roof",
                icon = Icons.Default.Home,
                onClick = { onSelectMountType("Rooftop") }
            )

            // 2. Ground
            StitchSelectableCard(
                selected = selectedMountType == "Ground",
                title = "Ground",
                subtitle = "In an open garden, yard, or dedicated vacant plot",
                icon = Icons.Default.Nature,
                onClick = { onSelectMountType("Ground") }
            )

            // Good to know
            StitchInfoCard(
                icon = Icons.Default.Lightbulb,
                title = "Good to know",
                description = "Rooftop setups are the most popular in urban homes as they don't consume ground space and capture unshaded sunlight."
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

