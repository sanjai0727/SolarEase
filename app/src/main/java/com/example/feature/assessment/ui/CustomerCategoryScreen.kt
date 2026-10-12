package com.example.feature.assessment.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun CustomerCategoryRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    CustomerCategoryScreen(
        selectedCategory = state.propertyType,
        onSelectCategory = { vendorViewModel.selectPropertyType(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun CustomerCategoryScreen(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "Location Verification",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 2,
                    totalSteps = 8,
                    stepTitle = "Location Type",
                    category = "25% Complete"
                )
            }
        },
        bottomBar = {
            Surface(
                color = StitchSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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
                drawableRes = R.drawable.ic_stitch_property_type,
                contentDescription = "Property Type Illustration"
            )

            // Header info
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Where will you install solar?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Choose the property where the solar system will be installed.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // Exactly 2 options matching Stitch design
            // 1. Home
            StitchSelectableCard(
                selected = selectedCategory == "Home" || selectedCategory == "Residential",
                title = "Home",
                subtitle = "Home — For residential houses, villas, or flats",
                icon = Icons.Default.Home,
                onClick = { onSelectCategory("Home") }
            )

            // 2. Business
            StitchSelectableCard(
                selected = selectedCategory == "Business" || selectedCategory == "Commercial",
                title = "Business",
                subtitle = "Business — For shops, offices, clinics and commercial spaces",
                icon = Icons.Default.Business,
                onClick = { onSelectCategory("Business") }
            )

            // Why we ask this?
            StitchInfoCard(
                icon = Icons.Default.Lightbulb,
                title = "Why we ask this?",
                description = "Tariff rates, sanction loads, and government subsidies (like PM Surya Ghar) depend on whether your setup is residential or commercial."
            )
        }
    }
}

