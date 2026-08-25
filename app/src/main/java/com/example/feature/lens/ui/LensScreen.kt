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
    onNavigateToDetails: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        LensCameraContent(viewModel, onNavigateBack, onNavigateToDetails)
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
    onNavigateToDetails: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentDetection by viewModel.currentDetection.collectAsState()

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
                                    ApplianceAnalyzer { detection ->
                                        viewModel.updateDetection(detection)
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
            currentDetection?.let { detection ->
                // Map the detection coordinates (which are in image space) to the view space.
                // For simplicity in this implementation, we draw a generic box in the center,
                // or if we had complex coordinate mapping, we'd map it here.
                // Assuming the bounding box is a relative area to the preview screen.
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            viewModel.selectAppliance(detection)
                            onNavigateToDetails()
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Drawing a simple centered box as an approximation since mapping from
                        // imageProxy resolution to PreviewView is complex without exact matrix.
                        val width = size.width * 0.6f
                        val height = size.height * 0.4f
                        val left = (size.width - width) / 2
                        val top = (size.height - height) / 2
                        
                        drawRect(
                            color = Color(0xFF4CAF50),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 8f)
                        )
                    }

                    // Label Overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 100.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = detection.label.replaceFirstChar { it.uppercase() },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = detection.powerEstimateStr,
                            color = Color(0xFF4CAF50),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to select",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            if (currentDetection == null) {
                Text(
                    text = "Point the camera at an appliance",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 64.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
