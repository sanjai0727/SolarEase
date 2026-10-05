package com.example.feature.lens.ui

import java.util.Locale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
                viewModel.processUrisAsync(context, listOf(capturedUri), (uiState as LensUiState.RoomOverview).room)
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
                    is LensUiState.RoomSetup -> "Set Up Rooms"
                    is LensUiState.RoomOverview -> (uiState as LensUiState.RoomOverview).room.name
                    is LensUiState.RoomCompleted -> "Room Certified"
                    is LensUiState.Processing -> "Inference"
                    else -> "SolarEase Lens"
                },
                subTitle = when (uiState) {
                    is LensUiState.Introduction -> "SOLAR LENS"
                    is LensUiState.RoomSetup -> "PRE-AUDIT"
                    is LensUiState.RoomOverview -> "ZONE 0${rooms.indexOfFirst { it.id == (uiState as LensUiState.RoomOverview).room.id } + 1}"
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
                            val uri = createCameraUri("jpg")
                            cameraPhotoLauncher.launch(uri)
                        },
                        onRecordVideoClicked = {
                            val uri = createCameraUri("mp4")
                            cameraVideoLauncher.launch(uri)
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

// ─── Screen 1: Solar Lens Intro (Stitch 13e85ee...) ─────────────────────────────

@Composable
fun IntroductionContent(
    onGoRoomByRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sub-Header Context Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LensColors.PrimaryFixed,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(LensColors.PrimaryLive, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Computer Vision v2.4",
                                style = LensTypography.labelCaps,
                                color = LensColors.OnPrimaryFixed
                            )
                        }
                    }
                    Text(
                        "NEURAL AUDIT ENGINE",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateCaption
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SolarEase Lens",
                    style = LensTypography.headlineLg,
                    color = LensColors.SlateText
                )
                Text(
                    text = "Intelligent Room-by-Room Electrical Load Audit",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateCaption
                )
            }
        }

        // Compact Feature Card: Target Ingestion & Optical Load Mapping
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.InverseSurface,
            border = BorderStroke(1.dp, LensColors.OutlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "TARGET INGESTION",
                        style = LensTypography.labelCaps,
                        color = LensColors.PrimaryFixedDim
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Multi-Zone Optical Load Mapping",
                        style = LensTypography.headlineSm,
                        color = LensColors.InverseOnSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = LensColors.SurfaceContainerLowest.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = LensColors.PrimaryFixedDim,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "CV-CORE",
                            style = LensTypography.labelCaps,
                            color = LensColors.InverseOnSurface
                        )
                    }
                }
            }
        }

        // Operational Overview Description
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Text(
                text = "Solar Lens performs an edge-computed visual inventory across your living spaces to determine peak simultaneous demand, aggregated wattage profiles, and baseline energy consumption for precise solar array and inverter sizing.",
                style = LensTypography.bodyMd,
                color = LensColors.SlateText,
                modifier = Modifier.padding(14.dp),
                lineHeight = 20.sp
            )
        }

        // 4-Step Structured Workflow Matrix
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
                    Text(
                        "AUDIT WORKFLOW MATRIX",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateMuted
                    )
                    Text(
                        "4 SEQUENTIAL STAGES",
                        style = LensTypography.labelCaps,
                        color = LensColors.SlateCaption
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))

                WorkflowStepItem(
                    number = "01",
                    icon = Icons.Default.Domain,
                    title = "Set up your rooms",
                    desc = "Confirm your floor plan sequence (Bedroom, Hall, Kitchen, Bathroom) to structure the load distribution model."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WorkflowStepItem(
                    number = "02",
                    icon = Icons.Default.CenterFocusStrong,
                    title = "Scan or upload each room",
                    desc = "Use live on-device vision or media upload to detect appliances with spatial bounding boxes."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WorkflowStepItem(
                    number = "03",
                    icon = Icons.Default.FactCheck,
                    title = "Review detected appliances",
                    desc = "Verify quantity, calibrated wattage ratings, and assign customized daily duty cycles (runtime hours)."
                )
                Spacer(modifier = Modifier.height(10.dp))
                WorkflowStepItem(
                    number = "04",
                    icon = Icons.Default.WbSunny,
                    title = "Get whole-house energy analysis",
                    desc = "Receive connected load (kW), monthly kWh projection, and exact PV array and battery system recommendations."
                )
            }
        }


        // Primary Execution Action
        Button(
            onClick = onGoRoomByRoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.PrimaryHover),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "Go Room by Room",
                    style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WorkflowStepItem(
    number: String,
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = LensColors.SurfaceContainer,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    number,
                    style = LensTypography.labelCaps,
                    color = LensColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(6.dp),
            color = LensColors.SurfaceContainerLow
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                        color = LensColors.SlateText
                    )
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    desc,
                    style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                    color = LensColors.SlateCaption,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── Screen 2: Room Setup (Stitch 726604d...) ───────────────────────────────────

@Composable
fun RoomSetupContent(
    rooms: List<Room>,
    onAddRoom: () -> Unit,
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Directive Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(LensColors.Primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "PRE-AUDIT ARCHITECTURE",
                        style = LensTypography.labelCaps,
                        color = LensColors.Primary
                    )
                }
                Text(
                    "STEP 01 OF 04",
                    style = LensTypography.labelCaps,
                    color = LensColors.SlateCaption
                )
            }
            Text(
                "Configure Household Rooms",
                style = LensTypography.headlineMd,
                color = LensColors.SlateText
            )
            Text(
                "Establish your room sequence. Scanning will proceed automatically in this order.",
                style = LensTypography.bodyMd,
                color = LensColors.SlateCaption
            )
        }

        // Telemetry Sub-Cell Status
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLow,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LensColors.SurfaceContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ViewTimeline,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "Active Sequence",
                            style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                            color = LensColors.SlateText
                        )
                        Text(
                            "${sortedRooms.size} Zones Initialized",
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                            color = LensColors.SlateCaption
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LensColors.SurfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = LensColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "~14 Mins Total",
                            style = LensTypography.labelCaps,
                            color = LensColors.Primary
                        )
                    }
                }
            }
        }

        // Reorderable Room List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedRooms.forEachIndexed { index, room ->
                val expectedCount = when (room.name.lowercase()) {
                    "bedroom" -> 3
                    "hall", "living room", "hall / living room" -> 4
                    "kitchen" -> 3
                    "bathroom" -> 2
                    else -> 2
                }

                val roomTag = when (room.name.lowercase()) {
                    "bedroom" -> "Primary Bed"
                    "hall", "living room", "hall / living room" -> "Living Area"
                    "kitchen" -> "Utility & Cooking"
                    "bathroom" -> "Wet Zone"
                    else -> "Custom Zone"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DragIndicator,
                            contentDescription = "Reorder",
                            tint = LensColors.SlateCaption.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LensColors.SurfaceContainerLow,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (room.name.lowercase()) {
                                        "bedroom" -> Icons.Default.Bed
                                        "kitchen" -> Icons.Default.Kitchen
                                        "bathroom" -> Icons.Default.Shower
                                        else -> Icons.Default.Chair
                                    },
                                    contentDescription = null,
                                    tint = LensColors.Primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    room.name,
                                    style = LensTypography.headlineSm.copy(fontSize = 15.sp),
                                    color = LensColors.SlateText
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = LensColors.PrimaryContainer.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        roomTag,
                                        style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                        color = LensColors.PrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "$expectedCount expected specs",
                                    style = LensTypography.labelSm.copy(fontSize = 11.sp),
                                    color = LensColors.PrimaryContainer
                                )
                                Text(" • ", color = LensColors.SlateCaption)
                                Text(
                                    "Zone 0${index + 1}",
                                    style = LensTypography.labelSm.copy(fontSize = 11.sp),
                                    color = LensColors.SlateCaption
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.Primary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { onRenameRoom(room) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = LensColors.Primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Edit",
                                        style = LensTypography.labelCaps.copy(fontSize = 10.sp),
                                        color = LensColors.Primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onMoveRoomUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Up",
                                    tint = if (index > 0) LensColors.Primary else LensColors.SlateCaption.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveRoomDown(index) },
                                enabled = index < sortedRooms.size - 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Down",
                                    tint = if (index < sortedRooms.size - 1) LensColors.Primary else LensColors.SlateCaption.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveRoom(room.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = LensColors.Error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LensColors.OutlineVariant)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Another Room", style = LensTypography.labelSm, color = LensColors.SlateText)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sticky Bottom Trigger
        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Start Room-by-Room Audit", style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold), color = LensColors.OnPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Screen 3: Active Room Staging (Stitch 7a2f79b...) ───────────────────────────

@Composable
fun RoomOverviewContent(
    room: Room,
    activeIdx: Int,
    total: Int,
    onRenameRoom: (Room) -> Unit,
    onLiveScanClicked: () -> Unit,
    onUploadPhotosClicked: () -> Unit,
    onUploadVideoClicked: () -> Unit,
    onCapturePhotoClicked: () -> Unit,
    onRecordVideoClicked: () -> Unit,
    onSkipRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Step Tracker & Breadcrumb Flow
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
                        Surface(
                            shape = CircleShape,
                            color = LensColors.Primary,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$activeIdx",
                                    style = LensTypography.labelCaps,
                                    color = LensColors.OnPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Room $activeIdx of $total: ${room.name}",
                            style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                            color = LensColors.SlateText
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = LensColors.Primary.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onRenameRoom(room) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = LensColors.Primary, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Rename", style = LensTypography.labelCaps.copy(fontSize = 10.sp), color = LensColors.Primary)
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LensColors.PrimaryFixed.copy(alpha = 0.4f)
                    ) {
                        Text(
                            "STEP $activeIdx ACTIVE",
                            style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                            color = LensColors.Primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // 4-Segment Linear Step Meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 1..total.coerceAtLeast(4)) {
                        val isFilled = i <= activeIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isFilled) LensColors.Primary else LensColors.SurfaceContainerHighest)
                        )
                    }
                }
            }
        }

        // Technical Calibration Header & Guidance
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
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Audit Zone: ${room.name}",
                            style = LensTypography.headlineMd,
                            color = LensColors.SlateText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Choose an audit workflow: upload media for cloud analysis, or scan the room in real time using the on-device detector.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            lineHeight = 18.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LensColors.SurfaceContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ViewInAr,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "TARGET APPLIANCES IN THIS ROOM",
                    style = LensTypography.labelCaps,
                    color = LensColors.SlateCaption
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetChip("AC", Icons.Default.AcUnit)
                    TargetChip("Fan", Icons.Default.WindPower)
                    TargetChip("Lights", Icons.Default.Lightbulb)
                    TargetChip("TV", Icons.Default.Tv)
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // WORKFLOW 1: UPLOAD (Backend Gemini 3.8 Flash)
        // ══════════════════════════════════════════════════════════════════
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LensColors.SurfaceContainerLow,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Upload Media",
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.PrimaryFixed.copy(alpha = 0.3f)
                            ) {
                                Text(
                                    "GEMINI 3.8 FLASH",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.Primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Multi-modal AI detection with Gemini 3.8 Flash. Select from gallery or capture with camera for upload.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Path Section 1: Gallery
                Text(
                    "PATH 1 & 2: SELECT FROM GALLERY",
                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                    color = LensColors.SlateCaption
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUploadPhotosClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Photos", style = LensTypography.labelSm, color = LensColors.SlateText)
                        }
                    }

                    OutlinedButton(
                        onClick = onUploadVideoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video", style = LensTypography.labelSm, color = LensColors.SlateText)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Path Section 2: Camera Capture for Upload
                Text(
                    "PATH 3: CAPTURE MEDIA FOR UPLOAD",
                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                    color = LensColors.SlateCaption
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCapturePhotoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo", style = LensTypography.labelSm, color = LensColors.SlateText)
                        }
                    }

                    OutlinedButton(
                        onClick = onRecordVideoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Video", style = LensTypography.labelSm, color = LensColors.SlateText)
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // WORKFLOW 2: LIVE SCAN (On-Device Local Detector)
        // ══════════════════════════════════════════════════════════════════
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LensColors.SurfaceContainerHigh,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Live Scan",
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = LensColors.SurfaceContainerHighest
                            ) {
                                Text(
                                    "ON-DEVICE ML KIT",
                                    style = LensTypography.labelCaps.copy(fontSize = 9.sp),
                                    color = LensColors.SlateText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Real-time camera scanning with on-device detector and live bounding boxes. Zero network requests.",
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateCaption,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onLiveScanClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Live Scan (On-Device)", style = LensTypography.labelSm, color = LensColors.OnPrimary)
                    }
                }
            }
        }

        // Secondary Skip Action
        TextButton(
            onClick = onSkipRoom,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Skip this room for now →", style = LensTypography.labelSm, color = LensColors.SlateCaption)
        }
    }
}

@Composable
private fun TargetChip(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LensColors.SurfaceContainerLow,
        border = BorderStroke(1.dp, LensColors.Outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, style = LensTypography.labelCaps.copy(fontSize = 10.sp), color = LensColors.SlateText)
        }
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
                val totalActiveAppliances = room.appliances.sumOf { it.quantity }
                val appliancesActiveLabel = if (totalActiveAppliances == 1) "1 APPLIANCE ACTIVE" else "$totalActiveAppliances APPLIANCES ACTIVE"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LOAD TELEMETRY BREAKDOWN", style = LensTypography.labelCaps, color = LensColors.SlateMuted)
                    Text(appliancesActiveLabel, style = LensTypography.labelCaps, color = LensColors.Primary)
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Connected Load",
                        value = "%.2f".format(Locale.US, room.connectedLoadKw),
                        unit = "kW",
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Est. Monthly Energy",
                        value = "%.1f".format(Locale.US, room.monthlyEnergyKwh),
                        unit = "kWh/month",
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
                        label = "Simplified Baseline Cost",
                        value = "₹%.0f".format(Locale.US, room.simplifiedBaselineCost),
                        unit = "/mo",
                        icon = Icons.Default.CurrencyRupee,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Solar Offset Target",
                        value = if (room.monthlyEnergyKwh > 0.0) "100.0" else "0.0",
                        unit = "%",
                        icon = Icons.Default.Eco,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Indicative estimate at ₹7/kWh. Actual household billing depends on total consumption, applicable tariff slabs, subsidies, fixed charges, and taxes.",
                    style = LensTypography.labelSm.copy(fontSize = 10.sp),
                    color = LensColors.SlateCaption,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("CONFIGURED HARDWARE NODES", style = LensTypography.labelCaps, color = LensColors.SlateCaption)
                Spacer(modifier = Modifier.height(6.dp))
                if (room.appliances.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LensColors.SurfaceContainerLow,
                        border = BorderStroke(1.dp, LensColors.Outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = LensColors.SlateCaption,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "No appliances confirmed",
                                style = LensTypography.headlineSm.copy(fontSize = 14.sp),
                                color = LensColors.SlateText
                            )
                            Text(
                                "No appliances were added to this room.\nYou can add appliances from the review screen.",
                                style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                                color = LensColors.SlateCaption,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
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
                                    val formattedUsage = String.format(Locale.US, "%.1f", app.usageHoursPerDay)
                                    Text("${app.quantity} × ${app.label}", style = LensTypography.bodyMd, color = LensColors.SlateText)
                                    Text("${app.powerWatts.toInt()}W • ${formattedUsage}h/d", style = LensTypography.labelSm, color = LensColors.SlateCaption)
                                }
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
