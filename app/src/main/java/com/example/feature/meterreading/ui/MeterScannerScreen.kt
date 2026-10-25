package com.example.feature.meterreading.ui

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import kotlinx.coroutines.delay

private val BrandGreen = Color(0xFF386A20)
private val ReticleColor = Color(0xFF10B981)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MeterScannerScreen(
    onNavigateBack: () -> Unit,
    onReadingConfirmed: (Double) -> Unit,
    viewModel: MeterScannerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val uiState by viewModel.uiState.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler {
        when (uiState) {
            is MeterScannerUiState.AdjustCrop -> viewModel.cancelCropAdjustment()
            is MeterScannerUiState.Review -> viewModel.retake()
            else -> onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraExecutor.shutdown()
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.w("MeterScannerScreen", "Camera cleanup error", e)
            }
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        // Permission Request State
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(BrandGreen.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Required",
                            tint = BrandGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Camera Permission Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "SolarEase needs camera access to photograph and extract your electricity meter reading on-device.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.textButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enter Reading Manually", color = Color(0xFF64748B))
                    }
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A).copy(alpha = 0.85f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Scan Electricity Meter",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = { viewModel.toggleTorch() }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                        )
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

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
                    previewViewRef = previewView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val rotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                        val preview = Preview.Builder()
                            .setTargetRotation(rotation)
                            .build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                        val capture = ImageCapture.Builder()
                            .setTargetRotation(rotation)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        imageCapture = capture

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()

                        try {
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            viewModel.attachCameraControl(camera.cameraControl)
                        } catch (e: Exception) {
                            Log.e("MeterScannerScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Guided Framing Overlay with Dark Scrim and Viewfinder Reticle
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                val viewWidthPx = with(density) { maxWidth.toPx() }
                val viewHeightPx = with(density) { maxHeight.toPx() }

                val frameWidth = viewWidthPx * 0.84f
                val frameHeight = frameWidth * 0.40f // ~2.5 : 1 aspect ratio for meter LCD/wheel display
                val frameLeft = (viewWidthPx - frameWidth) / 2f
                val frameTop = (viewHeightPx - frameHeight) / 2f - viewHeightPx * 0.05f

                val viewFinderRectNormalized = remember(viewWidthPx, viewHeightPx, frameLeft, frameTop, frameWidth, frameHeight) {
                    if (viewWidthPx > 0f && viewHeightPx > 0f) {
                        android.graphics.RectF(
                            frameLeft / viewWidthPx,
                            frameTop / viewHeightPx,
                            (frameLeft + frameWidth) / viewWidthPx,
                            (frameTop + frameHeight) / viewHeightPx
                        )
                    } else {
                        android.graphics.RectF(0.08f, 0.35f, 0.92f, 0.55f)
                    }
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Semi-transparent darkened background mask
                    drawRect(color = Color.Black.copy(alpha = 0.55f))

                    // Clear rectangular viewport in center
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(frameLeft, frameTop),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )

                    // Reticle Border
                    drawRoundRect(
                        color = ReticleColor.copy(alpha = 0.8f),
                        topLeft = Offset(frameLeft, frameTop),
                        size = Size(frameWidth, frameHeight),
                        cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Corner Accent Marks
                    val cornerLen = 22.dp.toPx()
                    val strokeW = 4.dp.toPx()
                    val accentCol = Color(0xFF34D399)

                    // Top-Left
                    drawLine(accentCol, Offset(frameLeft, frameTop), Offset(frameLeft + cornerLen, frameTop), strokeW)
                    drawLine(accentCol, Offset(frameLeft, frameTop), Offset(frameLeft, frameTop + cornerLen), strokeW)
                    // Top-Right
                    drawLine(accentCol, Offset(frameLeft + frameWidth, frameTop), Offset(frameLeft + frameWidth - cornerLen, frameTop), strokeW)
                    drawLine(accentCol, Offset(frameLeft + frameWidth, frameTop), Offset(frameLeft + frameWidth, frameTop + cornerLen), strokeW)
                    // Bottom-Left
                    drawLine(accentCol, Offset(frameLeft, frameTop + frameHeight), Offset(frameLeft + cornerLen, frameTop + frameHeight), strokeW)
                    drawLine(accentCol, Offset(frameLeft, frameTop + frameHeight), Offset(frameLeft, frameTop + frameHeight - cornerLen), strokeW)
                    // Bottom-Right
                    drawLine(accentCol, Offset(frameLeft + frameWidth, frameTop + frameHeight), Offset(frameLeft + frameWidth - cornerLen, frameTop + frameHeight), strokeW)
                    drawLine(accentCol, Offset(frameLeft + frameWidth, frameTop + frameHeight), Offset(frameLeft + frameWidth, frameTop + frameHeight - cornerLen), strokeW)
                }

                // Guidance Text Above Viewfinder
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = "Position your electricity meter display inside the frame",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Cycling Helpful Tips Banner Below Viewfinder
                MeterTipsBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(top = 220.dp, start = 24.dp, end = 24.dp)
                )

                // Bottom Controls: Shutter Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(3.dp, Color.White, CircleShape)
                            .padding(6.dp)
                            .background(BrandGreen, CircleShape)
                            .clickable(enabled = uiState is MeterScannerUiState.Ready) {
                                imageCapture?.let { capture ->
                                    val actualW = previewViewRef?.width?.toFloat()?.takeIf { it > 0f } ?: viewWidthPx
                                    val actualH = previewViewRef?.height?.toFloat()?.takeIf { it > 0f } ?: viewHeightPx
                                    viewModel.captureAndProcess(
                                        imageCapture = capture,
                                        executor = cameraExecutor,
                                        viewFinderRectNormalized = viewFinderRectNormalized,
                                        viewWidthPx = actualW,
                                        viewHeightPx = actualH
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture Meter Display",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Tap button to capture",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // In-flight Processing Overlay
            AnimatedVisibility(
                visible = uiState is MeterScannerUiState.Capturing || uiState is MeterScannerUiState.Processing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = ReticleColor)
                            Text(
                                text = (uiState as? MeterScannerUiState.Processing)?.message ?: "Processing photo...",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Review Modal Bottom Sheet
            if (uiState is MeterScannerUiState.Review) {
                val reviewState = uiState as MeterScannerUiState.Review
                MeterReviewBottomSheet(
                    croppedBitmap = reviewState.croppedBitmap,
                    candidate = reviewState.candidate,
                    editableText = reviewState.editableText,
                    validationError = reviewState.validationError,
                    plausibilityStatus = reviewState.plausibilityStatus,
                    isSaving = reviewState.isSaving,
                    sheetState = bottomSheetState,
                    isAutoLocalized = reviewState.isAutoLocalized,
                    onAdjustCrop = { viewModel.startCropAdjustment() },
                    onTextChanged = { viewModel.onEditableTextChanged(it) },
                    onConfirm = {
                        viewModel.confirmReading { confirmedValue ->
                            onReadingConfirmed(confirmedValue)
                        }
                    },
                    onRetake = { viewModel.retake() },
                    onManualEntry = onNavigateBack
                )
            }

            // Interactive Crop Adjustment Overlay
            if (uiState is MeterScannerUiState.AdjustCrop) {
                val adjustState = uiState as MeterScannerUiState.AdjustCrop
                MeterCropAdjustOverlay(
                    fullBitmap = adjustState.fullBitmap,
                    initialRectNormalized = adjustState.currentRectNormalized,
                    onApply = { newRect -> viewModel.applyAdjustedCrop(newRect) },
                    onCancel = { viewModel.cancelCropAdjustment() }
                )
            }
        }
    }
}

/**
 * Animated helpful tip ticker for optimal real-world meter capture.
 */
@Composable
private fun MeterTipsBanner(modifier: Modifier = Modifier) {
    val tips = listOf(
        "💡 Avoid reflections: tilt phone slightly if glare obscures numbers.",
        "⚡ For digital meters: capture when cumulative 'kWh' is displayed.",
        "🔄 Push meter button if screen currently displays Voltage (V) or Time.",
        "⚙️ For mechanical meters: ensure all rotating number wheels are in frame."
    )
    var currentTipIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            currentTipIdx = (currentTipIdx + 1) % tips.size
        }
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = tips[currentTipIdx],
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
