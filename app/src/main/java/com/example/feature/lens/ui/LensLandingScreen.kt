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
                    .padding(32.dp)
                    .background(LensColors.SurfaceContainerLowest, RoundedCornerShape(16.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(LensColors.ErrorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = LensColors.Error,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Something went wrong",
                    style = LensTypography.headlineMd,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = (uiState as LensUiState.Error).message,
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.dismissError() },
                    colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try Again", color = LensColors.OnPrimary)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            MinimalTopBar(
                title = when (uiState) {
                    is LensUiState.Introduction -> "SolarEase Lens"
                    is LensUiState.RoomSetup -> "Set Up Rooms"
                    is LensUiState.RoomOverview -> (uiState as LensUiState.RoomOverview).room.name
                    is LensUiState.RoomCompleted -> "Room Done"
                    is LensUiState.Processing -> "Analyzing..."
                    else -> "SolarEase Lens"
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
        title = { Text(title, style = LensTypography.headlineMd, color = LensColors.SlateText) },
        text = {
            Column {
                Text(
                    if (currentName.isEmpty()) "Enter the name for the new room:" else "Enter the new name for this room:",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Room Name") },
                    placeholder = { Text("e.g. Master Bedroom, Living Room") },
                    isError = !isValid && name.isNotEmpty(),
                    supportingText = if (!isValid && name.isNotEmpty()) {
                        { Text("Room name cannot be empty", color = LensColors.Error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(confirmButtonText, color = LensColors.OnPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LensColors.SlateMuted)
            }
        },
        containerColor = LensColors.SurfaceContainerLowest,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─── Minimal Top Bar ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalTopBar(
    title: String,
    onBackClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LensColors.Background,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = LensColors.SlateText,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = LensTypography.headlineMd,
                color = LensColors.SlateText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Screen 1: Landing / Introduction ────────────────────────────────────────

@Composable
fun IntroductionContent(
    onGoRoomByRoom: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero section
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(LensColors.PrimaryFixed, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "SolarEase Lens",
                        style = LensTypography.headlineLg,
                        color = LensColors.SlateText
                    )
                    Text(
                        "Smart Home Energy Scan",
                        style = LensTypography.bodyMd,
                        color = LensColors.SlateMuted
                    )
                }
            }
        }

        // Main introduction card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Understand your home's energy use",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scan your rooms to identify appliances and estimate your energy usage.",
                    style = LensTypography.bodyLg,
                    color = LensColors.SlateMuted,
                    lineHeight = 22.sp
                )
            }
        }

        // How it works — Simple 3-step visual
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "How it works",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(14.dp))

                SimpleStepItem(
                    stepNumber = 1,
                    title = "Set up rooms",
                    desc = "Add or choose the rooms you want to scan."
                )
                Spacer(modifier = Modifier.height(14.dp))
                SimpleStepItem(
                    stepNumber = 2,
                    title = "Scan appliances",
                    desc = "Use your camera, photo, or video to identify items."
                )
                Spacer(modifier = Modifier.height(14.dp))
                SimpleStepItem(
                    stepNumber = 3,
                    title = "Review your energy use",
                    desc = "Check appliance details and see your estimated solar requirement."
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Primary CTA Button
        Button(
            onClick = onGoRoomByRoom,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Start Room Scan",
                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = LensColors.OnPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = LensColors.OnPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SimpleStepItem(
    stepNumber: Int,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(LensColors.PrimaryFixed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$stepNumber",
                style = LensTypography.labelSm.copy(fontWeight = FontWeight.Bold),
                color = LensColors.Primary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = LensTypography.headlineSm,
                color = LensColors.SlateText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                desc,
                style = LensTypography.bodyMd,
                color = LensColors.SlateMuted,
                lineHeight = 18.sp
            )
        }
    }
}

// ─── Screen 2: Room Setup ────────────────────────────────────────────────────

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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Set Up Your Rooms",
                style = LensTypography.headlineLg,
                color = LensColors.SlateText
            )
            Text(
                "Choose the rooms you want to scan.",
                style = LensTypography.bodyMd,
                color = LensColors.SlateMuted
            )
        }

        // Room count indicator
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = LensColors.PrimaryFixed
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = LensColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                val roomLabel = if (sortedRooms.size == 1) "1 room" else "${sortedRooms.size} rooms"
                Text(
                    "$roomLabel added",
                    style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold),
                    color = LensColors.OnPrimaryFixed
                )
            }
        }

        // Room list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sortedRooms.forEachIndexed { index, room ->
                val zoneLabel = "Zone %02d".format(index + 1)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = LensColors.SurfaceContainerLowest,
                    border = BorderStroke(1.dp, LensColors.Outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Room icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(LensColors.SurfaceContainerLow, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (room.name.lowercase()) {
                                    "bedroom" -> Icons.Default.Bed
                                    "kitchen" -> Icons.Default.Kitchen
                                    "bathroom" -> Icons.Default.Shower
                                    else -> Icons.Default.Chair
                                },
                                contentDescription = null,
                                tint = LensColors.Primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))

                        // Room name & Zone label
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                room.name,
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Text(
                                zoneLabel,
                                style = LensTypography.bodyMd.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                                color = LensColors.SlateMuted
                            )
                        }

                        // Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onRenameRoom(room) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = LensColors.SlateMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveRoomUp(index) },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Up",
                                    tint = if (index > 0) LensColors.SlateMuted else LensColors.SlateCaption.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveRoomDown(index) },
                                enabled = index < sortedRooms.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Down",
                                    tint = if (index < sortedRooms.size - 1) LensColors.SlateMuted else LensColors.SlateCaption.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveRoom(room.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = LensColors.Error.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Add room button
            OutlinedButton(
                onClick = onAddRoom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LensColors.OutlineVariant)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("+ Add Room", style = LensTypography.labelSm.copy(fontWeight = FontWeight.Medium), color = LensColors.SlateText)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Proceed CTA button
        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Continue",
                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = LensColors.OnPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ─── Screen 3: Room Overview / Audit ─────────────────────────────────────────

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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress bar and room indicators
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        room.name,
                        style = LensTypography.headlineLg,
                        color = LensColors.SlateText
                    )
                    Text(
                        "Room $activeIdx of $total",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.Medium),
                        color = LensColors.SlateMuted
                    )
                }
                TextButton(
                    onClick = { onRenameRoom(room) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rename", style = LensTypography.labelSm, color = LensColors.Primary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..total.coerceAtLeast(1)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= activeIdx) LensColors.Primary else LensColors.Outline)
                    )
                }
            }
        }

        // Room introduction card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Scan your ${room.name}",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Take a photo, upload a photo/video, or scan the room live to identify appliances.",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateMuted,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Target appliances minimal chips/text
                Text(
                    "Appliances to look for",
                    style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = LensColors.SlateVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val targetAppliancesText = when (room.name.lowercase()) {
                    "kitchen" -> "Refrigerator · Microwave · Induction Cooktop · Mixer Grinder"
                    "bathroom" -> "Water Heater / Geyser · Exhaust Fan · Lights"
                    "living room", "hall" -> "Television · Ceiling Fan · Air Conditioner · Lights"
                    else -> "AC · Fan · Lights · TV"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Text(
                        targetAppliancesText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                        color = LensColors.SlateVariant
                    )
                }
            }
        }

        // Media Options Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Scan options",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Row 1: [ Take Photo ] [ Record Video ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCapturePhotoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo", style = LensTypography.labelSm, color = LensColors.SlateText)
                    }
                    OutlinedButton(
                        onClick = onRecordVideoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Video", style = LensTypography.labelSm, color = LensColors.SlateText)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: [ Upload Photo ] [ Upload Video ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUploadPhotosClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo", style = LensTypography.labelSm, color = LensColors.SlateText)
                    }
                    OutlinedButton(
                        onClick = onUploadVideoClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LensColors.OutlineVariant)
                    ) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = LensColors.Primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Video", style = LensTypography.labelSm, color = LensColors.SlateText)
                    }
                }
            }
        }

        // Live Scan Option (Separately, prominent)
        Button(
            onClick = onLiveScanClicked,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Scan Room Live",
                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = LensColors.OnPrimary
            )
        }

        // Skip action
        TextButton(
            onClick = onSkipRoom,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Skip this room →", style = LensTypography.labelSm, color = LensColors.SlateMuted)
        }
    }
}

// ─── Screen 4: Processing ────────────────────────────────────────────────────

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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Analyzing ${room.name}",
                    style = LensTypography.headlineMd,
                    color = LensColors.SlateText,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Detecting appliances...",
                    style = LensTypography.bodyMd,
                    color = LensColors.SlateMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Simple spinner
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = LensColors.Primary,
                        strokeWidth = 4.dp,
                        trackColor = LensColors.Outline
                    )
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Simple status
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = LensColors.SurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(LensColors.PrimaryLive, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            message,
                            style = LensTypography.bodyMd,
                            color = LensColors.SlateVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TextButton(onClick = onCancel) {
                    Text("Cancel", style = LensTypography.labelSm, color = LensColors.SlateMuted)
                }
            }
        }
    }
}

// ─── Screen 5: Room Completed ────────────────────────────────────────────────

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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.PrimaryFixed
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(LensColors.Primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "${room.name} — Done!",
                        style = LensTypography.headlineMd,
                        color = LensColors.OnPrimaryFixed
                    )
                    Text(
                        "Room $activeIdx of $total completed",
                        style = LensTypography.bodyMd,
                        color = LensColors.OnPrimaryFixed.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Summary metrics
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = LensColors.SurfaceContainerLowest,
            border = BorderStroke(1.dp, LensColors.Outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val totalAppliances = room.appliances.sumOf { it.quantity }
                Text(
                    "Summary",
                    style = LensTypography.headlineSm,
                    color = LensColors.SlateText
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CleanMetricCard(
                        label = "Load",
                        value = "%.2f".format(Locale.US, room.connectedLoadKw),
                        unit = "kW",
                        modifier = Modifier.weight(1f)
                    )
                    CleanMetricCard(
                        label = "Monthly Energy",
                        value = "%.1f".format(Locale.US, room.monthlyEnergyKwh),
                        unit = "kWh",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CleanMetricCard(
                        label = "Est. Cost",
                        value = "₹%.0f".format(Locale.US, room.simplifiedBaselineCost),
                        unit = "/mo",
                        modifier = Modifier.weight(1f)
                    )
                    CleanMetricCard(
                        label = "Appliances",
                        value = "$totalAppliances",
                        unit = "found",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Estimate based on ₹7/kWh. Actual bills depend on your tariff slab, subsidies, and taxes.",
                    style = LensTypography.bodyMd.copy(fontSize = 11.sp),
                    color = LensColors.SlateCaption,
                    lineHeight = 15.sp
                )

                // Appliance list
                if (room.appliances.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Appliances",
                        style = LensTypography.labelSm.copy(fontWeight = FontWeight.Medium),
                        color = LensColors.SlateMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        room.appliances.forEach { app ->
                            val formattedUsage = String.format(Locale.US, "%.1f", app.usageHoursPerDay)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = LensColors.SurfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${app.quantity} × ${app.label}",
                                        style = LensTypography.bodyMd,
                                        color = LensColors.SlateText
                                    )
                                    Text(
                                        "${app.powerWatts.toInt()}W • ${formattedUsage}h/day",
                                        style = LensTypography.labelSm.copy(fontSize = 12.sp),
                                        color = LensColors.SlateMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = LensColors.SurfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = LensColors.SlateCaption,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No appliances found",
                                style = LensTypography.headlineSm,
                                color = LensColors.SlateText
                            )
                            Text(
                                "You can add them manually from the review screen.",
                                style = LensTypography.bodyMd,
                                color = LensColors.SlateMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Next room indicator
        if (nextRoom != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = LensColors.SurfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NavigateNext,
                        contentDescription = null,
                        tint = LensColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "Up next: ${nextRoom.name}",
                            style = LensTypography.headlineSm,
                            color = LensColors.SlateText
                        )
                        Text(
                            "Room ${activeIdx + 1} of $total",
                            style = LensTypography.bodyMd.copy(fontSize = 12.sp),
                            color = LensColors.SlateMuted
                        )
                    }
                }
            }
        }

        // CTA Buttons
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LensColors.Primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                if (nextRoom != null) "Continue to ${nextRoom.name}" else "View Results",
                style = LensTypography.labelSm.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = LensColors.OnPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = LensColors.OnPrimary, modifier = Modifier.size(18.dp))
        }

        TextButton(
            onClick = onEdit,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Default.EditNote, contentDescription = null, tint = LensColors.SlateMuted, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit ${room.name} appliances", style = LensTypography.labelSm, color = LensColors.SlateMuted)
        }
    }
}

@Composable
private fun CleanMetricCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = LensColors.SurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = LensTypography.labelSm.copy(fontSize = 11.sp),
                color = LensColors.SlateMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = LensTypography.metricValMd, color = LensColors.SlateText)
                Spacer(modifier = Modifier.width(3.dp))
                Text(unit, style = LensTypography.labelSm.copy(fontSize = 11.sp), color = LensColors.SlateMuted)
            }
        }
    }
}
