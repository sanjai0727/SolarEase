package com.example.feature.lens.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.feature.lens.utils.ApplianceAnalyzer
import com.example.feature.lens.utils.DetectedAppliance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LensScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        LensCameraContent(viewModel, onNavigateBack, onNavigateToReview)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to scan appliances.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensCameraContent(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val uiState by viewModel.uiState.collectAsState()
    val currentFrameDetections by viewModel.currentFrameDetections.collectAsState()
    val scanSession by viewModel.scanSession.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is LensUiState.Review) {
            onNavigateToReview()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SolarEase Lens") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(
                                    Executors.newSingleThreadExecutor(),
                                    ApplianceAnalyzer { detections ->
                                        viewModel.updateDetections(detections)
                                    }
                                )
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (exc: Exception) {
                            Log.e("LensScreen", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for bounding boxes
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (detection in currentFrameDetections) {
                        val relRect = detection.boundingBoxRelative
                        
                        // Scale relative coordinates to canvas size
                        val left = relRect.left * size.width
                        val top = relRect.top * size.height
                        val right = relRect.right * size.width
                        val bottom = relRect.bottom * size.height
                        
                        drawRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 8f)
                        )
                    }
                }

                // Render Labels above bounding boxes
                for (detection in currentFrameDetections) {
                    val relRect = detection.boundingBoxRelative
                    
                    // Simple offset placement
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth() // we will control position below
                                .padding(
                                    // Use BoxWithConstraints or simple estimation
                                    // We will use standard modifiers, but this is a simplified overlay
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {}
                    }
                }
            }

            // A cleaner way to render labels using BoxWithConstraints
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                for (detection in currentFrameDetections) {
                    val relRect = detection.boundingBoxRelative
                    val left = relRect.left * maxWidth.value
                    val bottom = relRect.bottom * maxHeight.value
                    
                    Column(
                        modifier = Modifier
                            .offset(x = left.dp, y = (bottom + 8).dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = detection.label.replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = detection.powerEstimateStr,
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Status overlay and Stop Scanning button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Unique Appliances Found: ${scanSession.size}",
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { viewModel.stopScanning() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
                ) {
                    Text("Stop Scanning", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
