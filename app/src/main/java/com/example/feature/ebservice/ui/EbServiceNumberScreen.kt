package com.example.feature.ebservice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.feature.assessment.ui.*
import com.example.ui.theme.*

@Composable
fun EbServiceNumberRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    EbServiceNumberScreen(
        consumerNumber = state.rawConsumerNumber,
        formattedNumber = state.formattedConsumerNumber,
        isValid = state.isConsumerValid,
        onNumberChange = { vendorViewModel.updateConsumerNumber(it) },
        onClearNumber = { vendorViewModel.clearConsumerNumber() },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun EbServiceNumberScreen(
    consumerNumber: String,
    formattedNumber: String,
    isValid: Boolean,
    onNumberChange: (String) -> Unit,
    onClearNumber: () -> Unit,
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
                    currentStep = 1,
                    totalSteps = 8,
                    stepTitle = "Account Verification"
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
                    enabled = isValid,
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
                drawableRes = R.drawable.ic_stitch_consumer_number,
                contentDescription = "Consumer Number Verification"
            )

            // Header Text Group
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Consumer Number",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
                Text(
                    text = "Enter your electricity consumer number so we can use your electricity details for your solar recommendation.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // Main Consumer Number Input Card (Stitch faithful layout)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row: Bolt + Tamil Nadu (TNEB)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ELECTRICITY SERVICE CONNECTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            text = "Tamil Nadu (TNEB)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchSecondary
                        )
                    }

                    // Input row
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StitchSurfaceContainerLow,
                        border = BorderStroke(1.dp, StitchSecondaryContainer.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "#",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchSecondary
                                )

                                BasicTextField(
                                    value = consumerNumber,
                                    onValueChange = onNumberChange,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchOnSurface,
                                        letterSpacing = 0.5.sp
                                    ),
                                    cursorBrush = SolidColor(StitchPrimaryContainer),
                                    decorationBox = { innerTextField ->
                                        if (consumerNumber.isEmpty()) {
                                            Text(
                                                text = "e.g. 042345678901",
                                                fontSize = 18.sp,
                                                color = StitchSecondary.copy(alpha = 0.5f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            if (isValid) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Valid",
                                    tint = StitchPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Helper & Clear row
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
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isValid) StitchPrimaryContainer else StitchSecondary)
                            )
                            Text(
                                text = if (isValid) "Valid ${consumerNumber.length}-digit consumer number" else "Enter 11 or 12 digit consumer number",
                                fontSize = 12.sp,
                                color = if (isValid) StitchPrimaryContainer else StitchSecondary
                            )
                        }

                        Text(
                            text = "Clear",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchPrimaryContainer,
                            modifier = Modifier.clickable { onClearNumber() }
                        )
                    }
                }
            }

            // Why we ask this?
            StitchInfoCard(
                icon = Icons.Default.HelpOutline,
                title = "Why we ask this?",
                description = "Your consumer number helps connect your solar journey with your electricity connection and verify baseline demand."
            )

            // Trust & Verification Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLowest.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchPrimaryContainer.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Your data is only shared with verified DISCOM vendors you choose.",
                        fontSize = 12.sp,
                        color = StitchSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

