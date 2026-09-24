package com.example.feature.lens.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Camera permission is required for Solar Lens.", style = LensTypography.headlineSm, color = LensColors.SlateText)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary)
                ) {
                    Text("Grant Permission", color = LensColors.OnPrimary)
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

    val detector = remember { MlKitApplianceDetector() }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val analysisScope = remember { CoroutineScope(Dispatchers.Default) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    DisposableEffect(lifecycleOwner, uiState.mode) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w("LensScreen", "Failed to unbind camera", e)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.InverseSurface.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LensColors.InverseOnSurface)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Array Calibration: ${uiState.room.name}",
                            style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                            color = LensColors.InverseOnSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LensColors.PrimaryFixed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            "CV-SCAN",
                            style = LensTypography.labelCaps,
                            color = LensColors.PrimaryFixed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Segmented Instrument Mode Selector
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = LensColors.InverseSurface,
                border = BorderStroke(1.dp, LensColors.OutlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CameraModeTab(
                        label = "Photo",
                        icon = Icons.Default.PhotoCamera,
                        isSelected = uiState.mode == CameraMode.PHOTO,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startCamera(CameraMode.PHOTO) }
                    )
                    CameraModeTab(
                        label = "Video (10s)",
                        icon = Icons.Default.Videocam,
                        isSelected = uiState.mode == CameraMode.VIDEO,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startCamera(CameraMode.VIDEO) }
                    )
                    CameraModeTab(
                        label = "Live Scan",
                        icon = Icons.Default.DocumentScanner,
                        isSelected = uiState.mode == CameraMode.LIVE_SCAN,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.startCamera(CameraMode.LIVE_SCAN) }
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
                            when (uiState.mode) {
                                CameraMode.PHOTO -> {
                                    imageCapture = ImageCapture.Builder().build()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                                }
                                CameraMode.VIDEO -> {
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                }
                                CameraMode.LIVE_SCAN -> {
                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .build()

                                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        analysisScope.launch {
                                            try {
                                                val candidates = detector.processFrame(imageProxy)
                                                viewModel.onLocalCandidatesDetected(candidates)
                                            } catch (e: Exception) {
                                                Log.e("LensScreen", "Frame analysis failed", e)
                                            } finally {
                                                imageProxy.close()
                                            }
                                        }
                                    }
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                                }
                            }
                        } catch (exc: Exception) {
                            Log.e("LensScreen", "Camera binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Precision Viewport Graticule Canvas & HUD
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                // Center Crosshair
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                    start = Offset(cx - 24.dp.toPx(), cy),
                    end = Offset(cx - 8.dp.toPx(), cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                    start = Offset(cx + 8.dp.toPx(), cy),
                    end = Offset(cx + 24.dp.toPx(), cy),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                    start = Offset(cx, cy - 24.dp.toPx()),
                    end = Offset(cx, cy - 8.dp.toPx()),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.45f),
                    start = Offset(cx, cy + 8.dp.toPx()),
                    end = Offset(cx, cy + 24.dp.toPx()),
                    strokeWidth = 1.5f
                )
                drawCircle(
                    color = Color(0xFF10B981).copy(alpha = 0.35f),
                    radius = 4.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5f)
                )

                // CAD-style Corner Brackets for detected appliances
                if (uiState.mode == CameraMode.LIVE_SCAN) {
                    for (detection in currentFrameDetections) {
                        val rel = detection.boundingBoxRelative
                        val left = rel.left * size.width
                        val top = rel.top * size.height
                        val right = rel.right * size.width
                        val bottom = rel.bottom * size.height
                        val bracketLen = 14.dp.toPx()
                        val strokeW = 3.dp.toPx()
                        val color = Color(0xFF10B981)

                        // Interior soft tint
                        drawRect(
                            color = color.copy(alpha = 0.08f),
                            topLeft = Offset(left, top),
                            size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f))
                        )

                        // Top-Left Bracket
                        drawLine(color, Offset(left, top), Offset(left + bracketLen, top), strokeW)
                        drawLine(color, Offset(left, top), Offset(left, top + bracketLen), strokeW)

                        // Top-Right Bracket
                        drawLine(color, Offset(right, top), Offset(right - bracketLen, top), strokeW)
                        drawLine(color, Offset(right, top), Offset(right, top + bracketLen), strokeW)

                        // Bottom-Left Bracket
                        drawLine(color, Offset(left, bottom), Offset(left + bracketLen, bottom), strokeW)
                        drawLine(color, Offset(left, bottom), Offset(left, bottom - bracketLen), strokeW)

                        // Bottom-Right Bracket
                        drawLine(color, Offset(right, bottom), Offset(right - bracketLen, bottom), strokeW)
                        drawLine(color, Offset(right, bottom), Offset(right, bottom - bracketLen), strokeW)
                    }
                }
            }

            // Floating CAD Labels & Tags for Detected Appliances
            if (uiState.mode == CameraMode.LIVE_SCAN) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    for (detection in currentFrameDetections) {
                        val rel = detection.boundingBoxRelative
                        val left = (rel.left * maxWidth.value).coerceIn(8f, (maxWidth.value - 160f).coerceAtLeast(8f))
                        val top = (rel.top * maxHeight.value - 32f).coerceIn(8f, (maxHeight.value - 60f).coerceAtLeast(8f))

                        Surface(
                            modifier = Modifier
                                .offset(x = left.dp, y = top.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = LensColors.InverseSurface.copy(alpha = 0.92f),
                            border = BorderStroke(1.dp, LensColors.PrimaryLive.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(LensColors.PrimaryLive, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    detection.label.uppercase(),
                                    style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                    color = LensColors.InverseOnSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = LensColors.PrimaryContainer
                                ) {
                                    Text(
                                        "~${detection.powerWatts.toInt()}W",
                                        style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                        color = LensColors.OnPrimary,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top Status Diagnostic Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = LensColors.InverseSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, LensColors.OutlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(LensColors.PrimaryLive, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Edge Vision v2.4 (Offline Engine)",
                            style = LensTypography.labelCaps,
                            color = LensColors.InverseOnSurface
                        )
                    }
                    val inReticle = if (uiState.mode == CameraMode.LIVE_SCAN) {
                        "${currentFrameDetections.size} IN RETICLE"
                    } else {
                        "READY"
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LensColors.PrimaryContainer
                    ) {
                        Text(
                            inReticle,
                            style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                            color = LensColors.OnPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Bottom Shutter / Action Controls Layer
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                when (uiState.mode) {
                    CameraMode.PHOTO -> {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.Transparent, CircleShape)
                                .clickable {
                                    val file = File(context.cacheDir, "temp_photo.jpg")
                                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                    imageCapture?.takePicture(
                                        outputOptions,
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageSavedCallback {
                                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                val bytes = file.readBytes()
                                                viewModel.processImagesAsync(listOf(bytes), "PHOTO", uiState.room)
                                            }
                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("LensScreen", "Photo capture failed", exception)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = LensColors.InverseOnSurface,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(LensColors.Primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    CameraMode.VIDEO -> {
                        Button(
                            onClick = { viewModel.finishScan() },
                            modifier = Modifier
                                .size(70.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Record", tint = Color.White)
                        }
                    }
                    CameraMode.LIVE_SCAN -> {
                        Button(
                            onClick = { viewModel.finishScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "Finish Scan (${scanSession.size} detected)",
                                    style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                    color = LensColors.OnPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraModeTab(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) LensColors.SurfaceContainerLowest else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, LensColors.Outline) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) LensColors.Primary else LensColors.InverseOnSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                label,
                style = LensTypography.labelSm.copy(fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                color = if (isSelected) LensColors.SlateText else LensColors.InverseOnSurface.copy(alpha = 0.8f)
            )
        }
    }
}
