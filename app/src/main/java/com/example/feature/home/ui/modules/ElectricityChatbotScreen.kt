package com.example.feature.home.ui.modules

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricityChatbotRoute(
    onNavigateBack: () -> Unit,
    viewModel: ElectricityChatbotViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "SolarEase Assistant", 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "AI Domain Expert", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                inputText = inputText,
                onInputChanged = { inputText = it },
                onSend = {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                },
                isTyping = uiState.isTyping
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Configuration Selectors
            ConfigurationBar(
                currentMode = uiState.currentMode,
                currentLanguage = uiState.currentLanguage,
                onModeSelected = viewModel::setMode,
                onLanguageSelected = viewModel::setLanguage
            )

            if (uiState.errorMessage != null) {
                ErrorMessage(uiState.errorMessage!!)
            }

            if (uiState.messages.isEmpty()) {
                EmptyWelcomeState(
                    language = uiState.currentLanguage,
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
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages) { message ->
                        AnimatedMessageBubble(message)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurationBar(
    currentMode: String,
    currentLanguage: String,
    onModeSelected: (String) -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(bottom = 8.dp)
    ) {
        val modes = listOf("citizen", "student", "professional", "engineering")
        val languages = listOf("en" to "English", "ta" to "தமிழ்")

        // Language Selector
        ScrollableTabRow(
            selectedTabIndex = languages.indexOfFirst { it.first == currentLanguage },
            edgePadding = 16.dp,
            divider = {},
            indicator = {},
            containerColor = Color.Transparent,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            languages.forEach { (code, name) ->
                val selected = currentLanguage == code
                FilterChip(
                    selected = selected,
                    onClick = { onLanguageSelected(code) },
                    label = { Text(name) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // Mode Selector
        ScrollableTabRow(
            selectedTabIndex = modes.indexOf(currentMode),
            edgePadding = 16.dp,
            divider = {},
            indicator = {},
            containerColor = Color.Transparent
        ) {
            modes.forEach { mode ->
                val selected = currentMode == mode
                FilterChip(
                    selected = selected,
                    onClick = { onModeSelected(mode) },
                    label = { Text(mode.replaceFirstChar { it.uppercase() }) },
                    modifier = Modifier.padding(end = 8.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyWelcomeState(language: String, onSuggestionClicked: (String) -> Unit) {
    val isEnglish = language == "en"
    
    val title = if (isEnglish) "Welcome to SolarEase" else "SolarEase-க்கு வரவேற்கிறோம்"
    val subtitle = if (isEnglish) 
        "Ask me anything about electricity, solar panels, EV charging, and energy efficiency." 
    else 
        "மின்சாரம், சோலார் பேனல்கள், EV சார்ஜிங் மற்றும் ஆற்றல் திறன் பற்றி என்னிடம் கேளுங்கள்."
        
    val suggestions = if (isEnglish) {
        listOf("What is solar energy?", "How can I save electricity?", "What is an EV charger?")
    } else {
        listOf("சூரிய ஆற்றல் என்றால் என்ன?", "மின்சாரத்தை எவ்வாறு சேமிப்பது?", "EV சார்ஜர் என்றால் என்ன?")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        suggestions.forEach { suggestion ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSuggestionClicked(suggestion) },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = suggestion,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AnimatedMessageBubble(message: ChatMessage) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
    ) {
        MessageBubble(message)
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val isUser = message.isUser
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (message.isLoading) {
                TypingIndicator(color = textColor)
            } else {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun TypingIndicator(color: Color) {
    var dots by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(400)
            dots = (dots + 1) % 4
        }
    }
    
    Text(
        text = "Typing" + ".".repeat(dots),
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun ErrorMessage(error: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ChatInputBar(
    inputText: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    isTyping: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding() // Respect safe area / keyboard
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
                placeholder = { Text("Ask a question...") },
                maxLines = 4,
                enabled = !isTyping,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            val canSend = inputText.isNotBlank() && !isTyping
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (canSend) 4.dp else 0.dp
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}
