package com.example.feature.lens.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.example.feature.lens.detector.MlKitApplianceDetector
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.example.feature.lens.utils.DetectedAppliance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import androidx.activity.compose.BackHandler
import com.google.accompanist.permissions.rememberPermissionState
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LensScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        viewModel.resetToIdle()
        onNavigateBack()
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LensUiState.Review) {
            onNavigateToReview()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        if (uiState is LensUiState.Error) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Couldn't analyze your scan.",
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
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        viewModel.resetToIdle()
                        onNavigateBack()
                    }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LensCameraContent(viewModel, uiState, onNavigateBack)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission is required to scan appliances.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    viewModel.resetToIdle()
                    onNavigateBack()
                }) {
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
    uiState: LensUiState,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentFrameDetections by viewModel.currentFrameDetections.collectAsState()
    val scanSession by viewModel.scanSession.collectAsState()

    var progress by remember { mutableFloatStateOf(0f) }
    val detector = remember { MlKitApplianceDetector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysisScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w("LensScreen", "Failed to unbind camera provider on dispose", e)
            }
            analysisExecutor.shutdown()
            detector.close()
        }
    }

    // 15-second Scan session timer
    LaunchedEffect(uiState) {
        if (uiState is LensUiState.Scanning) {
            val scanDurationMs = 15000L
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - startTime

                if (elapsed >= scanDurationMs) {
                    break
                }

                progress = (elapsed.toFloat() / scanDurationMs).coerceIn(0f, 1f)
                delay(100L)
            }

            if (isActive) {
                viewModel.requestFinalizeScan()
            }
        } else {
            progress = 0f
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SolarEase Lens") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetToIdle()
                        onNavigateBack()
                    }) {
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
            // Live Camera Preview with ImageAnalysis
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
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .build()

                        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            analysisScope.launch {
                                try {
                                    val candidates = detector.processFrame(imageProxy)

                                    var cachedRotatedBitmap: Bitmap? = null
                                    val cropProvider: (android.graphics.RectF) -> ByteArray? = { relRect ->
                                        try {
                                            if (cachedRotatedBitmap == null) {
                                                val rawBitmap = imageProxy.toBitmap()
                                                val rotation = imageProxy.imageInfo.rotationDegrees
                                                cachedRotatedBitmap = if (rotation != 0) {
                                                    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                                    Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                                                } else rawBitmap
                                            }
                                            val bmp = cachedRotatedBitmap
                                            if (bmp != null && !bmp.isRecycled) {
                                                val leftPx = (relRect.left * bmp.width).toInt().coerceIn(0, bmp.width - 1)
                                                val topPx = (relRect.top * bmp.height).toInt().coerceIn(0, bmp.height - 1)
                                                val rightPx = (relRect.right * bmp.width).toInt().coerceIn(leftPx + 1, bmp.width)
                                                val bottomPx = (relRect.bottom * bmp.height).toInt().coerceIn(topPx + 1, bmp.height)

                                                val cropWidth = rightPx - leftPx
                                                val cropHeight = bottomPx - topPx
                                                if (cropWidth > 15 && cropHeight > 15) {
                                                    val crop = Bitmap.createBitmap(bmp, leftPx, topPx, cropWidth, cropHeight)
                                                    val stream = ByteArrayOutputStream()
                                                    crop.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                                    stream.toByteArray()
                                                } else null
                                            } else null
                                        } catch (e: Exception) {
                                            Log.w("LensScreen", "Crop extraction error", e)
                                            null
                                        }
                                    }

                                    viewModel.onLocalCandidatesDetected(candidates, cropProvider)
                                } catch (e: Exception) {
                                    Log.e("LensScreen", "Frame analysis failed", e)
                                } finally {
                                    imageProxy.close()
                                }
                            }
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
                            Log.e("LensScreen", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for current-frame bounding boxes: only rendered when actively scanning
            if (uiState is LensUiState.Scanning) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (detection in currentFrameDetections) {
                            val relRect = detection.boundingBoxRelative
                            val left = relRect.left * size.width
                            val top = relRect.top * size.height
                            val right = relRect.right * size.width
                            val bottom = relRect.bottom * size.height

                            val width = (right - left).coerceAtLeast(0f)
                            val height = (bottom - top).coerceAtLeast(0f)

                            val boxColor = when {
                                detection.isVerified -> Color(0xFF4CAF50)
                                detection.isVerifying -> Color(0xFFFFB300)
                                else -> Color(0xFF29B6F6)
                            }
                            val strokeWidth = if (detection.isVerified) 6f else 4f

                            drawRect(
                                color = boxColor,
                                topLeft = Offset(left, top),
                                size = Size(width, height),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                    }

                    // Render Labels above bounding boxes
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        for (detection in currentFrameDetections) {
                            val relRect = detection.boundingBoxRelative
                            val left = (relRect.left * maxWidth.value).coerceIn(8f, (maxWidth.value - 140f).coerceAtLeast(8f))
                            val top = (relRect.top * maxHeight.value).coerceIn(8f, (maxHeight.value - 40f).coerceAtLeast(8f))

                            Column(
                                modifier = Modifier
                                    .offset(x = left.dp, y = top.dp)
                                    .background(Color.Black.copy(alpha = 0.80f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = detection.label,
                                    color = if (detection.isVerified) Color(0xFF4CAF50) else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (detection.isVerified && detection.powerEstimateStr.isNotBlank()) {
                                    Text(
                                        text = detection.powerEstimateStr,
                                        color = Color(0xFF81C784),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Status overlay and Scan Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is LensUiState.Finalizing -> {
                        Text(
                            text = "Finalizing scan results...",
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    is LensUiState.Scanning -> {
                        Text(
                            text = if (currentFrameDetections.any { it.isVerified }) "Appliance confirmed!" else "Scanning appliances... slowly pan around the room",
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confirmed ${scanSession.size} appliance${if (scanSession.size == 1) "" else "s"}",
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(0.6f),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.requestFinalizeScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp)
                        ) {
                            Text(
                                "Finish Early",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "Point your camera around the room to scan appliances",
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.startScanning() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(56.dp)
                        ) {
                            Text(
                                "Start 15s Scan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
