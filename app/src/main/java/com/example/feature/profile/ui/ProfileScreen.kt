package com.example.feature.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.legal.LegalComplianceDialog
import com.example.core.legal.LegalSection
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.ebservice.util.formatConsumerNumber
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.profile.ui.sheets.AboutSolarEaseSheet
import com.example.feature.profile.ui.sheets.DpdpConsentSheet
import com.example.feature.profile.ui.sheets.ElectricityDetailsSheet
import com.example.feature.profile.ui.sheets.HelpSupportSheet
import com.example.feature.profile.ui.sheets.HouseAddressSheet
import com.example.feature.profile.ui.sheets.NotificationPreferencesSheet
import com.example.feature.profile.ui.sheets.PersonalInformationSheet
import com.example.feature.profile.ui.sheets.PrivacySecuritySheet
import com.example.feature.profile.ui.sheets.ThemeSelectorSheet
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.components.SolarEaseNavigationRail
import com.example.ui.components.subtleTopRightGradient
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalIsReducedMotionEnabled
import com.example.ui.theme.LocalThemeController
import com.example.ui.theme.SolarEaseMotion
import com.example.ui.theme.SolarEaseTheme
import com.example.ui.theme.ThemeMode
import com.example.ui.theme.motionTween
import com.example.ui.theme.rememberSolarEaseWindowLayoutInfo
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileRoute(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToHome: () -> Unit = onNavigateBack,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToRecommends: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notifPrefs by viewModel.notificationPreferences.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val biometricLock by viewModel.biometricLock.collectAsStateWithLifecycle()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = SolarEaseTheme.colors
    val layoutInfo = rememberSolarEaseWindowLayoutInfo()

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Delete Account & Erase Data",
                    fontWeight = FontWeight.Bold,
                    color = colors.error,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Under Section 12 of the DPDP Act, 2023, this action permanently deletes your SolarEase account, revokes active sessions, and erases all local encrypted bill records, meter readings, and appliance data. This cannot be undone.",
                    color = colors.onSurfaceVariant,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccountDialog = false
                    viewModel.deleteAccountAndEraseData(context, onSignOut)
                }) {
                    Text(
                        text = "Delete Account",
                        color = colors.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    viewModel.signOut(context, onSignOut)
                }) {
                    Text(
                        text = "Sign Out",
                        color = colors.error,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = colors.surface,
        bottomBar = {
            if (!layoutInfo.useNavigationRail) {
                SolarEaseBottomNavigation(
                    currentTab = SolarEaseNavTab.PROFILE,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToRecommends = onNavigateToRecommends,
                    onNavigateToProfile = { /* Already on Profile */ }
                )
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (layoutInfo.useNavigationRail) {
                SolarEaseNavigationRail(
                    currentTab = SolarEaseNavTab.PROFILE,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToRecommends = onNavigateToRecommends,
                    onNavigateToProfile = { /* Already on Profile */ }
                )
            }

            val user = (uiState as? ProfileUiState.Success)?.user 
                ?: UserSession.currentUser 
                ?: User(id = "", username = "", mobileNumber = "")

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                ProfileContent(
                    user = user,
                    notifPrefs = notifPrefs,
                    selectedLanguage = selectedLanguage,
                    biometricLock = biometricLock,
                    onSaveProfile = { name, username, mobile, address, lat, lng, email ->
                        viewModel.updateProfile(name, username, mobile, address, lat, lng, email)
                    },
                    onSaveElectricity = { consumerNumber, tnebMobile, onResult ->
                        viewModel.updateElectricityDetails(consumerNumber, tnebMobile, onResult)
                    },
                    onSetNotification = { key, value ->
                        viewModel.setNotificationPreference(key, value)
                    },
                    onSetLanguage = { lang ->
                        viewModel.setAppLanguage(lang)
                    },
                    onSetBiometricLock = { enabled ->
                        viewModel.setBiometricLock(enabled)
                    },
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteAccountClick = { showDeleteAccountDialog = true }
                )
            }
        }
    }
}

// ─── Main Profile Content ──────────────────────────────────────────────────────

@Composable
fun ProfileContent(
    user: User,
    notifPrefs: NotificationPreferences,
    selectedLanguage: String,
    biometricLock: Boolean,
    onSaveProfile: (String, String, String, String, Double?, Double?, String?) -> Unit,
    onSaveElectricity: (String, String?, (Boolean, String?) -> Unit) -> Unit,
    onSetNotification: (String, Boolean) -> Unit,
    onSetLanguage: (String) -> Unit,
    onSetBiometricLock: (Boolean) -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit = {}
) {
    val layoutInfo = rememberSolarEaseWindowLayoutInfo()
    val themeController = LocalThemeController.current
    val context = LocalContext.current

    // ── Sheets / Dialogs State ──
    var showPersonalInfoSheet by remember { mutableStateOf(false) }
    var showElectricitySheet by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showDpdpSheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var activeLegalSection by remember { mutableStateOf<LegalSection?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        if (layoutInfo.useTwoColumnGrid) {
            // Adaptive Two-Column Layout on Wide Screens (Tablet / Desktop >= 600dp)
            Row(
                modifier = Modifier
                    .widthIn(max = layoutInfo.maxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column: Profile Header & Energy Details
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileHeaderSection(
                        user = user,
                        onAddressClick = { showAddressSheet = true }
                    )

                    EnergyDetailsSection()
                }

                // Right Column: My Details, Account & App, Sign Out
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MyDetailsSection(
                        user = user,
                        onPersonalInfoClick = { showPersonalInfoSheet = true },
                        onElectricityDetailsClick = { showElectricitySheet = true },
                        onAddressClick = { showAddressSheet = true }
                    )

                    AccountAppSection(
                        currentThemeMode = themeController.currentThemeMode,
                        selectedLanguage = selectedLanguage,
                        onNotificationsClick = { showNotificationsSheet = true },
                        onPrivacyClick = { showPrivacySheet = true },
                        onThemeClick = { showThemeSheet = true },
                        onDpdpClick = { showDpdpSheet = true },
                        onHelpClick = { showHelpSheet = true },
                        onLanguageClick = { showLanguageSheet = true },
                        onAboutClick = { showAboutSheet = true }
                    )

                    SignOutSection(onSignOutClick = onSignOutClick)
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        } else {
            // Phone Portrait / Compact Layout: Sequential Single Column
            Column(
                modifier = Modifier
                    .widthIn(max = layoutInfo.maxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profile Header Section
                StaggeredProfileItem(index = 0) {
                    ProfileHeaderSection(
                        user = user,
                        onAddressClick = { showAddressSheet = true }
                    )
                }

                // 2. Energy Details Section
                StaggeredProfileItem(index = 1) {
                    EnergyDetailsSection()
                }

                // 3. My Details Section
                StaggeredProfileItem(index = 2) {
                    MyDetailsSection(
                        user = user,
                        onPersonalInfoClick = { showPersonalInfoSheet = true },
                        onElectricityDetailsClick = { showElectricitySheet = true },
                        onAddressClick = { showAddressSheet = true }
                    )
                }

                // 4. Account & App Section
                StaggeredProfileItem(index = 3) {
                    AccountAppSection(
                        currentThemeMode = themeController.currentThemeMode,
                        selectedLanguage = selectedLanguage,
                        onNotificationsClick = { showNotificationsSheet = true },
                        onPrivacyClick = { showPrivacySheet = true },
                        onThemeClick = { showThemeSheet = true },
                        onDpdpClick = { showDpdpSheet = true },
                        onHelpClick = { showHelpSheet = true },
                        onLanguageClick = { showLanguageSheet = true },
                        onAboutClick = { showAboutSheet = true }
                    )
                }

                // 5. Sign Out Section
                StaggeredProfileItem(index = 4) {
                    SignOutSection(onSignOutClick = onSignOutClick)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // ── Sheets / Modals ──

    if (showPersonalInfoSheet) {
        PersonalInformationSheet(
            user = user,
            onDismiss = { showPersonalInfoSheet = false },
            onSave = { name, username, email, mobile ->
                onSaveProfile(
                    name,
                    username,
                    mobile ?: (user.mobileNumber ?: ""),
                    user.houseAddress ?: "",
                    user.latitude,
                    user.longitude,
                    email
                )
                showPersonalInfoSheet = false
            }
        )
    }

    if (showElectricitySheet) {
        ElectricityDetailsSheet(
            user = user,
            onDismiss = { showElectricitySheet = false },
            onSave = { consumerNumber, tnebMobile, onResult ->
                onSaveElectricity(consumerNumber, tnebMobile) { success, err ->
                    onResult(success, err)
                }
            }
        )
    }

    if (showAddressSheet) {
        HouseAddressSheet(
            user = user,
            onDismiss = { showAddressSheet = false },
            onSaveAddress = { address, lat, lng ->
                onSaveProfile(
                    user.name ?: "",
                    user.username,
                    user.mobileNumber ?: "",
                    address,
                    lat,
                    lng,
                    user.email
                )
                showAddressSheet = false
            }
        )
    }

    if (showNotificationsSheet) {
        NotificationPreferencesSheet(
            preferences = notifPrefs,
            onToggle = onSetNotification,
            onDismiss = { showNotificationsSheet = false }
        )
    }

    if (showPrivacySheet) {
        PrivacySecuritySheet(
            biometricLock = biometricLock,
            onToggleBiometric = onSetBiometricLock,
            onSignOutClick = {
                showPrivacySheet = false
                onSignOutClick()
            },
            onDeleteAccountClick = {
                showPrivacySheet = false
                onDeleteAccountClick()
            },
            onOpenLegalSection = { section ->
                showPrivacySheet = false
                activeLegalSection = section
            },
            onDismiss = { showPrivacySheet = false }
        )
    }

    if (showThemeSheet) {
        ThemeSelectorSheet(
            currentThemeMode = themeController.currentThemeMode,
            onThemeSelect = { mode ->
                themeController.setThemeMode(mode)
                UserSession.setThemeMode(context, mode)
                showThemeSheet = false
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    if (showDpdpSheet) {
        DpdpConsentSheet(
            onExportData = { /* Handled via data export */ },
            onDeleteAccount = {
                showDpdpSheet = false
                onDeleteAccountClick()
            },
            onOpenLegalSection = { section ->
                showDpdpSheet = false
                activeLegalSection = section
            },
            onDismiss = { showDpdpSheet = false }
        )
    }

    if (showHelpSheet) {
        HelpSupportSheet(onDismiss = { showHelpSheet = false })
    }

    if (showLanguageSheet) {
        AppLanguageSheet(
            currentLanguage = selectedLanguage,
            onSelectLanguage = { lang ->
                onSetLanguage(lang)
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showAboutSheet) {
        AboutSolarEaseSheet(
            onOpenLegalSection = { section ->
                showAboutSheet = false
                activeLegalSection = section
            },
            onDismiss = { showAboutSheet = false }
        )
    }

    if (activeLegalSection != null) {
        LegalComplianceDialog(
            initialSection = activeLegalSection!!,
            onDismiss = { activeLegalSection = null }
        )
    }
}

// ─── 1. PROFILE HEADER SECTION ──────────────────────────────────────────────────

@Composable
private fun ProfileHeaderSection(
    user: User,
    onAddressClick: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
                letterSpacing = 0.sp
            )
            Text(
                text = "Consumer Dashboard & Hardware Tier",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }

        val isDark = LocalIsDarkTheme.current
        val headerGradient = if (isDark) {
            Brush.linearGradient(listOf(Color(0xFF171D15), Color(0xFF121811)))
        } else {
            Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF1F7F1)))
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = colors.surface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .subtleTopRightGradient(
                        shape = RoundedCornerShape(20.dp),
                        radiusMultiplier = 0.8f
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                val displayName = user.name?.trim()?.ifBlank { null } ?: user.username.trim()
                val avatarLetter = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(colors.secondaryContainer)
                            .border(1.5.dp, colors.secondary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarLetter,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.secondary
                        )
                    }

                    if (!user.ebServiceNumber.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colors.primary)
                                .border(1.5.dp, colors.surface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "TNEB Connected",
                                modifier = Modifier.size(11.dp),
                                tint = colors.onPrimary
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.onSurface,
                            letterSpacing = 0.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!user.ebServiceNumber.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(18.dp),
                                tint = colors.primary
                            )
                        }
                    }

                    Text(
                        text = "@${user.username}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )

                    val cityDisplay = extractCity(user.houseAddress)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(onClick = onAddressClick)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (cityDisplay.isNotEmpty()) colors.error else colors.onSurfaceVariant
                        )
                        Text(
                            text = if (cityDisplay.isNotEmpty()) cityDisplay else "Add your address",
                            fontSize = 12.sp,
                            color = if (cityDisplay.isNotEmpty()) colors.onSurfaceVariant else colors.tertiary,
                            fontWeight = if (cityDisplay.isNotEmpty()) FontWeight.Normal else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(50),
                        color = colors.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                            Text(
                                text = "Verified Consumer • TNEB LT-1A",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
            }
        }
    }
}

private fun extractCity(address: String?): String {
    if (address.isNullOrBlank()) return ""
    val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        parts.takeLast(2).joinToString(", ")
    } else {
        address.take(28)
    }
}

// ─── 2. ENERGY DETAILS SECTION ──────────────────────────────────────────────────

@Composable
private fun EnergyDetailsSection() {
    val cachedBill = TnebBillCache.billData.collectAsState().value
    val bills = cachedBill?.bills ?: emptyList()
    val currentBill = bills.firstOrNull()
    val previousBill = if (bills.size >= 2) bills[1] else null

    val currentUnits = currentBill?.units?.trim()?.toIntOrNull()
    val previousUnits = previousBill?.units?.trim()?.toIntOrNull()
    val currentPeriod = currentBill?.period?.trim().orEmpty()
    val previousPeriod = previousBill?.period?.trim().orEmpty()

    val hasCurrentData = currentUnits != null
    val hasDiffData = currentUnits != null && previousUnits != null

    val diff = if (hasDiffData) currentUnits!! - previousUnits!! else null
    val colors = SolarEaseTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Energy Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    letterSpacing = 0.sp
                )
                Text(
                    text = if (currentPeriod.isNotEmpty()) "Billing cycle: $currentPeriod" else "Verified billing cycle consumption",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colors.surfaceVariant
            ) {
                Text(
                    text = "BI-MONTHLY CYCLE",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EnergyMetricCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                icon = Icons.Filled.Bolt,
                iconBgColor = colors.secondaryContainer,
                iconTint = colors.secondary,
                label = "CURRENT USAGE",
                value = if (hasCurrentData) "$currentUnits" else "Unavailable",
                unit = if (hasCurrentData) "kWh" else "",
                subtitle = if (hasCurrentData) {
                    if (currentPeriod.isNotEmpty()) "$currentPeriod cycle" else "Current cycle"
                } else {
                    "Connect bill to track"
                }
            )

            if (!hasDiffData) {
                EnergyMetricCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    icon = Icons.Filled.CompareArrows,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    label = "CYCLE CHANGE",
                    value = "Unavailable",
                    unit = "",
                    subtitle = "Needs 2 billing cycles"
                )
            } else {
                val nonNullDiff = diff!!
                when {
                    nonNullDiff < 0 -> {
                        val absDiff = abs(nonNullDiff)
                        EnergyMetricCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Filled.ArrowDownward,
                            iconBgColor = colors.primaryContainer.copy(alpha = 0.5f),
                            iconTint = colors.primary,
                            label = "CYCLE CHANGE",
                            value = "↓ $absDiff",
                            unit = "kWh",
                            valueTint = colors.primary,
                            unitTint = colors.primary.copy(alpha = 0.8f),
                            subtitle = if (previousPeriod.isNotEmpty()) "vs $previousPeriod" else "vs previous cycle"
                        )
                    }
                    nonNullDiff > 0 -> {
                        EnergyMetricCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Filled.ArrowUpward,
                            iconBgColor = colors.errorContainer.copy(alpha = 0.5f),
                            iconTint = colors.error,
                            label = "CYCLE CHANGE",
                            value = "↑ $nonNullDiff",
                            unit = "kWh",
                            valueTint = colors.error,
                            unitTint = colors.error.copy(alpha = 0.8f),
                            subtitle = if (previousPeriod.isNotEmpty()) "vs $previousPeriod" else "vs previous cycle"
                        )
                    }
                    else -> {
                        EnergyMetricCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Filled.Remove,
                            iconBgColor = colors.surfaceVariant,
                            iconTint = colors.onSurfaceVariant,
                            label = "CYCLE CHANGE",
                            value = "0",
                            unit = "kWh",
                            valueTint = colors.onSurface,
                            unitTint = colors.onSurfaceVariant,
                            subtitle = if (previousPeriod.isNotEmpty()) "Same as $previousPeriod" else "Same as previous"
                        )
                    }
                }
            }
        }

        if (!hasCurrentData || !hasDiffData) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = colors.primary
                    )
                    Text(
                        text = "Consumption data unavailable. Connect your TNEB consumer number in My Details to track verified billing usage.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueTint: Color = SolarEaseTheme.colors.onSurface,
    unitTint: Color = SolarEaseTheme.colors.onSurfaceVariant,
    subtitle: String
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current
    val cardGradient = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF171D15), Color(0xFF121811)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF3F8F2)))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardGradient)
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(20.dp),
                    radiusMultiplier = 0.8f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = iconTint
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = value,
                            fontSize = if (value == "Unavailable") 17.sp else 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = valueTint,
                            letterSpacing = (-0.2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (unit.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = unit,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = unitTint,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── 3. MY DETAILS SECTION ──────────────────────────────────────────────────────

@Composable
private fun MyDetailsSection(
    user: User,
    onPersonalInfoClick: () -> Unit,
    onElectricityDetailsClick: () -> Unit,
    onAddressClick: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = "My Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = "Manage connection and household records",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column {
                val personalSubtitle = buildString {
                    append(user.name ?: user.username)
                    if (!user.email.isNullOrBlank()) append(" • ${user.email}")
                    if (!user.mobileNumber.isNullOrBlank()) append(" • ${user.mobileNumber}")
                }
                DetailRow(
                    icon = Icons.Filled.Person,
                    iconBgColor = colors.secondaryContainer,
                    iconTint = colors.secondary,
                    title = "Personal Information",
                    subtitle = personalSubtitle,
                    onClick = onPersonalInfoClick,
                    showDivider = true
                )

                val ebSubtitle = if (!user.ebServiceNumber.isNullOrBlank()) {
                    val formattedConsumer = formatConsumerNumber(user.ebServiceNumber!!)
                    val maskedMobile = if (!user.tnebRegisteredMobile.isNullOrBlank()) {
                        val mob = user.tnebRegisteredMobile!!.filter { it.isDigit() }
                        if (mob.length >= 10) " • +91 •••${mob.takeLast(4)}" else ""
                    } else ""
                    "Verified Consumer • $formattedConsumer$maskedMobile"
                } else {
                    "Not connected • Tap to add consumer number"
                }
                DetailRow(
                    icon = Icons.Filled.ElectricMeter,
                    iconBgColor = colors.primaryContainer,
                    iconTint = colors.primary,
                    title = "Electricity Details",
                    subtitle = ebSubtitle,
                    onClick = onElectricityDetailsClick,
                    showDivider = true
                )

                val addressSubtitle = user.houseAddress?.ifBlank { null } ?: "Not set • Add your address"
                DetailRow(
                    icon = Icons.Filled.Home,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.onSurfaceVariant,
                    title = "House Address",
                    subtitle = addressSubtitle,
                    onClick = onAddressClick,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val colors = SolarEaseTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = colors.outlineVariant
            )
        }
    }
}

// ─── 4. ACCOUNT & APP SECTION ───────────────────────────────────────────────────

@Composable
private fun AccountAppSection(
    currentThemeMode: ThemeMode,
    selectedLanguage: String,
    onNotificationsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onThemeClick: () -> Unit,
    onDpdpClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    val themeSubtitle = when (currentThemeMode) {
        ThemeMode.SYSTEM -> "System Default"
        ThemeMode.LIGHT -> "Light Mode"
        ThemeMode.DARK -> "Dark Mode"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = "Account & App",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Text(
                text = "System preferences & diagnostic integrity",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = colors.surface,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column {
                AccountRow(
                    icon = Icons.Filled.Notifications,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.primary,
                    title = "Notifications",
                    subtitle = "Bill reminders, usage alerts & weekly insights",
                    onClick = onNotificationsClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Security,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.primary,
                    title = "Privacy & Permissions",
                    subtitle = "Data sovereignty & device permissions",
                    onClick = onPrivacyClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Palette,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.primary,
                    title = "Appearance & Theme",
                    subtitle = "$themeSubtitle • Visual contrast & palette",
                    onClick = onThemeClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Shield,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.primary,
                    title = "DPDP Act & Consent Rights",
                    subtitle = "Section 12 data erasure & portability",
                    onClick = onDpdpClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.ContactSupport,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.secondary,
                    title = "Help & Support",
                    subtitle = "FAQ, meter reading guides, appliance audit support",
                    onClick = onHelpClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Language,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.primary,
                    title = "App Language",
                    subtitle = "$selectedLanguage • English & தமிழ்",
                    onClick = onLanguageClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Info,
                    iconBgColor = colors.surfaceVariant,
                    iconTint = colors.onSurface,
                    title = "About SolarEase",
                    subtitle = "Version 2.4.1 (Build 184) • Clean Energy Hub",
                    onClick = onAboutClick,
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    val colors = SolarEaseTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                }
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconTint
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.onSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = colors.outlineVariant
            )
        }
    }
}

// ─── 5. SIGN OUT & FOOTER ───────────────────────────────────────────────────────

@Composable
private fun SignOutSection(onSignOutClick: () -> Unit) {
    val colors = SolarEaseTheme.colors
    val btnInteraction = remember { MutableInteractionSource() }
    val btnPressed by btnInteraction.collectIsPressedAsState()
    val btnScale by animateFloatAsState(
        targetValue = if (btnPressed) 0.98f else 1f,
        animationSpec = motionTween(durationMillis = 120),
        label = "signOutScale"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSignOutClick,
            interactionSource = btnInteraction,
            modifier = Modifier
                .fillMaxWidth()
                .scale(btnScale)
                .heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, colors.error.copy(alpha = 0.4f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = colors.error
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "SolarEase v2.4.1 • Tamil Nadu Electricity Board (LT-1A)",
            fontSize = 11.5.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppLanguageSheet(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    val languages = listOf("English", "Tamil (தமிழ்)")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "App Language",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Text(
                        text = "Select your preferred application display language",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = colors.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceVariant,
                border = BorderStroke(1.dp, colors.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    languages.forEachIndexed { index, lang ->
                        val isSelected = currentLanguage.contains(lang.take(5), ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLanguage(lang) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.primary else colors.onSurface
                                )
                                Text(
                                    text = if (lang.startsWith("Eng")) "Standard English Interface" else "தமிழ் இடைமுகம் மற்றும் விழிப்புணர்வு",
                                    fontSize = 11.5.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectLanguage(lang) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.onSurfaceVariant
                                )
                            )
                        }
                        if (index < languages.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = colors.outlineVariant)
                        }
                    }
                }
            }

            Text(
                text = "Application labels and Energy Guidelines will adapt to your selected language.",
                fontSize = 11.5.sp,
                color = colors.onSurfaceVariant
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Confirm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StaggeredProfileItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val isReducedMotion = LocalIsReducedMotionEnabled.current
    var visible by remember { mutableStateOf(isReducedMotion) }
    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            visible = true
        }
    }

    if (isReducedMotion) {
        content()
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 280,
                    delayMillis = (index * 55).coerceAtMost(260),
                    easing = SolarEaseMotion.easingDecelerate
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = (index * 55).coerceAtMost(260),
                    easing = SolarEaseMotion.easingDecelerate
                ),
                initialOffsetY = { it / 10 }
            )
        ) {
            content()
        }
    }
}
