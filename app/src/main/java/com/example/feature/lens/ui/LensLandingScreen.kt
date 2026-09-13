package com.example.feature.lens.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.outlined.PhotoLibrary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensLandingScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                viewModel.processUrisAsync(context, uris)
            }
        }
    )

    androidx.compose.runtime.LaunchedEffect(uiState) {
        if (uiState is LensUiState.Review) {
            onNavigateToReview()
        }
    }

    if (uiState is LensUiState.Processing) {
        androidx.activity.compose.BackHandler {
            viewModel.cancelAnalysis()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as LensUiState.Processing).message,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = { viewModel.cancelAnalysis() }) {
                    Text("Cancel")
                }
            }
        }
        return
    }

    if (uiState is LensUiState.Error) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Couldn't analyze images",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as LensUiState.Error).message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { viewModel.restartScanning() }) {
                    Text("Try Again")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SolarEase Lens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF9FBFC)) // very light background
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Hero Section
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SolarEase",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "SolarEase\nLens",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Note: "Lens" can be colored primary by using an AnnotatedString, or just keeping it simple

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Scan your home appliances and get an estimated power consumption.",
                            fontSize = 15.sp,
                            color = Color(0xFF6B7280),
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Supporting message badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "A smarter home\ntowards a brighter tomorrow",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Simplified Hero Image Representation
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(180.dp)
                            .background(Color(0xFFE5F5E9), RoundedCornerShape(32.dp)), // Blob background color
                        contentAlignment = Alignment.Center
                    ) {
                        // Phone frame
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(150.dp)
                                .background(Color(0xFF333333), RoundedCornerShape(16.dp))
                                .border(2.dp, Color.LightGray, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // How it works section
                Text(
                    text = "How it works?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "It's simple. Just a few steps to build your energy profile.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Step 1
                StepCard(
                    number = "1",
                    icon = androidx.compose.material.icons.Icons.Default.CameraAlt,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    title = "Capture or upload",
                    desc = "Use your camera or add photos of your appliances."
                )
                // Step 2
                StepCard(
                    number = "2",
                    icon = androidx.compose.material.icons.Icons.Default.AutoAwesome,
                    iconColor = Color(0xFF2196F3),
                    iconBgColor = Color(0xFF2196F3).copy(alpha = 0.1f),
                    title = "We identify appliances",
                    desc = "SolarEase Lens detects supported appliances using AI."
                )
                // Step 3
                StepCard(
                    number = "3",
                    icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.List,
                    iconColor = Color(0xFFFF9800),
                    iconBgColor = Color(0xFFFF9800).copy(alpha = 0.1f),
                    title = "Review the results",
                    desc = "Check and edit the detected appliances."
                )
                // Step 4
                StepCard(
                    number = "4",
                    icon = androidx.compose.material.icons.Icons.Default.Edit,
                    iconColor = Color(0xFF9C27B0),
                    iconBgColor = Color(0xFF9C27B0).copy(alpha = 0.1f),
                    title = "Add usage details",
                    desc = "Set or adjust wattage, usage hours and days."
                )
                // Step 5
                StepCard(
                    number = "5",
                    icon = androidx.compose.material.icons.Icons.Default.BarChart,
                    iconColor = MaterialTheme.colorScheme.primary,
                    iconBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    title = "Get your estimate",
                    desc = "View your total power consumption and explore solar savings."
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Bottom Actions Container
            Surface(
                color = Color(0xFFF9FBFC), // matches background
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Add Photos Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clickable {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Outlined.PhotoLibrary,
                                contentDescription = "Add Photos",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Add Photos",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Select from your gallery",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    // Open Camera Button
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clickable { onOpenCamera() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CameraAlt,
                                contentDescription = "Open Camera",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = "Open Camera",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Scan your room (5-15 sec)",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepCard(
    number: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    title: String,
    desc: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.dp // very light or none
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Number Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4B5563)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
