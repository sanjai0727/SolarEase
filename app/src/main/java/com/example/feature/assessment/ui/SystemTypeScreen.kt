package com.example.feature.assessment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun SystemTypeRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    SystemTypeScreen(
        selectedSystemType = state.systemType,
        onSelectSystemType = { vendorViewModel.selectSystemType(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun SystemTypeScreen(
    selectedSystemType: String,
    onSelectSystemType: (String) -> Unit,
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
                    currentStep = 3,
                    totalSteps = 8,
                    stepTitle = "System Configuration"
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
            // Visual Hero Illustration from Stitch
            StitchHeroIllustration(
                drawableRes = R.drawable.ic_stitch_solar_system,
                contentDescription = "Solar System Type Illustration"
            )

            // Header Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "What type of solar system do you need?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Choose the configuration that best matches your energy goals.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // 1. On-Grid
            StitchSelectableCard(
                selected = selectedSystemType == "On-Grid",
                title = "☀️ On-Grid",
                subtitle = "Works with the electricity grid. Send excess solar back to DISCOM to reduce your bill.",
                icon = Icons.Default.WbSunny,
                onClick = { onSelectSystemType("On-Grid") }
            )

            // 2. Off-Grid
            StitchSelectableCard(
                selected = selectedSystemType == "Off-Grid",
                title = "🔋 Off-Grid",
                subtitle = "Works independently with battery storage. Great for areas with frequent power cuts.",
                icon = Icons.Default.BatteryChargingFull,
                onClick = { onSelectSystemType("Off-Grid") }
            )

            // 3. Hybrid
            StitchSelectableCard(
                selected = selectedSystemType == "Hybrid",
                title = "⚡ Hybrid",
                subtitle = "Solar + battery + grid. Maximum backup protection plus grid bill savings.",
                icon = Icons.Default.Bolt,
                onClick = { onSelectSystemType("Hybrid") }
            )

            // Helpful Tip Card
            StitchInfoCard(
                icon = Icons.Default.Lightbulb,
                title = "Need help deciding?",
                description = "Most homes in Tamil Nadu choose On-Grid because it is the most affordable and qualifies for full DBT subsidies."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

