package com.example.feature.lens.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.cancel
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.feature.lens.detector.LensScanDetector
import com.example.feature.lens.detector.MlKitApplianceDetector
import com.example.feature.lens.ui.theme.LensColors
import com.example.feature.lens.ui.theme.LensTypography
import com.example.feature.lens.utils.DetectedAppliance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
        if (uiState is LensUiState.CameraActive) {
            viewModel.popUiState()
        } else {
            onNavigateBack()
        }
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
        if (uiState is LensUiState.CameraActive) {
            LensCameraContent(
                viewModel = viewModel,
                uiState = uiState as LensUiState.CameraActive,
                onNavigateBack = { viewModel.popUiState() }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LensColors.InverseSurface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LensColors.PrimaryLive)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LensColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(LensColors.PrimaryFixed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Camera access needed",
                    style = LensTypography.headlineMd,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Allow camera access to scan your rooms for appliances.",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Allow Camera", color = LensColors.OnPrimary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensCameraContent(
    viewModel: LensViewModel,
    uiState: LensUiState.CameraActive,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFrameDetections by viewModel.currentFrameDetections.collectAsState()
    val scanSession by viewModel.scanSession.collectAsState()

    val scanDetector = remember { LensScanDetector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysisScope = remember { CoroutineScope(Dispatchers.Default) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner, uiState.mode) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                scanDetector.close()
                analysisExecutor.shutdown()
                analysisScope.cancel()
            } catch (e: Exception) {
                Log.w("LensScreen", "Failed to unbind camera or clean up executor", e)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.InverseSurface.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LensColors.InverseOnSurface)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Scanning: ${uiState.room.name}",
                            style = LensTypography.headlineSm,
                            color = LensColors.InverseOnSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.InverseSurface.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(LensColors.PrimaryLive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Live scanning • On-device detection",
                        style = LensTypography.labelSm,
                        color = LensColors.InverseOnSurface.copy(alpha = 0.8f)
                    )
                }
            }
        },
        containerColor = LensColors.InverseSurface
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Camera Preview View
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
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()

                        try {
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                analysisScope.launch {
                                    try {
                                        val candidates = scanDetector.processFrame(imageProxy)
                                        viewModel.onLocalCandidatesDetected(candidates)
                                    } catch (e: Exception) {
                                        Log.e("LensScreen", "Frame analysis failed", e)
                                    } finally {
                                        imageProxy.close()
                                    }
                                }
                            }
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        } catch (exc: Exception) {
                            Log.e("LensScreen", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Detection overlay — clean rounded corner brackets
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (uiState.mode == CameraMode.LIVE_SCAN) {
                    for (detection in currentFrameDetections) {
                        val rel = detection.boundingBoxRelative
                        val left = rel.left * size.width
                        val top = rel.top * size.height
                        val right = rel.right * size.width
                        val bottom = rel.bottom * size.height
                        val bracketLen = 16.dp.toPx()
                        val strokeW = 2.5.dp.toPx()
                        val color = Color(0xFF22C55E)
                        val radius = 4.dp.toPx()

                        // Soft fill
                        drawRect(
                            color = color.copy(alpha = 0.06f),
                            topLeft = Offset(left, top),
                            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f))
                        )

                        // Corner brackets
                        drawLine(color, Offset(left, top + radius), Offset(left, top + bracketLen), strokeW)
                        drawLine(color, Offset(left + radius, top), Offset(left + bracketLen, top), strokeW)

                        drawLine(color, Offset(right, top + radius), Offset(right, top + bracketLen), strokeW)
                        drawLine(color, Offset(right - radius, top), Offset(right - bracketLen, top), strokeW)

                        drawLine(color, Offset(left, bottom - radius), Offset(left, bottom - bracketLen), strokeW)
                        drawLine(color, Offset(left + radius, bottom), Offset(left + bracketLen, bottom), strokeW)

                        drawLine(color, Offset(right, bottom - radius), Offset(right, bottom - bracketLen), strokeW)
                        drawLine(color, Offset(right - radius, bottom), Offset(right - bracketLen, bottom), strokeW)
                    }
                }
            }

            // Detection labels
            if (uiState.mode == CameraMode.LIVE_SCAN) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    for (detection in currentFrameDetections) {
                        val rel = detection.boundingBoxRelative
                        val left = (rel.left * maxWidth.value).coerceIn(8f, (maxWidth.value - 160f).coerceAtLeast(8f))
                        val top = (rel.top * maxHeight.value - 28f).coerceIn(8f, (maxHeight.value - 50f).coerceAtLeast(8f))

                        Surface(
                            modifier = Modifier.offset(x = left.dp, y = top.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = LensColors.InverseSurface.copy(alpha = 0.85f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    detection.label,
                                    style = LensTypography.labelSm.copy(fontSize = 11.sp),
                                    color = LensColors.InverseOnSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "~${detection.powerWatts.toInt()}W",
                                    style = LensTypography.labelSm.copy(fontSize = 10.sp),
                                    color = LensColors.PrimaryLive
                                )
                            }
                        }
                    }
                }
            }

            // Detection count indicator
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = LensColors.InverseSurface.copy(alpha = 0.8f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(LensColors.PrimaryLive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "${currentFrameDetections.size} detected",
                        style = LensTypography.labelSm,
                        color = LensColors.InverseOnSurface
                    )
                }
            }

            // Finish scan button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Button(
                    onClick = { viewModel.finishScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                ) {
                    Text(
                        "Done Scanning (${scanSession.size} found)",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                        color = LensColors.OnPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
