package com.example.feature.home.ui.modules

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.UserSession
import com.example.ui.theme.*
import androidx.compose.ui.text.font.FontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val GeistFontFamily = FontFamily.Default
private val HankenGroteskFontFamily = FontFamily.Default
private val labelCaps = androidx.compose.ui.text.TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.2.sp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricityChatbotRoute(
    onNavigateBack: () -> Unit,
    viewModel: ElectricityChatbotViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Back button handling: if in History view, go back to chat; otherwise exit chatbot
    BackHandler {
        if (uiState.showHistory) {
            viewModel.toggleHistory(false)
        } else {
            onNavigateBack()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (uiState.showHistory) "Chat History" else "SolarEase Assistant",
                            fontFamily = HankenGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = StitchOnSurface
                        )
                        Text(
                            text = if (uiState.showHistory) "ARCHIVED SESSIONS" else "AI Domain Expert",
                            fontFamily = GeistFontFamily,
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (uiState.showHistory) {
                                viewModel.toggleHistory(false)
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StitchOnSurface
                        )
                    }
                },
                actions = {
                    if (!uiState.showHistory) {
                        IconButton(
                            onClick = { viewModel.toggleHistory(true) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = StitchSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.startNewChat()
                                Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = StitchPrimary
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.startNewChat()
                                Toast.makeText(context, "New chat started", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Chat",
                                tint = StitchPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchSurface
                )
            )
        },
        bottomBar = {
            if (!uiState.showHistory) {
                ChatInputBarStitch(
                    inputText = inputText,
                    onInputChanged = { inputText = it },
                    onSend = {
                        val textToSend = inputText.trim()
                        if (textToSend.isNotBlank()) {
                            viewModel.sendMessage(textToSend)
                            inputText = ""
                        }
                    },
                    isTyping = uiState.isTyping,
                    currentLanguage = uiState.currentLanguage
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(StitchSurface)
        ) {
            if (uiState.showHistory) {
                ChatHistoryView(
                    uiState = uiState,
                    onQueryChange = viewModel::setHistorySearchQuery,
                    onCategoryChange = viewModel::setHistoryCategory,
                    onSelectSession = viewModel::loadSession,
                    onNewChat = viewModel::startNewChat
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 1. Real Context Ribbon (Rendered only if real user/profile data is available)
                    RealContextRibbon()

                    // 2. Stitch Mode & Language Selector Drawer
                    ConfigurationBarStitch(
                        currentMode = uiState.currentMode,
                        currentLanguage = uiState.currentLanguage,
                        onModeSelected = viewModel::setMode,
                        onLanguageSelected = viewModel::setLanguage
                    )

                    // 3. Conversation Area
                    if (uiState.messages.isEmpty()) {
                        EmptyWelcomeStateStitch(
                            currentLanguage = uiState.currentLanguage,
                            onSuggestionClicked = { suggestion ->
                                viewModel.sendMessage(suggestion)
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.messages) { message ->
                                if (message.isUser) {
                                    UserMessageBubbleStitch(message = message)
                                } else if (message.isLoading) {
                                    AssistantLoadingCardStitch(currentMode = uiState.currentMode)
                                } else {
                                    AssistantMessageCardStitch(
                                        message = message,
                                        currentMode = uiState.currentMode,
                                        currentLanguage = uiState.currentLanguage,
                                        onCopy = { text ->
                                            clipboardManager.setText(AnnotatedString(text))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            // Error Card if last message failed
                            if (uiState.errorMessage != null && !uiState.isTyping) {
                                item {
                                    ErrorRetryCardStitch(
                                        errorMessage = uiState.errorMessage!!,
                                        onRetry = { viewModel.retryLastQuery() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Context Ribbon
 * Only shown when real SolarEase profile/session data is present. Zero fake telemetry.
 */
@Composable
private fun RealContextRibbon() {
    val currentUser = UserSession.currentUser ?: return
    val contextInfo = buildList {
        currentUser.name?.takeIf { it.isNotBlank() }?.let { add(it) }
        currentUser.ebServiceNumber?.takeIf { it.isNotBlank() }?.let { add("EB #$it") }
        currentUser.houseAddress?.takeIf { it.isNotBlank() }?.let { add(it.take(24)) }
    }.joinToString(" • ")

    if (contextInfo.isBlank()) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = StitchSurfaceContainerLow,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = StitchPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "CONTEXT SYNCED",
                    style = labelCaps,
                    color = StitchOnSurfaceVariant
                )
                Text(
                    text = "•",
                    color = StitchOutlineVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = contextInfo,
                    fontFamily = GeistFontFamily,
                    fontSize = 12.sp,
                    color = StitchOnSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StitchPrimary.copy(alpha = pulseAlpha))
                )
                Text(
                    text = "LIVE",
                    fontFamily = GeistFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary
                )
            }
        }
    }
}

/**
 * Mode and Language Configuration Drawer matching Stitch
 */
@Composable
private fun ConfigurationBarStitch(
    currentMode: String,
    currentLanguage: String,
    onModeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Language Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = StitchSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (currentLanguage == "ta") "மொழி அமைப்புகள்" else "Language",
                        style = labelCaps,
                        color = StitchOnSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StitchSurfaceContainerHigh
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        LanguageTab(
                            title = "English",
                            isSelected = currentLanguage == "en",
                            onClick = { onLanguageSelected("en") }
                        )
                        LanguageTab(
                            title = "தமிழ்",
                            isSelected = currentLanguage == "ta",
                            onClick = { onLanguageSelected("ta") }
                        )
                    }
                }
            }

            // Row 2: Persona Mode Selector Track
            val modes = listOf(
                Triple("citizen", if (currentLanguage == "ta") "குடிமகன் (Citizen)" else "Citizen", Icons.Default.Home),
                Triple("student", if (currentLanguage == "ta") "மாணவர்" else "Student", Icons.Default.School),
                Triple("professional", if (currentLanguage == "ta") "தொழில்முறை" else "Professional", Icons.Default.BusinessCenter),
                Triple("engineering", if (currentLanguage == "ta") "பொறியியல்" else "Engineering", Icons.Default.Terminal)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONA",
                    style = labelCaps,
                    color = StitchSecondary,
                    modifier = Modifier.padding(end = 2.dp)
                )

                modes.forEach { (modeKey, modeTitle, modeIcon) ->
                    val isSelected = currentMode == modeKey
                    PersonaChip(
                        title = modeTitle,
                        icon = modeIcon,
                        isSelected = isSelected,
                        modeKey = modeKey,
                        onClick = { onModeSelected(modeKey) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) StitchPrimary else Color.Transparent,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            fontFamily = GeistFontFamily,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) StitchOnPrimary else StitchOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PersonaChip(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modeKey: String,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected && modeKey == "engineering" -> StitchInverseSurface
        isSelected -> StitchPrimary
        else -> StitchSurfaceContainerLow
    }

    val contentColor = when {
        isSelected && modeKey == "engineering" -> StitchInverseOnSurface
        isSelected -> StitchOnPrimary
        else -> StitchSecondary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        shadowElevation = if (isSelected) 1.dp else 0.dp,
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected && modeKey == "engineering") StitchPrimaryFixed else contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontFamily = GeistFontFamily,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
            if (isSelected && modeKey == "engineering") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(StitchPrimary)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "ACTV",
                        fontFamily = GeistFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnPrimary
                    )
                }
            }
        }
    }
}

/**
 * Empty Welcome State with Frequent Questions (Low clutter, zero fake data)
 */
@Composable
private fun EmptyWelcomeStateStitch(
    currentLanguage: String,
    onSuggestionClicked: (String) -> Unit
) {
    val isTamil = currentLanguage == "ta"

    val welcomeTitle = if (isTamil) "SolarEase AI உதவியாளர்" else "SolarEase AI"
    val welcomeDesc = if (isTamil)
        "மின்சாரம், சோலார் பேனல்கள், மாதாந்திர மின்கட்டணம் மற்றும் PM சூர்யா கர் மானியம் பற்றிய உங்களின் கேள்விகளுக்கு துல்லியமான வழிகாட்டி."
        "மின் நுகர்வு, TNEB மின்கட்டணம், ஸ்லாப் கணக்கீடு மற்றும் மின்சார சேமிப்பு பற்றிய உங்களின் கேள்விகளுக்கு துல்லியமான வழிகாட்டி."
    else
        "Your on-demand solar & energy advisory engine. Ask anything regarding solar sizing, electricity tariffs, government subsidies, and home energy."
        "Your on-demand electricity & tariff advisory engine. Ask anything regarding TNEB billing, slab rates, appliance power draw, and home energy conservation."

    val suggestions = if (isTamil) {
        listOf(
            "வீட்டிற்கு எத்தனை கிலோவாட் சோலார் தேவை?" to "மின் நுகர்வுக்கு ஏற்ப தேவையான சோலார் திறன் கணக்கீடு.",
            "PM சூர்யா கர் திட்டத்தின் கீழ் மானியம் பெறுவது எப்படி?" to "மத்திய அரசின் ₹78,000 வரையிலான மானிய உதவி விபரங்கள்."
            "TNEB மின்கட்டண ஸ்லாப் கணக்கீடு எப்படி?" to "LT-1A கட்டண அமைப்புகள் மற்றும் 100 இலவச யூனிட் விபரங்கள்.",
            "மாதாந்திர மின் நுகர்வை குறைப்பது எப்படி?" to "அதிக மின்சாரம் எடுக்கும் சாதனங்களை கண்டறிந்து சேமிக்கும் வழிகள்."
        )
    } else {
        listOf(
            "How much solar capacity do I need?" to "Calculates optimal PV output based on household load.",
            "What government subsidy can I get under PM Surya Ghar?" to "Central financial assistance up to ₹78,000 for residential systems."
            "How are my bi-monthly bill slabs calculated?" to "Explains TNEB LT-1A slab tiers, fixed charges, and subsidies.",
            "How can I reduce my bi-monthly consumption?" to "Practical tips to audit high-power loads and stay in lower slabs."
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // SolarEase AI Information Card (Restored)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLowest,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StitchSurfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SolarPower,
                                    contentDescription = null,
                                    tint = StitchPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = welcomeTitle,
                                    fontFamily = HankenGroteskFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "Residential & Commercial Advisory",
                                    fontFamily = GeistFontFamily,
                                    fontSize = 11.sp,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StitchPrimaryFixed.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "Ready",
                                fontFamily = GeistFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = StitchPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = welcomeDesc,
                        fontFamily = GeistFontFamily,
                        fontSize = 13.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Suggested Inquiries Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTamil) "பரிந்துரைக்கப்பட்ட கேள்விகள்" else "SUGGESTED INQUIRIES",
                    style = labelCaps,
                    color = StitchOnSurfaceVariant
                )
                Text(
                    text = "SELECT TO ASK",
                    style = labelCaps,
                    color = StitchPrimary
                )
            }
        }

        // Suggested Inquiries Cards
        items(suggestions) { (title, subtitle) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClicked(title) },
                shape = RoundedCornerShape(10.dp),
                color = StitchSurfaceContainerLowest,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            fontFamily = HankenGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = StitchOnSurface
                        )
                        Text(
                            text = subtitle,
                            fontFamily = GeistFontFamily,
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = StitchOutlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * User Message Bubble matching Stitch
 */
@Composable
private fun UserMessageBubbleStitch(message: ChatMessage) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 2.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
            color = StitchPrimaryContainer,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.text,
                fontFamily = GeistFontFamily,
                fontSize = 14.sp,
                color = StitchOnPrimaryContainer,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        Row(
            modifier = Modifier.padding(top = 3.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = timeStr,
                fontFamily = GeistFontFamily,
                fontSize = 10.sp,
                color = StitchSecondary
            )
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = null,
                tint = StitchPrimary,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Assistant Message Card matching Stitch with Persona Header and Actions
 */
@Composable
private fun AssistantMessageCardStitch(
    message: ChatMessage,
    currentMode: String,
    currentLanguage: String,
    onCopy: (String) -> Unit
) {
    val isTamil = currentLanguage == "ta"
    var feedbackSelected by remember { mutableStateOf<Boolean?>(null) }

    val personaTitle = when {
        isTamil -> "SolarEase கள நிபுணர்"
        currentMode == "student" -> "SolarEase • Student Mode"
        currentMode == "engineering" -> "SolarEase • Engineering Mode"
        currentMode == "professional" -> "SolarEase • Professional Mode"
        else -> "SolarEase Citizen Guide"
    }

    val personaTag = when {
        isTamil -> "தமிழ்"
        currentMode == "student" -> "Educational"
        currentMode == "engineering" -> "Engineering"
        currentMode == "professional" -> "Industry Specs"
        else -> "Jargon-Free"
    }

    val personaIcon = when {
        currentMode == "student" -> Icons.Default.School
        currentMode == "engineering" -> Icons.Default.Terminal
        currentMode == "professional" -> Icons.Default.BusinessCenter
        else -> Icons.Default.SolarPower
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = personaIcon,
                            contentDescription = null,
                            tint = StitchOnPrimaryFixed,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = personaTitle,
                        fontFamily = HankenGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = StitchOnSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StitchPrimaryFixed.copy(alpha = 0.35f)
                ) {
                    Text(
                        text = personaTag,
                        fontFamily = GeistFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Message Body Text
            Text(
                text = message.text,
                fontFamily = GeistFontFamily,
                fontSize = 13.sp,
                color = StitchOnSurface,
                lineHeight = 20.sp
            )

            // Bottom Micro-Action Buttons (Copy, Thumbs up, Thumbs down)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onCopy(message.text) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = StitchSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { feedbackSelected = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Helpful",
                            tint = if (feedbackSelected == true) StitchPrimary else StitchSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = { feedbackSelected = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbDown,
                            contentDescription = "Not helpful",
                            tint = if (feedbackSelected == false) StitchError else StitchSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Assistant Thinking / Loading Card matching Stitch Screen 2
 */
@Composable
private fun AssistantLoadingCardStitch(currentMode: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "d1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 150, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "d2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 300, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "d3"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp),
        shape = RoundedCornerShape(topStart = 2.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchPrimary.copy(alpha = dot1)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchPrimary.copy(alpha = dot2)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchPrimary.copy(alpha = dot3)))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SolarEase is preparing your answer...",
                    fontFamily = GeistFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchOnSurface
                )
                Text(
                    text = "Synthesizing response for ${currentMode.replaceFirstChar { it.uppercase() }} Mode",
                    fontFamily = GeistFontFamily,
                    fontSize = 11.sp,
                    color = StitchOnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Error / Retry Card matching Stitch Screen 4
 */
@Composable
private fun ErrorRetryCardStitch(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = StitchErrorContainer,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = StitchError,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Unable to stream response",
                    fontFamily = HankenGroteskFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = StitchError
                )
            }

            Text(
                text = errorMessage,
                fontFamily = GeistFontFamily,
                fontSize = 12.sp,
                color = StitchOnSurface
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimary,
                    contentColor = StitchOnPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Retry Calculation",
                        fontFamily = GeistFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Bottom Chat Input Bar with clean subtle blue accent outline and vertically centered input
 */
@Composable
private fun ChatInputBarStitch(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean,
    currentLanguage: String
) {
    val isTamil = currentLanguage == "ta"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Outlined Input Container with subtle blue accent
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .border(1.5.dp, StitchPrimary, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = StitchSurfaceContainerLow
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp, max = 100.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isTyping) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StitchPrimary)
                            )
                            Text(
                                text = if (isTamil) "உதவியாளர் பதிலளிக்கிறது..." else "Assistant is responding...",
                                fontFamily = GeistFontFamily,
                                fontSize = 13.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = if (isTamil) "ஆற்றல் பற்றி எதையும் கேளுங்கள்" else "Ask anything about energy",
                                    fontFamily = GeistFontFamily,
                                    fontSize = 13.sp,
                                    color = StitchOutlineVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = inputText,
                                onValueChange = onInputChanged,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = GeistFontFamily,
                                    fontSize = 13.sp,
                                    color = StitchOnSurface
                                ),
                                singleLine = true,
                                maxLines = 1,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { onSend() }),
                                cursorBrush = SolidColor(StitchPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Send Button FAB with subtle blue accent
            val canSend = inputText.isNotBlank() && !isTyping
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) StitchPrimary else StitchSurfaceContainerHigh)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.White else StitchSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Chat History Screen matching Stitch Screen 5
 */
@Composable
private fun ChatHistoryView(
    uiState: ChatbotUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSelectSession: (ChatSession) -> Unit,
    onNewChat: () -> Unit
) {
    val filteredSessions = remember(uiState.sessions, uiState.historySearchQuery, uiState.selectedHistoryCategory) {
        uiState.sessions.filter { session ->
            val matchesQuery = uiState.historySearchQuery.isBlank() ||
                session.title.contains(uiState.historySearchQuery, ignoreCase = true) ||
                session.snippet.contains(uiState.historySearchQuery, ignoreCase = true)
            val matchesCategory = uiState.selectedHistoryCategory == "all" ||
                session.category.equals(uiState.selectedHistoryCategory, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }

    val categories = listOf(
        "all" to "All",
        "solar-sizing" to "Solar Sizing",
        "subsidies" to "Subsidies",
        "tariffs" to "Bills & Tariffs",
        "solar-lens" to "Solar Lens"
        "efficiency" to "Energy Savings",
        "solar-lens" to "Appliance Audit"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchSurface)
    ) {
        // Top Controls: Thread Count + New Chat Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = StitchSurfaceContainerLow,
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Conversations",
                            fontFamily = HankenGroteskFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = StitchOnSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StitchSurfaceContainer
                        ) {
                            Text(
                                text = "${uiState.sessions.size} THREADS",
                                style = labelCaps,
                                color = StitchOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onNewChat,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimary,
                            contentColor = StitchOnPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(text = "New Chat", fontFamily = GeistFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Search Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = StitchSurfaceContainerLowest,
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = StitchOutlineVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        TextField(
                            value = uiState.historySearchQuery,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    text = "Search conversations or topics...",
                                    fontFamily = GeistFontFamily,
                                    fontSize = 12.sp,
                                    color = StitchOutlineVariant
                                )
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = GeistFontFamily,
                                fontSize = 12.sp,
                                color = StitchOnSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (uiState.historySearchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = StitchSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSelected = uiState.selectedHistoryCategory == catKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StitchPrimary else StitchSurfaceContainer,
                            modifier = Modifier.clickable { onCategoryChange(catKey) }
                        ) {
                            Text(
                                text = catLabel,
                                fontFamily = GeistFontFamily,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) StitchOnPrimary else StitchOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Session List
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = StitchOutlineVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (uiState.historySearchQuery.isNotBlank()) "No matching conversations found" else "No chat history yet",
                        fontFamily = HankenGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Send a question to start your first archived thread.",
                        fontFamily = GeistFontFamily,
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions) { session ->
                    SessionCard(session = session, onClick = { onSelectSession(session) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ChatSession,
    onClick: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }
    val timeStr = remember(session.timestamp) { timeFormat.format(Date(session.timestamp)) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = session.title,
                        fontFamily = HankenGroteskFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = StitchOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = timeStr,
                    fontFamily = GeistFontFamily,
                    fontSize = 10.sp,
                    color = StitchSecondary
                )
            }

            Text(
                text = session.snippet,
                fontFamily = GeistFontFamily,
                fontSize = 12.sp,
                color = StitchOnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StitchSurfaceContainer
                    ) {
                        Text(
                            text = "${session.mode.replaceFirstChar { it.uppercase() }} Mode • ${if (session.language == "ta") "தமிழ்" else "English"}",
                            fontFamily = GeistFontFamily,
                            fontSize = 10.sp,
                            color = StitchOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = StitchPrimaryFixed.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = session.category.replaceFirstChar { it.uppercase() },
                            fontFamily = GeistFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = StitchOutlineVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
