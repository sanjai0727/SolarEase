package com.example.feature.lens.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.lens.ui.theme.LensColors
import com.example.feature.lens.ui.theme.LensTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensLandingScreen(
    viewModel: LensViewModel,
    onNavigateBack: () -> Unit,
    onOpenCamera: () -> Unit,
    onNavigateToReview: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val selectedPhotos by viewModel.selectedRoomPhotos.collectAsState()

    var roomToRename by remember { mutableStateOf<Room?>(null) }
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var currentCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun createCameraUri(ext: String): android.net.Uri {
        val cameraDir = java.io.File(context.cacheDir, "camera").apply { mkdirs() }
        val tempFile = java.io.File(cameraDir, "lens_upload_${System.currentTimeMillis()}.$ext")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        currentCameraUri = uri
        return uri
    }

    val galleryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(),
        onResult = { uris ->
            if (uris.isNotEmpty() && uiState is LensUiState.RoomOverview) {
                viewModel.processUrisAsync(context, uris, (uiState as LensUiState.RoomOverview).room)
            }
        }
    )

    val galleryVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null && uiState is LensUiState.RoomOverview) {
                viewModel.processUrisAsync(context, listOf(uri), (uiState as LensUiState.RoomOverview).room)
            }
        }
    )

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            val capturedUri = currentCameraUri
            if (success && capturedUri != null && uiState is LensUiState.RoomOverview) {
                val added = viewModel.addSelectedPhoto(capturedUri)
                if (!added && viewModel.selectedRoomPhotos.value.size >= LensViewModel.MAX_ROOM_PHOTOS) {
                    android.widget.Toast.makeText(
                        context,
                        "Maximum of 4 photos reached. Remove a photo to add another.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo(),
        onResult = { success ->
            val capturedUri = currentCameraUri
            if (success && capturedUri != null && uiState is LensUiState.RoomOverview) {
                viewModel.processUrisAsync(context, listOf(capturedUri), (uiState as LensUiState.RoomOverview).room)
            }
        }
    )

    if (roomToRename != null) {
        val target = roomToRename!!
        RenameRoomDialog(
            currentName = target.name,
            title = "Rename Room",
            confirmButtonText = "Save",
            onDismiss = { roomToRename = null },
            onConfirm = { newName ->
                viewModel.renameRoom(target.id, newName)
                roomToRename = null
            }
        )
    }

    if (showAddRoomDialog) {
        RenameRoomDialog(
            currentName = "",
            title = "Add Room",
            confirmButtonText = "Add Room",
            onDismiss = { showAddRoomDialog = false },
            onConfirm = { newName ->
                viewModel.addRoom(newName)
                showAddRoomDialog = false
            }
        )
    }

    BackHandler(enabled = uiState !is LensUiState.Introduction && uiState !is LensUiState.Processing) {
        viewModel.popUiState()
    }

    LaunchedEffect(uiState) {
        if (uiState is LensUiState.CameraActive) {
            onOpenCamera()
        } else if (uiState is LensUiState.Review) {
            onNavigateToReview()
        } else if (uiState is LensUiState.WholeHouseAnalytics) {
            onNavigateToAnalytics()
        }
    }

    if (uiState is LensUiState.Error) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LensColors.Background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = LensColors.Error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Analysis Error",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as LensUiState.Error).message,
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateCaption,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.dismissError() },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Try Again", color = LensColors.OnPrimary)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            LensTopAppBar(
                title = when (uiState) {
                    is LensUiState.Introduction -> "SolarEase Lens"
                    is LensUiState.RoomSetup -> "Configure Rooms"
                    is LensUiState.RoomOverview -> (uiState as LensUiState.RoomOverview).room.name
                    is LensUiState.RoomCompleted -> "Room Certified"
                    is LensUiState.Processing -> "Inference"
                    else -> "SolarEase Lens"
                },
                subTitle = when (uiState) {
                    is LensUiState.Introduction -> "HOME AUDIT"
                    is LensUiState.RoomSetup -> "STEP 01 OF 04"
                    is LensUiState.RoomOverview -> "ROOM ${rooms.indexOfFirst { it.id == (uiState as LensUiState.RoomOverview).room.id } + 1} OF ${rooms.size}"
                    else -> null
                },
                onBackClicked = {
                    if (uiState is LensUiState.Introduction) {
                        onNavigateBack()
                    } else {
                        viewModel.popUiState()
                    }
                }
            )
        },
        containerColor = LensColors.Background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LensColors.Background)
        ) {
            when (uiState) {
                is LensUiState.Introduction -> {
                    IntroductionContent(
                        onGoRoomByRoom = { viewModel.startRoomSetup() }
                    )
                }
                is LensUiState.RoomSetup -> {
                    RoomSetupContent(
                        rooms = rooms,
                        onAddRoom = { showAddRoomDialog = true },
                        onAddPresetRoom = { name -> viewModel.addRoom(name) },
                        onRenameRoom = { room -> roomToRename = room },
                        onRemoveRoom = { viewModel.removeRoom(it) },
                        onMoveRoomUp = { index -> viewModel.reorderRooms(index, index - 1) },
                        onMoveRoomDown = { index -> viewModel.reorderRooms(index, index + 1) },
                        onProceed = { viewModel.proceedToScanning() }
                    )
                }
                is LensUiState.RoomOverview -> {
                    val room = (uiState as LensUiState.RoomOverview).room
                    val sortedRooms = rooms.sortedBy { it.order }
                    val activeIdx = sortedRooms.indexOfFirst { it.id == room.id } + 1
                    val total = sortedRooms.size

                    RoomOverviewContent(
                        room = room,
                        activeIdx = if (activeIdx > 0) activeIdx else 1,
                        total = total,
                        selectedPhotos = selectedPhotos,
                        onRenameRoom = { roomToRename = room },
                        onLiveScanClicked = {
                            viewModel.startCamera(CameraMode.LIVE_SCAN)
                        },
                        onUploadPhotosClicked = {
                            galleryPhotoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onUploadVideoClicked = {
                            galleryVideoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.VideoOnly
                                )
                            )
                        },
                        onCapturePhotoClicked = {
                            if (selectedPhotos.size >= LensViewModel.MAX_ROOM_PHOTOS) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Maximum of 4 photos reached. Remove a photo to add another.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val uri = createCameraUri("jpg")
                                cameraPhotoLauncher.launch(uri)
                            }
                        },
                        onRecordVideoClicked = {
                            val uri = createCameraUri("mp4")
                            cameraVideoLauncher.launch(uri)
                        },
                        onRemovePhoto = { uri ->
                            viewModel.removeSelectedPhoto(uri)
                        },
                        onAnalyzePhotos = { photos ->
                            if (photos.isNotEmpty()) {
                                viewModel.processUrisAsync(context, photos, room)
                            }
                        },
                        onSkipRoom = {
                            viewModel.skipCurrentRoom()
                        }
                    )
                }
                is LensUiState.Processing -> {
                    val proc = uiState as LensUiState.Processing
                    ProcessingContent(
                        room = proc.room,
                        message = proc.message,
                        onCancel = { viewModel.cancelAnalysis() }
                    )
                }
                is LensUiState.RoomCompleted -> {
                    val completed = uiState as LensUiState.RoomCompleted
                    val sortedRooms = rooms.sortedBy { it.order }
                    val activeIdx = sortedRooms.indexOfFirst { it.id == completed.room.id } + 1

                    RoomCompletedContent(
                        room = completed.room,
                        nextRoom = completed.nextRoom,
                        activeIdx = if (activeIdx > 0) activeIdx else 1,
                        total = sortedRooms.size,
                        onContinue = { viewModel.advanceFromRoomCompleted() },
                        onEdit = { viewModel.returnToEditRoom(completed.room.id) }
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun RenameRoomDialog(
    currentName: String,
    title: String = "Rename Room",
    confirmButtonText: String = "Save",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    val isValid = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = LensTypography.headlineSm, color = LensColors.SlateText) },
        text = {
            Column {
                Text(
                    if (currentName.isEmpty()) "Enter the name for the new room:" else "Enter the new name for this room:",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateCaption
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Master Bedroom, Living Room, Kitchen") },
                    isError = !isValid && name.isNotEmpty(),
                    supportingText = if (!isValid && name.isNotEmpty()) {
                        { Text("Room name cannot be empty", color = LensColors.Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid) {
                        onConfirm(name.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(confirmButtonText, color = LensColors.OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LensColors.SlateCaption)
            }
        },
        containerColor = LensColors.SurfaceContainerLowest,
        shape = RoundedCornerShape(12.dp)
    )
}

// ─── Stitch Header Bar ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensTopAppBar(
    title: String,
    subTitle: String?,
    onBackClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LensColors.Background.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = LensColors.SlateText,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = LensTypography.headlineSm,
                        color = LensColors.SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subTitle != null) {
                        Text(
                            text = subTitle.uppercase(),
                            style = LensTypography.labelCaps,
                            color = LensColors.SlateCaption
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(LensColors.SurfaceContainerLow, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = LensColors.SlateCaption,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(LensColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = LensColors.OnPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─── Screen A: Solar Lens Welcome (Stitch Design) ───────────────────────────

@Composable
fun SolarLensWelcomeIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val baseWidth = 280f
        val baseHeight = 210f
        val scale = minOf(size.width / baseWidth, size.height / baseHeight)
        val dx = (size.width - baseWidth * scale) / 2f
        val dy = (size.height - baseHeight * scale) / 2f

        withTransform({
            translate(left = dx, top = dy)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // Soft Ground & Environment
            drawOval(
                color = Color(0xFFEFF4FF),
                topLeft = Offset(20f, 178f),
                size = Size(240f, 24f)
            )

            // Soft Left Bush
            val bush1 = Path().apply {
                moveTo(40f, 188f)
                cubicTo(50f, 176f, 70f, 176f, 80f, 188f)
                close()
            }
            drawPath(bush1, color = Color(0xFF7BD8B1).copy(alpha = 0.4f))

            // Soft Right Bush
            val bush2 = Path().apply {
                moveTo(200f, 188f)
                cubicTo(210f, 172f, 230f, 172f, 240f, 188f)
                close()
            }
            drawPath(bush2, color = Color(0xFF7BD8B1).copy(alpha = 0.5f))

            // Subtle Sun Rays in Background
            drawCircle(color = Color(0xFFE5EEFF), radius = 28f, center = Offset(215f, 55f))
            drawCircle(color = Color(0xFF9FFDD3).copy(alpha = 0.6f), radius = 18f, center = Offset(215f, 55f))

            val sunDashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
            drawLine(
                color = Color(0xFF047857).copy(alpha = 0.4f),
                start = Offset(215f, 30f),
                end = Offset(215f, 22f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = sunDashEffect
            )
            drawLine(
                color = Color(0xFF047857).copy(alpha = 0.4f),
                start = Offset(215f, 88f),
                end = Offset(215f, 80f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = sunDashEffect
            )
            drawLine(
                color = Color(0xFF047857).copy(alpha = 0.4f),
                start = Offset(190f, 55f),
                end = Offset(182f, 55f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = sunDashEffect
            )
            drawLine(
                color = Color(0xFF047857).copy(alpha = 0.4f),
                start = Offset(248f, 55f),
                end = Offset(240f, 55f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
                pathEffect = sunDashEffect
            )

            // Main House Structure
            drawRoundRect(
                color = Color(0xFFF8F9FF),
                topLeft = Offset(75f, 96f),
                size = Size(130f, 92f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // Roof with Overhang
            val roof = Path().apply {
                moveTo(60f, 98f)
                lineTo(140f, 38f)
                lineTo(220f, 98f)
                close()
            }
            drawPath(roof, color = Color(0xFF047857))

            // Solar Panels on Roof
            val panelLeft = Path().apply {
                moveTo(100f, 76f)
                lineTo(136f, 49f)
                lineTo(136f, 88f)
                lineTo(100f, 88f)
                close()
            }
            drawPath(panelLeft, color = Color(0xFF005683).copy(alpha = 0.95f))

            val panelRight = Path().apply {
                moveTo(144f, 49f)
                lineTo(180f, 76f)
                lineTo(180f, 88f)
                lineTo(144f, 88f)
                close()
            }
            drawPath(panelRight, color = Color(0xFF005683).copy(alpha = 0.95f))

            // Solar Grid Lines
            val gridLineColor = Color(0xFF9FFDD3).copy(alpha = 0.7f)
            drawLine(gridLineColor, Offset(118f, 62f), Offset(118f, 88f), strokeWidth = 1f)
            drawLine(gridLineColor, Offset(162f, 62f), Offset(162f, 88f), strokeWidth = 1f)
            drawLine(gridLineColor, Offset(104f, 80f), Offset(134f, 80f), strokeWidth = 1f)
            drawLine(gridLineColor, Offset(146f, 80f), Offset(176f, 80f), strokeWidth = 1f)

            // Front Door
            drawRoundRect(
                color = Color(0xFF047857),
                topLeft = Offset(125f, 142f),
                size = Size(30f, 46f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Door Handle
            drawCircle(Color(0xFF9FFDD3), radius = 2.5f, center = Offset(149f, 165f))

            // Left Room: Cozy Kitchenette (Refrigerator)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(85f, 132f),
                size = Size(28f, 42f),
                cornerRadius = CornerRadius(4f, 4f)
            )
            drawLine(Color(0xFFE5EEFF), Offset(85f, 146f), Offset(113f, 146f), strokeWidth = 1.5f)
            drawRoundRect(Color(0xFF047857), Offset(88f, 137f), Size(2f, 6f), CornerRadius(1f, 1f))
            drawRoundRect(Color(0xFF047857), Offset(88f, 152f), Size(2f, 10f), CornerRadius(1f, 1f))
            // Magnet Note
            drawRoundRect(Color(0xFF9FFDD3), Offset(98f, 136f), Size(6f, 5f), CornerRadius(1f, 1f))

            // Right Room: Warm Living Area (Window & Ceiling Fan)
            drawRoundRect(Color.White, Offset(167f, 122f), Size(26f, 32f), CornerRadius(4f, 4f))
            drawRoundRect(Color(0xFFEFF4FF), Offset(170f, 125f), Size(20f, 26f), CornerRadius(2f, 2f))
            // Ceiling fan
            drawLine(Color(0xFF565E74), Offset(180f, 125f), Offset(180f, 131f), strokeWidth = 1.5f)
            drawOval(Color(0xFF565E74), Offset(172f, 130f), Size(16f, 4f))
            drawCircle(Color(0xFF047857), radius = 2f, center = Offset(180f, 132f))
            // Warm Ambient Lamp Glow
            drawCircle(Color(0xFF9FFDD3).copy(alpha = 0.6f), radius = 5f, center = Offset(180f, 144f))
            drawCircle(Color(0xFF047857), radius = 2f, center = Offset(180f, 144f))

            // Whimsical Clean Energy Sparkle
            val sparkle = Path().apply {
                moveTo(68f, 62f)
                lineTo(70f, 54f)
                lineTo(72f, 62f)
                lineTo(80f, 64f)
                lineTo(72f, 66f)
                lineTo(70f, 74f)
                lineTo(68f, 66f)
                lineTo(60f, 64f)
                close()
            }
            drawPath(sparkle, color = Color(0xFF047857).copy(alpha = 0.35f))
        }
    }
}

@Composable
fun IntroductionContent(
    onGoRoomByRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcoming Card with Delightful Native Vector Illustration
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vector Illustration: House with rooftop solar panels & cozy cutaways
                SolarLensWelcomeIllustration(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Let's understand your home's electricity use",
                    style = LensTypography.headlineLg,
                    color = LensColors.SlateText,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Check which appliances use electricity and explore the right solar setup for your home.",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateCaption,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = LensColors.Primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            "Usually takes ~3 minutes",
                            style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                            color = LensColors.Primary
                        )
                    }
                }
            }
        }

        // Section Title: 3 Simple Steps
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "3 Simple Steps",
                style = LensTypography.headlineSm,
                color = LensColors.SlateText
            )
            Text(
                "QUICK & EASY",
                style = LensTypography.labelCaps,
                color = LensColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 3 Simple Friendly Step Cards
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CitizenStepCard(
                stepNumber = "1",
                icon = Icons.Default.MeetingRoom,
                title = "Set up your rooms",
                description = "Add the rooms you want to check, like living room, kitchen, or bedrooms."
            )
            CitizenStepCard(
                stepNumber = "2",
                icon = Icons.Default.PhotoCamera,
                title = "Add photos & appliances",
                description = "Snap a photo or add appliances quickly from our everyday list."
            )
            CitizenStepCard(
                stepNumber = "3",
                icon = Icons.Default.SolarPower,
                title = "Get your energy report",
                description = "See estimated electricity use and solar sizing recommended just for you."
            )
        }

        // Helpful Neighbor Tip Callout
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = LensColors.PrimaryFixed,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = LensColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "No bill needed right now! You can freely adjust numbers or check back anytime.",
                        style = LensTypography.bodyMd,
                        color = LensColors.SlateText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Bottom CTA Action Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onGoRoomByRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Start Home Audit",
                        style = LensTypography.headlineSm.copy(fontSize = 16.sp),
                        color = LensColors.OnPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LensColors.OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = "Free for homeowners • Takes ~3 mins",
                style = LensTypography.labelSm,
                color = LensColors.SlateCaption
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CitizenStepCard(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = LensColors.SurfaceContainerLowest,
        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = LensColors.SurfaceContainerLow,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        stepNumber,
                        style = LensTypography.headlineSm,
                        color = LensColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                        color = LensColors.SlateText
                    )
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateCaption,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─── Screen B: Room Setup (Stitch Design) ───────────────────────────────────

@Composable
fun RoomSetupContent(
    rooms: List<Room>,
    onAddRoom: () -> Unit,
    onAddPresetRoom: (String) -> Unit = {},
    onRenameRoom: (Room) -> Unit,
    onRemoveRoom: (String) -> Unit,
    onMoveRoomUp: (Int) -> Unit,
    onMoveRoomDown: (Int) -> Unit,
    onProceed: () -> Unit
) {
    val sortedRooms = rooms.sortedBy { it.order }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Friendly Header Banner
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LensColors.SurfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(LensColors.Primary, CircleShape)
                    )
                    Text(
                        "Your rooms · ${sortedRooms.size} added",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Configure your rooms",
                style = LensTypography.headlineLg,
                color = LensColors.SlateText
            )
            Text(
                "We will check your home room by room. You can add, rename, or reorder them anytime.",
                style = LensTypography.bodyMd,
                color = LensColors.SlateCaption,
                lineHeight = 18.sp
            )
        }

        // Helpful Walking Order Tip Callout
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.PrimaryFixed,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = LensColors.Primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "HELPFUL TIP",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Keep rooms in your daily walking order so scanning is fast, natural, and seamless.",
                        style = LensTypography.bodyMd,
                        color = LensColors.SlateText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Citizen-Friendly Room Cards List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sortedRooms.forEachIndexed { index, room ->
                val roomIcon = when (room.name.lowercase()) {
                    "bedroom", "master bedroom", "guest bedroom" -> Icons.Default.Bed
                    "kitchen" -> Icons.Default.Kitchen
                    "bathroom" -> Icons.Default.Shower
                    "hall", "living room", "hall / living room" -> Icons.Default.Chair
                    else -> Icons.Default.MeetingRoom
                }

                val expectedAppliances = when (room.name.lowercase()) {
                    "bedroom" -> "Fan, AC & lighting"
                    "hall", "living room", "hall / living room" -> "Living area & entertainment"
                    "kitchen" -> "Cooking & refrigeration"
                    "bathroom" -> "Water heater zone"
                    else -> "Everyday household appliances"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 44px circular room icon badge
                        Surface(
                            shape = CircleShape,
                            color = LensColors.PrimaryFixed,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = roomIcon,
                                    contentDescription = null,
                                    tint = LensColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Room Info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    room.name,
                                    style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                                    color = LensColors.SlateText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = LensColors.SurfaceContainer
                                ) {
                                    Text(
                                        "Room ${index + 1}",
                                        style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                        color = LensColors.SlateCaption,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                expectedAppliances,
                                style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                color = LensColors.SlateCaption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Room Actions (Edit, Up, Down, Delete)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onRenameRoom(room) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit ${room.name}",
                                    tint = LensColors.SlateCaption,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveRoomUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) LensColors.Primary else LensColors.SlateCaption.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMoveRoomDown(index) },
                                enabled = index < sortedRooms.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move Down",
                                    tint = if (index < sortedRooms.size - 1) LensColors.Primary else LensColors.SlateCaption.copy(alpha = 0.25f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onRemoveRoom(room.id) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete ${room.name}",
                                    tint = LensColors.Error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // "+ Add Another Room" Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddRoom() },
            shape = RoundedCornerShape(12.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = LensColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "+ Add Another Room",
                    style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                    color = LensColors.Primary
                )
            }
        }

        // Quick Add Common Presets
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "QUICK PRESETS",
                style = LensTypography.labelCaps,
                color = LensColors.SlateCaption
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Balcony", "Terrace", "Dining Room", "Home Office").forEach { preset ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LensColors.SurfaceContainerLowest,
                        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAddPresetRoom(preset) }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                preset,
                                style = LensTypography.labelSm.copy(fontSize = 11.sp),
                                color = LensColors.SlateText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // "Take your time" Comfort Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = LensColors.SurfaceContainer
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerHigh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = LensColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Take your time",
                        style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                        color = LensColors.SlateText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "You can pause and resume this audit whenever convenient.",
                        style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                        color = LensColors.SlateCaption
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sticky Bottom Trigger Bar
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(LensColors.Primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Next: ${sortedRooms.firstOrNull()?.name ?: "Living Room"}",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                        color = LensColors.SlateText
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "(Room 1 of ${sortedRooms.size})",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateCaption
                    )
                }
                Text(
                    "READY TO AUDIT",
                    style = LensTypography.labelCaps,
                    color = LensColors.Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Start Room-by-Room Audit",
                        style = LensTypography.headlineSm.copy(fontSize = 16.sp),
                        color = LensColors.OnPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LensColors.OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Screen C: Room Audit (Stitch Minimal Reference) ─────────────────────────

@Composable
fun RoomOverviewContent(
    room: Room,
    activeIdx: Int,
    total: Int,
    selectedPhotos: List<android.net.Uri> = emptyList(),
    onRenameRoom: (Room) -> Unit,
    onLiveScanClicked: () -> Unit,
    onUploadPhotosClicked: () -> Unit,
    onUploadVideoClicked: () -> Unit,
    onCapturePhotoClicked: () -> Unit,
    onRecordVideoClicked: () -> Unit,
    onRemovePhoto: (android.net.Uri) -> Unit = {},
    onAnalyzePhotos: (List<android.net.Uri>) -> Unit = {},
    onSkipRoom: () -> Unit
) {
    var showVideoOptions by remember { mutableStateOf(false) }
    val progressPercent = (activeIdx.toFloat() / total.coerceAtLeast(1).toFloat() * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Progress Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(LensColors.Primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Room $activeIdx of $total · ${room.name}",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onRenameRoom(room) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Rename Room",
                            tint = LensColors.SlateCaption,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    "$progressPercent% Done",
                    style = LensTypography.labelSm,
                    color = LensColors.SlateCaption,
                    fontWeight = FontWeight.Medium
                )
            }

            // Segmented Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..total.coerceAtLeast(1)) {
                    val isCompleted = i <= activeIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) LensColors.Primary else LensColors.SurfaceContainerHigh)
                    )
                }
            }
        }

        // Warm Title Section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Let's check your ${room.name.lowercase()}",
                style = LensTypography.headlineLg,
                color = LensColors.SlateText
            )
            Text(
                "Take pictures of the appliances in the room",
                style = LensTypography.bodyMd,
                color = LensColors.SlateCaption,
                lineHeight = 18.sp
            )
        }

        // Primary Option: Add Room Photos Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f)),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LensColors.SurfaceContainerHigh,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Add room photos",
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.PrimaryFixed
                            ) {
                                Text(
                                    "EASIEST",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Take pictures of the appliances in the room",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            lineHeight = 16.sp
                        )
                    }
                }

                // Selected Photos Section (Thumbnail display, remove, count, and analyze button)
                if (selectedPhotos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${selectedPhotos.size} of 4 photos selected",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = LensColors.SlateText
                            )
                        }

                        if (selectedPhotos.size >= 4) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.PrimaryFixed
                            ) {
                                Text(
                                    "MAX REACHED",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4-Slot Photo Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until 4) {
                            val photoUri = selectedPhotos.getOrNull(i)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LensColors.SurfaceContainer)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (photoUri != null) LensColors.Primary.copy(alpha = 0.5f) else LensColors.Outline.copy(alpha = 0.2f)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoUri != null) {
                                    PhotoThumbnail(
                                        uri = photoUri,
                                        index = i + 1,
                                        onRemove = { onRemovePhoto(photoUri) }
                                    )
                                } else if (i == selectedPhotos.size) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable(onClick = onCapturePhotoClicked),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = "Add another photo",
                                            tint = LensColors.Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "+ Add",
                                            style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                            color = LensColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        "#${i + 1}",
                                        style = LensTypography.labelCaps.copy(fontSize = 11.sp),
                                        color = LensColors.SlateCaption.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Analyze Button when at least 1 photo is selected
                    Button(
                        onClick = { onAnalyzePhotos(selectedPhotos) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Analyze ${selectedPhotos.size} Photo${if (selectedPhotos.size > 1) "s" else ""}",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.Bold),
                                color = LensColors.OnPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = LensColors.OnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dual Action Buttons: Side-by-side [Take Photo] & [Choose Gallery]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isMaxPhotos = selectedPhotos.size >= 4
                    Button(
                        onClick = onCapturePhotoClicked,
                        enabled = !isMaxPhotos,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LensColors.Primary,
                            disabledContainerColor = LensColors.SurfaceContainerHigh
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (!isMaxPhotos) LensColors.OnPrimary else LensColors.SlateCaption,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                when {
                                    selectedPhotos.isEmpty() -> "Take Photo"
                                    isMaxPhotos -> "Max 4 Photos"
                                    else -> "Add Photo (${selectedPhotos.size}/4)"
                                },
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = if (!isMaxPhotos) LensColors.OnPrimary else LensColors.SlateCaption
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { onUploadPhotosClicked() },
                        shape = RoundedCornerShape(12.dp),
                        color = LensColors.SurfaceContainer,
                        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Choose Gallery",
                                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                                color = LensColors.SlateText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Video Action Toggle
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextButton(onClick = { showVideoOptions = !showVideoOptions }) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = LensColors.Tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (showVideoOptions) "Hide video options" else "Or choose a short room video clip",
                            style = LensTypography.labelSm,
                            color = LensColors.Tertiary
                        )
                    }

                    if (showVideoOptions) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onRecordVideoClicked,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Record Video", style = LensTypography.labelSm)
                            }
                            OutlinedButton(
                                onClick = onUploadVideoClicked,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Choose Video", style = LensTypography.labelSm)
                            }
                        }
                    }
                }
            }
        }

        // Option B: Live camera scan Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LensColors.SurfaceContainerLowest,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = LensColors.SlateCaption,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            "Live camera scan",
                            style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                            color = LensColors.SlateText
                        )
                        Text(
                            "Scan around the room in real time",
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                            color = LensColors.SlateCaption
                        )
                    }
                }

                Button(
                    onClick = onLiveScanClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.SurfaceContainerLowest),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        "Start Scan",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                        color = LensColors.SlateText
                    )
                }
            }
        }

        // Helpful Tips Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainer
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "HELPFUL TIPS",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateText,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = LensColors.Primary, fontWeight = FontWeight.Bold)
                        Text(
                            "Try to include main appliances like ceiling fan, AC, or TV.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            lineHeight = 16.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = LensColors.Primary, fontWeight = FontWeight.Bold)
                        Text(
                            "You can snap and add more photos if things are spread out.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            lineHeight = 16.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        Text("• ", color = LensColors.Primary, fontWeight = FontWeight.Bold)
                        Text(
                            "You will review and edit every detected item in the next step.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // Bottom Navigation: Skip Room
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TextButton(onClick = onSkipRoom) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Skip this room for now",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                        color = LensColors.SlateCaption
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = LensColors.SlateCaption,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                "Room $activeIdx of $total • Next: ${if (activeIdx < total) "Next Room" else "Summary"}",
                style = LensTypography.labelCaps,
                color = LensColors.SlateCaption
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Screen 5: Processing (Stitch da97b3c...) ───────────────────────────────────

@Composable
fun ProcessingContent(
    room: Room,
    message: String,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(LensColors.PrimaryLive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "NEURAL HARDWARE INFERENCE",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Analyzing ${room.name} Telemetry",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Radial Dial Telemetry
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = LensColors.Primary,
                        strokeWidth = 6.dp,
                        trackColor = LensColors.SurfaceContainer
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Inference", style = LensTypography.labelCaps.copy(fontSize = 9.sp), color = LensColors.SlateCaption)
                        Text("14ms", style = LensTypography.metricValMd, color = LensColors.SlateText)
                        Text("NPU Core", style = LensTypography.labelCaps.copy(fontSize = 8.sp), color = LensColors.Primary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Execution Sequence Pipeline
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LensColors.SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PipelineStep("Spatial mesh bounding features anchored", isDone = true)
                    PipelineStep(message, isRunning = true)
                    PipelineStep("Duty cycle & wattage envelopes calibration", isQueued = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, LensColors.OutlineVariant)
                ) {
                    Text("Cancel Analysis", style = LensTypography.labelSm, color = LensColors.SlateCaption)
                }
            }
        }
    }
}

@Composable
private fun PipelineStep(
    text: String,
    isDone: Boolean = false,
    isRunning: Boolean = false,
    isQueued: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    when {
                        isDone -> LensColors.PrimaryFixed
                        isRunning -> LensColors.Primary
                        else -> LensColors.SurfaceContainerHighest
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(10.dp))
            } else if (isRunning) {
                CircularProgressIndicator(color = LensColors.OnPrimary, strokeWidth = 1.5.dp, modifier = Modifier.size(10.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text,
            style = LensTypography.labelSm.copy(fontSize = 11.sp),
            color = if (isQueued) LensColors.SlateCaption else LensColors.SlateText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Screen 7: Room Completed Milestone (Stitch cc8bf0d...) ─────────────────────

@Composable
fun RoomCompletedContent(
    room: Room,
    nextRoom: Room?,
    activeIdx: Int,
    total: Int,
    onContinue: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress Rail
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(LensColors.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(12.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Room $activeIdx of $total Completed",
                            style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                            color = LensColors.SlateText
                        )
                    }
                    val pct = ((activeIdx.toFloat() / total) * 100).toInt()
                    Text(
                        "$pct% OF TOTAL AUDIT",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary
                    )
                }
            }
        }

        // Zone Verified Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ZONE SYNCHRONIZED", style = LensTypography.labelCaps, color = LensColors.Primary)
                        Text(room.name, style = LensTypography.headlineMd, color = LensColors.SlateText)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LensColors.PrimaryFixed
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("VERIFIED", style = LensTypography.labelCaps, color = LensColors.OnPrimaryFixed)
                        }
                    }
                }
            }
        }

        // Telemetry Data Readout Card (2x2 Grid)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LOAD TELEMETRY BREAKDOWN", style = LensTypography.labelCaps, color = LensColors.SlateMuted)
                    Text("${room.appliances.size} APPLIANCES ACTIVE", style = LensTypography.labelCaps, color = LensColors.Primary)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Connected Load",
                        value = "%.2f".format(room.connectedLoadKw),
                        unit = "kW",
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Est. Monthly Power",
                        value = "%.1f".format(room.monthlyKwh),
                        unit = "kWh",
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Monthly Cost",
                        value = "₹%.0f".format(room.monthlyCost),
                        unit = "/mo",
                        icon = Icons.Default.CurrencyRupee,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Solar Offset Target",
                        value = "100.0",
                        unit = "%",
                        icon = Icons.Default.Eco,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("CONFIGURED HARDWARE NODES", style = LensTypography.labelCaps, color = LensColors.SlateCaption)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    room.appliances.forEach { app ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LensColors.SurfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${app.quantity}× ${app.label}", style = LensTypography.bodyMd, color = LensColors.SlateText)
                                Text("${app.powerWatts.toInt()}W • ${app.usageHoursPerDay}h/d", style = LensTypography.labelSm, color = LensColors.SlateCaption)
                            }
                        }
                    }
                }
            }
        }

        // Upcoming Phase Card
        if (nextRoom != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = LensColors.SurfaceContainerHighest.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, LensColors.Outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = LensColors.Tertiary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UPCOMING PHASE", style = LensTypography.labelCaps, color = LensColors.Tertiary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Room ${activeIdx + 1} of $total — ${nextRoom.name}", style = LensTypography.headlineSm, color = LensColors.SlateText)
                    Text("Next sequential space for load profile aggregation.", style = LensTypography.bodyMd, color = LensColors.SlateCaption)
                }
            }
        }

        // Bottom CTA Buttons
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (nextRoom != null) "Continue to ${nextRoom.name}" else "View Whole House Analysis",
                    style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                    color = LensColors.OnPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
            }
        }

        TextButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = LensColors.SlateCaption, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Modify ${room.name} appliances", style = LensTypography.labelSm, color = LensColors.SlateCaption)
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = LensColors.SurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = LensColors.SlateCaption, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = LensTypography.labelCaps.copy(fontSize = 9.sp), color = LensColors.SlateCaption)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = LensTypography.metricValMd, color = LensColors.SlateText)
                Spacer(modifier = Modifier.width(2.dp))
                Text(unit, style = LensTypography.labelCaps, color = LensColors.SlateCaption)
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(
    uri: android.net.Uri,
    index: Int,
    onRemove: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                android.graphics.BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "Appliance photo $index",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LensColors.SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = LensColors.SlateCaption,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Index badge (bottom-left)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                "#$index",
                style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // Remove 'X' button (top-right)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove photo $index",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

