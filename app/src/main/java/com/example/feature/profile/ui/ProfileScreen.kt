package com.example.feature.profile.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.User
import com.example.feature.ebservice.util.ConsumerNumberParser
import com.example.feature.ebservice.util.formatConsumerNumber
import com.example.feature.ebservice.util.formatIndianMobile
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import java.util.Locale

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
    val context = LocalContext.current

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out?",
                    color = StitchOnSurfaceVariant,
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
                        color = StitchError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(
                        text = "Cancel",
                        color = StitchOnSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            },
            containerColor = StitchSurfaceContainerLowest,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = StitchSurface,
        bottomBar = {
            SolarEaseBottomNavigation(
                currentTab = SolarEaseNavTab.PROFILE,
                onNavigateToHome = onNavigateToHome,
                onNavigateToRecommends = onNavigateToRecommends,
                onNavigateToProfile = { /* Already on Profile */ }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = StitchPrimaryContainer
                    )
                }
                is ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = StitchError,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = state.message,
                            color = StitchError,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
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
                        onSignOutClick = { showSignOutDialog = true }
                    )
                }
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
    onSignOutClick: () -> Unit
) {
    // ── Sheets / Dialogs State ──
    var showPersonalInfoSheet by remember { mutableStateOf(false) }
    var showElectricitySheet by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }
    var showHelpSheet by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }
    var showRewardsSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── 1. PROFILE HEADER (No Top Edit Button) ─────────────────────
        ProfileHeaderSection(
            user = user,
            onAddressClick = { showAddressSheet = true }
        )

        // ── 2. ENERGY SAVINGS (Compact 2×2 Dashboard Grid) ─────────────
        EnergySavingsSection()

        // ── 3. REWARDS & CREDITS ───────────────────────────────────────
        RewardsCreditsSection(
            onViewRewardsClick = { showRewardsSheet = true },
            onCreditHistoryClick = { showHistorySheet = true }
        )

        // ── 4. MY DETAILS (Functional Rows with Dialogs) ───────────────
        MyDetailsSection(
            user = user,
            onPersonalInfoClick = { showPersonalInfoSheet = true },
            onElectricityDetailsClick = { showElectricitySheet = true },
            onAddressClick = { showAddressSheet = true }
        )

        // ── 5. ACCOUNT & APP (Functional Sub-screens & Language) ────────
        AccountAppSection(
            selectedLanguage = selectedLanguage,
            onNotificationsClick = { showNotificationsSheet = true },
            onPrivacyClick = { showPrivacySheet = true },
            onHelpClick = { showHelpSheet = true },
            onLanguageClick = { showLanguageSheet = true },
            onAboutClick = { showAboutSheet = true }
        )

        // ── 6. SIGN OUT & FOOTER ───────────────────────────────────────
        SignOutSection(onSignOutClick = onSignOutClick)

        Spacer(modifier = Modifier.height(24.dp))
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
            onDismiss = { showPrivacySheet = false }
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
        AboutSolarEaseSheet(onDismiss = { showAboutSheet = false })
    }

    if (showRewardsSheet) {
        ViewRewardsSheet(onDismiss = { showRewardsSheet = false })
    }

    if (showHistorySheet) {
        CreditHistorySheet(onDismiss = { showHistorySheet = false })
    }
}

// ─── 1. PROFILE HEADER SECTION ──────────────────────────────────────────────────

@Composable
private fun ProfileHeaderSection(
    user: User,
    onAddressClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Title row — Global Edit button REMOVED completely per user requirements
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StitchOnSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Consumer Dashboard & Hardware Tier",
                fontSize = 12.sp,
                color = StitchOnSurfaceVariant
            )
        }

        // User profile card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StitchSurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, StitchSurfaceContainer)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circular letter avatar using the FIRST LETTER of the user's name
                val displayName = user.name?.trim()?.ifBlank { null } ?: user.username.trim()
                val avatarLetter = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"

                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(StitchSecondaryContainer)
                            .border(1.5.dp, StitchTertiary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarLetter,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchTertiary
                        )
                    }

                    // TNEB Connected badge (small bolt indicator)
                    if (!user.ebServiceNumber.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .offset(x = 2.dp, y = 2.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(StitchPrimaryContainer)
                                .border(1.5.dp, StitchSurfaceContainerLowest, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "TNEB Connected",
                                modifier = Modifier.size(11.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                // User info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StitchOnSurface,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!user.ebServiceNumber.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                modifier = Modifier.size(18.dp),
                                tint = StitchPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "@${user.username}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = StitchOnSurfaceVariant
                    )

                    // Address / City display
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
                            tint = if (cityDisplay.isNotEmpty()) StitchError else StitchOnSurfaceVariant
                        )
                        Text(
                            text = if (cityDisplay.isNotEmpty()) cityDisplay else "Add your address",
                            fontSize = 12.sp,
                            color = if (cityDisplay.isNotEmpty()) StitchOnSurfaceVariant else StitchTertiary,
                            fontWeight = if (cityDisplay.isNotEmpty()) FontWeight.Normal else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Compact Status Badge — EXACT string: "Verified Consumer • TNEB LT-1A"
                    Surface(
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(50),
                        color = StitchPrimaryFixed.copy(alpha = 0.5f)
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
                                    .background(StitchPrimaryContainer)
                            )
                            Text(
                                text = "Verified Consumer • TNEB LT-1A",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Extract city name from full address for compact display */
private fun extractCity(address: String?): String {
    if (address.isNullOrBlank()) return ""
    val parts = address.split(",").map { it.trim() }.filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        parts.takeLast(2).joinToString(", ")
    } else {
        address.take(28)
    }
}

// ─── 2. ENERGY SAVINGS SECTION (Compact 2×2 Dashboard Grid) ─────────────────────

@Composable
private fun EnergySavingsSection() {
    val cachedBill = TnebBillCache.billData.collectAsState().value
    val unitsConsumed = cachedBill?.latestBill?.units?.toIntOrNull() ?: 30

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Energy Savings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Accumulated across verified billing cycles",
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StitchSurfaceContainer
            ) {
                Text(
                    text = "BI-MONTHLY CYCLE",
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Sleek compact 2×2 grid with reduced padding and height
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnergySavingCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Bolt,
                iconBgColor = StitchSecondaryContainer,
                iconTint = StitchTertiary,
                label = "SAVED",
                value = "$unitsConsumed",
                unit = "kWh",
                subtitle = "Net reduction"
            )
            EnergySavingCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.MilitaryTech,
                iconBgColor = StitchPrimaryFixed.copy(alpha = 0.5f),
                iconTint = StitchPrimaryContainer,
                label = "VERIFIED",
                value = "98",
                unit = "Pts",
                valueTint = StitchPrimaryContainer,
                unitTint = StitchPrimaryContainer.copy(alpha = 0.8f),
                subtitle = "Bill credits"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnergySavingCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.EnergySavingsLeaf,
                iconBgColor = StitchPrimaryFixed.copy(alpha = 0.5f),
                iconTint = StitchPrimaryContainer,
                label = "OFFSET",
                value = "24.6",
                unit = "kg",
                subtitle = "CO₂ reduction"
            )
            EnergySavingCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.BarChart,
                iconBgColor = StitchSurfaceContainerHigh,
                iconTint = StitchTertiaryContainer,
                label = "AUDITED",
                value = "Bill-based",
                unit = "",
                subtitle = "TNEB verified"
            )
        }

        // Info note
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = StitchSurfaceContainerLow,
            border = BorderStroke(1.dp, StitchSurfaceContainer)
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
                    tint = StitchPrimaryContainer
                )
                Text(
                    text = "Energy Credits come from verified bill-to-bill consumption reduction, independent of daily habit check-ins.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = StitchOnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnergySavingCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    valueTint: Color = StitchOnSurface,
    unitTint: Color = StitchOnSurfaceVariant,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLowest,
        shadowElevation = 0.5.dp,
        border = BorderStroke(1.dp, StitchSurfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = iconTint
                    )
                }
                Text(
                    text = label,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = valueTint,
                        letterSpacing = (-0.3).sp
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = unit,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = unitTint
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = StitchOnSurfaceVariant
                )
            }
        }
    }
}

// ─── 3. REWARDS & CREDITS SECTION ───────────────────────────────────────────────

@Composable
private fun RewardsCreditsSection(
    onViewRewardsClick: () -> Unit,
    onCreditHistoryClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rewards & Credits",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                letterSpacing = (-0.3).sp
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = StitchPrimaryFixed.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "TIER 1 ACTIVE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimaryContainer,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Rewards card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StitchSurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, StitchSurfaceContainer)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Credits header
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchPrimaryFixed.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = StitchPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = "Energy Credits: 98",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface
                            )
                            Text(
                                text = "Current Balance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "49%",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = StitchOnSurface,
                        letterSpacing = (-0.3).sp
                    )
                }

                // Progress bar
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(StitchSurfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.49f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(StitchPrimaryContainer)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "0 Credits", fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
                        Text(
                            text = "98 / 200 Target",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer
                        )
                        Text(text = "200 Credits", fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
                    }
                }

                // Next reward info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StitchSurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = StitchPrimaryContainer
                        )
                        Text(
                            text = "102 credits to unlock Next Reward (Tier 1 Eco Voucher).",
                            fontSize = 11.5.sp,
                            color = StitchOnSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewRewardsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimaryContainer,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(text = "View Rewards", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = onCreditHistoryClick,
                        modifier = Modifier.height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StitchOnSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, StitchOutline),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Credit History", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── 4. MY DETAILS SECTION ──────────────────────────────────────────────────────

@Composable
private fun MyDetailsSection(
    user: User,
    onPersonalInfoClick: () -> Unit,
    onElectricityDetailsClick: () -> Unit,
    onAddressClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = "My Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchOnSurface
            )
            Text(
                text = "Manage connection and household records",
                fontSize = 12.sp,
                color = StitchOnSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StitchSurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, StitchSurfaceContainer)
        ) {
            Column {
                // 1. Personal Information
                val personalSubtitle = buildString {
                    append(user.name ?: user.username)
                    if (!user.email.isNullOrBlank()) append(" • ${user.email}")
                    if (!user.mobileNumber.isNullOrBlank()) append(" • ${user.mobileNumber}")
                }
                DetailRow(
                    icon = Icons.Filled.Person,
                    iconBgColor = StitchSecondaryContainer,
                    iconTint = StitchTertiary,
                    title = "Personal Information",
                    subtitle = personalSubtitle,
                    onClick = onPersonalInfoClick,
                    showDivider = true
                )

                // 2. Electricity Details
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
                    iconBgColor = StitchSurfaceContainerHighest,
                    iconTint = StitchPrimaryContainer,
                    title = "Electricity Details",
                    subtitle = ebSubtitle,
                    onClick = onElectricityDetailsClick,
                    showDivider = true
                )

                // 3. House Address
                val addressSubtitle = user.houseAddress?.ifBlank { null } ?: "Not set • Add your address"
                DetailRow(
                    icon = Icons.Filled.Home,
                    iconBgColor = StitchSurfaceContainer,
                    iconTint = StitchSecondary,
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                    color = StitchOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = StitchOnSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = StitchSurfaceContainer
            )
        }
    }
}

// ─── 5. ACCOUNT & APP SECTION ───────────────────────────────────────────────────

@Composable
private fun AccountAppSection(
    selectedLanguage: String,
    onNotificationsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onHelpClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                text = "Account & App",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchOnSurface
            )
            Text(
                text = "System preferences & diagnostic integrity",
                fontSize = 12.sp,
                color = StitchOnSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StitchSurfaceContainerLowest,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, StitchSurfaceContainer)
        ) {
            Column {
                AccountRow(
                    icon = Icons.Filled.Security,
                    iconBgColor = StitchSurfaceContainerHigh,
                    iconTint = StitchPrimaryContainer,
                    title = "Privacy & Permissions",
                    subtitle = "Data sovereignty & device permissions",
                    onClick = onPrivacyClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.ContactSupport,
                    iconBgColor = StitchSurfaceContainerHigh,
                    iconTint = StitchTertiaryContainer,
                    title = "Help & Support",
                    subtitle = "FAQ, meter reading guides, solar lens support",
                    onClick = onHelpClick,
                    showDivider = true
                )
                AccountRow(
                    icon = Icons.Filled.Info,
                    iconBgColor = StitchSurfaceContainerHigh,
                    iconTint = StitchOnSurface,
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
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
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
                    color = StitchOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = StitchOnSurfaceVariant
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 1.dp,
                color = StitchSurfaceContainer
            )
        }
    }
}

// ─── 6. SIGN OUT & FOOTER ───────────────────────────────────────────────────────

@Composable
private fun SignOutSection(onSignOutClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        // Sign Out Button
        Surface(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StitchErrorContainer,
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = StitchError
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchError
                )
            }
        }

        // Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SOLAREASE TECHNOLOGIES © 2026 • POWERING GREENER HOMES",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = StitchOnSurfaceVariant,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "TNEB Smart Grid Certified Client v2.4.1",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = StitchOnSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── MODAL BOTTOM SHEETS / DIALOGS ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInformationSheet(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(user.name ?: "") }
    var username by remember { mutableStateOf(user.username) }
    var email by remember { mutableStateOf(user.email ?: "") }
    var mobileNumber by remember { mutableStateOf(user.mobileNumber ?: "") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val usernameRegex = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,30}$".toRegex()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Personal Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "View and edit your profile credentials",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Full Name") },
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                singleLine = true
            )

            // Username
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (usernameRegex.matches(it)) usernameError = null
                },
                label = { Text("Username") },
                isError = usernameError != null,
                supportingText = {
                    Text(usernameError ?: "3-30 letters, numbers, or underscores")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                singleLine = true
            )

            // Email ID
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email ID") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )

            // Mobile Number
            OutlinedTextField(
                value = mobileNumber,
                onValueChange = { mobileNumber = it },
                label = { Text("Mobile Number") },
                supportingText = {
                    Text("Linked to authentication. Re-verification required to modify active sign-in.")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StitchOutline)
                ) {
                    Text("Cancel", color = StitchOnSurfaceVariant, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = "Name cannot be empty"
                            return@Button
                        }
                        if (!usernameRegex.matches(username)) {
                            usernameError = "Invalid username format"
                            return@Button
                        }
                        onSave(name, username, email.ifBlank { null }, mobileNumber.ifBlank { null })
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimaryContainer,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElectricityDetailsSheet(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String?, (Boolean, String?) -> Unit) -> Unit
) {
    val hasSavedData = !user.ebServiceNumber.isNullOrBlank()
    var isEditing by remember(user.ebServiceNumber) { mutableStateOf(!hasSavedData) }

    val initialConsumer = user.ebServiceNumber?.ifBlank { null }
        ?: TnebBillCache.billData.value?.consumerNumber?.ifBlank { null }
        ?: ""
    val initialMobile = user.tnebRegisteredMobile ?: ""

    var consumerNumberInput by remember(user.ebServiceNumber, isEditing) {
        mutableStateOf(initialConsumer)
    }
    var tnebMobileInput by remember(user.tnebRegisteredMobile, isEditing) {
        mutableStateOf(initialMobile)
    }

    var consumerError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var parsedRegionName by remember { mutableStateOf<String?>(null) }

    // Live validation for consumer number
    LaunchedEffect(consumerNumberInput) {
        val clean = consumerNumberInput.filter { it.isDigit() }
        if (clean.length in 10..12) {
            val res = ConsumerNumberParser.parse(clean)
            if (res.isSuccess) {
                parsedRegionName = res.getOrNull()?.regionName
                consumerError = null
            } else {
                parsedRegionName = null
                consumerError = res.exceptionOrNull()?.message
            }
        } else {
            parsedRegionName = null
            if (clean.isNotEmpty() && clean.length < 10) {
                consumerError = "${clean.length}/10-12 digits entered"
            } else {
                consumerError = null
            }
        }
    }

    // Live validation for TNEB mobile
    LaunchedEffect(tnebMobileInput) {
        val clean = tnebMobileInput.filter { it.isDigit() }
        if (clean.isNotEmpty() && clean.length == 10 && clean[0] !in '6'..'9') {
            mobileError = "Indian mobile number must start with 6, 7, 8, or 9"
        } else {
            mobileError = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEditing && hasSavedData) "Edit Electricity Details" else "Electricity Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "TANGEDCO / TNEB Consumer Connection",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            // Connection Details Card (Display Only Info)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Electricity Board", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                        Text(
                            "TANGEDCO (Tamil Nadu)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tariff Category", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                        Text(
                            "LT-1A Domestic",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchPrimaryContainer
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Connection Status", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                        Text(
                            "Verified Consumer • TNEB LT-1A",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer
                        )
                    }
                    val currentRegionName = parsedRegionName
                        ?: user.ebServiceNumber?.let { ConsumerNumberParser.parse(it).getOrNull()?.regionName }
                    if (currentRegionName != null) {
                        HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Region Name", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                            Text(
                                currentRegionName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchTertiary
                            )
                        }
                    }
                }
            }

            if (!isEditing && hasSavedData) {
                // ── 2. SAVED STATE UI ──
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceContainerLow,
                    border = BorderStroke(1.dp, StitchSurfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Consumer Number Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Consumer Number",
                                    fontSize = 12.sp,
                                    color = StitchOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = formatConsumerNumber(user.ebServiceNumber ?: ""),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                            }
                            // Clear green check/tick indicating verified/saved in the app
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Verified Consumer Number",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)

                        // TNEB Registered Mobile Number Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TNEB Registered Mobile Number",
                                    fontSize = 12.sp,
                                    color = StitchOnSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val mobileText = if (!user.tnebRegisteredMobile.isNullOrBlank()) {
                                    formatIndianMobile(user.tnebRegisteredMobile!!)
                                } else {
                                    "Not registered • Tap Edit to add"
                                }
                                Text(
                                    text = mobileText,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (!user.tnebRegisteredMobile.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (!user.tnebRegisteredMobile.isNullOrBlank()) StitchOnSurface else StitchOnSurfaceVariant
                                )
                            }
                            if (!user.tnebRegisteredMobile.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Verified TNEB Mobile Number",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Edit Button
                Button(
                    onClick = {
                        consumerNumberInput = user.ebServiceNumber ?: ""
                        tnebMobileInput = user.tnebRegisteredMobile ?: ""
                        consumerError = null
                        mobileError = null
                        generalError = null
                        isEditing = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimaryContainer,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // ── 4. ELECTRICITY DETAILS FORM (EDIT / ENTRY MODE) ──
                // Field 1: Consumer Number
                OutlinedTextField(
                    value = consumerNumberInput,
                    onValueChange = { input ->
                        consumerNumberInput = input.filter { it.isDigit() }.take(12)
                    },
                    label = { Text("Consumer Number") },
                    supportingText = {
                        if (consumerError != null) {
                            Text(consumerError ?: "", color = StitchError)
                        } else if (parsedRegionName != null) {
                            Text("Region: $parsedRegionName", color = StitchPrimaryContainer)
                        } else {
                            Text("10 to 12 digits (e.g. 09-440-233-135)")
                        }
                    },
                    isError = consumerError != null && consumerNumberInput.length in 10..12,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Field 2: TNEB Registered Mobile Number (explicitly separated from authentication mobile)
                OutlinedTextField(
                    value = tnebMobileInput,
                    onValueChange = { input ->
                        tnebMobileInput = input.filter { it.isDigit() }.take(10)
                    },
                    label = { Text("TNEB Registered Mobile Number") },
                    supportingText = {
                        if (mobileError != null) {
                            Text(mobileError ?: "", color = StitchError)
                        } else {
                            Text("Mobile registered with TNEB. Independent of SolarEase login.")
                        }
                    },
                    isError = mobileError != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                if (generalError != null) {
                    Text(
                        text = generalError ?: "",
                        color = StitchError,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Form Actions: Cancel & Save
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (hasSavedData) {
                                isEditing = false
                                generalError = null
                                consumerError = null
                                mobileError = null
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StitchOutline)
                    ) {
                        Text("Cancel", color = StitchOnSurfaceVariant, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val cleanConsumer = consumerNumberInput.filter { it.isDigit() }
                            if (cleanConsumer.length !in 10..12) {
                                consumerError = "Consumer number must be 10 to 12 digits."
                                return@Button
                            }
                            val parseResult = ConsumerNumberParser.parse(cleanConsumer)
                            if (parseResult.isFailure) {
                                consumerError = parseResult.exceptionOrNull()?.message ?: "Invalid Consumer Number."
                                return@Button
                            }

                            val cleanMobile = tnebMobileInput.filter { it.isDigit() }.let { raw ->
                                when {
                                    raw.length == 10 -> raw
                                    raw.length == 11 && raw.startsWith("0") -> raw.substring(1)
                                    raw.length == 12 && raw.startsWith("91") -> raw.substring(2)
                                    else -> raw
                                }
                            }
                            if (cleanMobile.isNotEmpty()) {
                                if (cleanMobile.length != 10 || cleanMobile[0] !in '6'..'9') {
                                    mobileError = "Enter a valid 10-digit Indian mobile number."
                                    return@Button
                                }
                            }

                            generalError = null
                            onSave(cleanConsumer, cleanMobile.ifBlank { null }) { success, err ->
                                if (success) {
                                    isEditing = false
                                    generalError = null
                                    consumerError = null
                                    mobileError = null
                                } else {
                                    generalError = err ?: "Validation failed"
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimaryContainer,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseAddressSheet(
    user: User,
    onDismiss: () -> Unit,
    onSaveAddress: (String, Double?, Double?) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var streetAddress by remember { mutableStateOf(user.houseAddress ?: "") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("Tamil Nadu") }
    var pincode by remember { mutableStateOf("") }

    var detectedAddress by remember { mutableStateOf<String?>(null) }
    var detectedLat by remember { mutableStateOf<Double?>(null) }
    var detectedLng by remember { mutableStateOf<Double?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }

    fun fetchCurrentLocation() {
        isLocating = true
        locationError = null
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                isLocating = false
                if (loc != null) {
                    detectedLat = loc.latitude
                    detectedLng = loc.longitude
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val fullAddr = listOfNotNull(
                                addr.subThoroughfare,
                                addr.thoroughfare,
                                addr.locality,
                                addr.adminArea,
                                addr.postalCode
                            ).filter { it.isNotBlank() }.joinToString(", ")
                            detectedAddress = fullAddr
                            city = addr.locality ?: ""
                            pincode = addr.postalCode ?: ""
                            state = addr.adminArea ?: "Tamil Nadu"
                            streetAddress = listOfNotNull(addr.subThoroughfare, addr.thoroughfare).joinToString(" ")
                        } else {
                            detectedAddress = "Coordinates: ${loc.latitude}, ${loc.longitude}"
                        }
                    } catch (e: Exception) {
                        detectedAddress = "Coordinates: ${loc.latitude}, ${loc.longitude}"
                    }
                } else {
                    locationError = "Unable to get current GPS location. Please enter manually."
                }
            }.addOnFailureListener {
                isLocating = false
                locationError = it.message ?: "Location request failed"
            }
        } catch (e: SecurityException) {
            isLocating = false
            locationError = "Location permission denied."
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchCurrentLocation()
        } else {
            locationError = "Location permission is required to detect coordinates."
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "House Address",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Residential address for solar irradiance and grid matching",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            // OPTION A: USE CURRENT LOCATION
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSecondaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, StitchTertiary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = null,
                            tint = StitchTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "OPTION A — USE CURRENT LOCATION",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchTertiary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "Fetch GPS coordinates automatically and reverse-geocode to your home address.",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted) {
                                fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchTertiary,
                            contentColor = Color.White
                        ),
                        enabled = !isLocating
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting location...", fontSize = 13.sp)
                        } else {
                            Icon(Icons.Filled.LocationSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use Current Location", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (locationError != null) {
                        Text(
                            text = locationError ?: "",
                            fontSize = 11.sp,
                            color = StitchError
                        )
                    }

                    // Detected location confirmation card
                    if (detectedAddress != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = StitchSurfaceContainerLowest,
                            border = BorderStroke(1.dp, StitchPrimaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = StitchPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Detected GPS Address",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer
                                    )
                                }
                                Text(
                                    text = detectedAddress ?: "",
                                    fontSize = 12.sp,
                                    color = StitchOnSurface
                                )
                                Button(
                                    onClick = {
                                        onSaveAddress(detectedAddress ?: "", detectedLat, detectedLng)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StitchPrimaryContainer,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Confirm & Save Detected Location", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = StitchSurfaceContainer)
                Text(
                    text = "  OR ENTER MANUALLY  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = StitchSurfaceContainer)
            }

            // OPTION B: ENTER MANUALLY
            OutlinedTextField(
                value = streetAddress,
                onValueChange = { streetAddress = it },
                label = { Text("House / Street Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Pincode") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text("State") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = profileTextFieldColors(),
                singleLine = true
            )

            // Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StitchOutline)
                ) {
                    Text("Cancel", color = StitchOnSurfaceVariant, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val compiled = listOfNotNull(
                            streetAddress.ifBlank { null },
                            city.ifBlank { null },
                            state.ifBlank { null },
                            pincode.ifBlank { null }
                        ).joinToString(", ")
                        if (compiled.isNotBlank()) {
                            onSaveAddress(compiled, detectedLat, detectedLng)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimaryContainer,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save Address", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPreferencesSheet(
    preferences: NotificationPreferences,
    onToggle: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Manage your telemetry alerts and billing notifications",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    PreferenceToggleRow(
                        title = "Bill Reminders",
                        description = "Alerts for upcoming TNEB bi-monthly bill due dates and cycles",
                        checked = preferences.billReminders,
                        onCheckedChange = { onToggle("bill_reminders", it) },
                        showDivider = true
                    )
                    PreferenceToggleRow(
                        title = "Meter Reading Reminders",
                        description = "Bi-monthly reminders to capture meter readings for accurate telemetry",
                        checked = preferences.meterReminders,
                        onCheckedChange = { onToggle("meter_reminders", it) },
                        showDivider = true
                    )
                    PreferenceToggleRow(
                        title = "Energy-Saving Mantras",
                        description = "Daily and weekly habit check-in prompts for the 9 Mantras",
                        checked = preferences.mantraReminders,
                        onCheckedChange = { onToggle("mantra_reminders", it) },
                        showDivider = true
                    )
                    PreferenceToggleRow(
                        title = "Reward & Credit Alerts",
                        description = "Milestone notifications when unlocking new eco-vouchers",
                        checked = preferences.rewardReminders,
                        onCheckedChange = { onToggle("reward_reminders", it) },
                        showDivider = true
                    )
                    PreferenceToggleRow(
                        title = "Important SolarEase Alerts",
                        description = "Critical grid advisories, tariff updates and system notices",
                        checked = preferences.alertReminders,
                        onCheckedChange = { onToggle("alert_reminders", it) },
                        showDivider = false
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimaryContainer,
                    contentColor = Color.White
                )
            ) {
                Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurface
                )
                Text(
                    text = description,
                    fontSize = 11.5.sp,
                    color = StitchOnSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = StitchPrimaryContainer,
                    uncheckedThumbColor = StitchOnSurfaceVariant,
                    uncheckedTrackColor = StitchSurfaceContainerHigh
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = StitchSurfaceContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacySecuritySheet(
    biometricLock: Boolean,
    onToggleBiometric: (Boolean) -> Unit,
    onSignOutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Privacy & Security",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Device protection and permission integrity",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Permission Status Rows
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Location Permission", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                            Text(
                                if (hasLocationPermission) "Granted • Coordinates enabled" else "Not Granted • GPS disabled",
                                fontSize = 11.5.sp,
                                color = if (hasLocationPermission) StitchPrimaryContainer else StitchError
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, StitchOutline)
                        ) {
                            Text("Settings", fontSize = 11.sp, color = StitchOnSurface)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = StitchSurfaceContainer)

                    // Data sovereignty info
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Local Data Sovereignty & Encryption",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All your electricity metrics, meter OCR logs, and solar estimates are protected with TLS 1.3 encryption. Your consumer connection data is never sold or shared.",
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }
            }

            // Sign out button
            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StitchError.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchError)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sign Out of Session", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpSupportSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    val faqs = listOf(
        "How are Energy Credits calculated?" to "Energy credits are generated from verified bi-monthly TNEB bill reductions compared to your baseline consumption, plus active daily check-in streaks on the 9 Energy-Saving Mantras.",
        "How do I find my 12-digit TNEB consumer number?" to "Your 12-digit number is printed on your physical TNEB receipt or card. Format is: 2-digit Region Code, 3-digit Section, 3-digit Distribution, 4-digit Service Number (e.g. 09-440-233-135).",
        "How often should I record meter readings?" to "We recommend taking a quick reading bi-weekly or at the start of your billing cycle so that predictive alerts and tariff warnings are 100% accurate.",
        "How does Solar Lens scan my meter?" to "Solar Lens utilizes on-device computer vision and optical character recognition (OCR) to detect LCD or analog meter dial digits in real-time."
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Help & Support",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Frequently asked questions and advisor assistance",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            Text(
                text = "FREQUENTLY ASKED QUESTIONS",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            // FAQs
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    faqs.forEachIndexed { index, (q, a) ->
                        val isExpanded = expandedFaqIndex == index
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedFaqIndex = if (isExpanded) -1 else index }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = q,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchOnSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = StitchOnSurfaceVariant
                                )
                            }
                            AnimatedVisibility(visible = isExpanded) {
                                Text(
                                    text = a,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = StitchOnSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                        if (index < faqs.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                        }
                    }
                }
            }

            // Quick Help Topics
            Text(
                text = "SUPPORT TOPICS",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupportTopicChip(
                    modifier = Modifier.weight(1f),
                    title = "Bill Status Help",
                    onClick = { expandedFaqIndex = 1 }
                )
                SupportTopicChip(
                    modifier = Modifier.weight(1f),
                    title = "Meter Reading Help",
                    onClick = { expandedFaqIndex = 2 }
                )
                SupportTopicChip(
                    modifier = Modifier.weight(1f),
                    title = "Solar Lens Help",
                    onClick = { expandedFaqIndex = 3 }
                )
            }

            // Direct Contact Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSecondaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, StitchTertiary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Contact Energy Advisor",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchTertiary
                    )
                    Text(
                        text = "Helpline: 1912 (TNEB Toll Free) • Email: support@solarease.in",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@solarease.in?subject=SolarEase%20Support%20Request")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchTertiary)
                    ) {
                        Text("Send Support Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportTopicChip(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainer)
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = StitchOnSurface,
            textAlign = TextAlign.Center
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
    val languages = listOf("English", "Tamil (தமிழ்)")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
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
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Select your preferred application display language",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
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
                                    color = if (isSelected) StitchPrimaryContainer else StitchOnSurface
                                )
                                Text(
                                    text = if (lang.startsWith("Eng")) "Standard English Interface" else "தமிழ் இடைமுகம் மற்றும் விழிப்புணர்வு",
                                    fontSize = 11.5.sp,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectLanguage(lang) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = StitchPrimaryContainer,
                                    unselectedColor = StitchOnSurfaceVariant
                                )
                            )
                        }
                        if (index < languages.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                        }
                    }
                }
            }

            Text(
                text = "Application labels and Solar Mantras will adapt to your selected language.",
                fontSize = 11.5.sp,
                color = StitchOnSurfaceVariant
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer)
            ) {
                Text("Confirm", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutSolarEaseSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SolarEase logo badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(StitchPrimaryFixed.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EnergySavingsLeaf,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = StitchPrimaryContainer
                )
            }

            Text(
                text = "SolarEase",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = StitchOnSurface
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = StitchPrimaryFixed.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Version 2.4.1 (Build 184) • Clean Energy Hub",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchPrimaryContainer
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Empowering Sustainable Homes",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "SolarEase helps consumers monitor domestic electricity consumption, avoid tariff spikes under TNEB LT-1A slabs, and transition seamlessly to rooftop solar power.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = StitchOnSurfaceVariant
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                    Text(
                        text = "Certified Standards: TNEB Smart Grid Compliance • REAP Telemetry v2.4",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("• Terms & Conditions: Licensed for consumer energy telemetry.", fontSize = 11.5.sp, color = StitchOnSurfaceVariant)
                Text("• Privacy Policy: Zero third-party tracker policy.", fontSize = 11.5.sp, color = StitchOnSurfaceVariant)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer)
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewRewardsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                        text = "SolarEase Eco Rewards",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Redeem verified energy savings credits",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            RewardTierItem(
                tier = "Tier 1 Eco Voucher",
                pointsRequired = "200 Pts",
                currentPoints = "98 Pts",
                description = "10% off TNEB Green Tariff recharge & Eco-Partner Voucher",
                isUnlocked = false,
                progress = 0.49f
            )

            RewardTierItem(
                tier = "Tier 2 Solar Subsidy Voucher",
                pointsRequired = "500 Pts",
                currentPoints = "98 Pts",
                description = "Additional ₹2,500 subsidy on MNRE approved rooftop solar installation",
                isUnlocked = false,
                progress = 0.20f
            )

            RewardTierItem(
                tier = "Tier 3 Smart Grid Rebate",
                pointsRequired = "1000 Pts",
                currentPoints = "98 Pts",
                description = "Free bi-directional net meter upgrade consultation",
                isUnlocked = false,
                progress = 0.10f
            )

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer)
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RewardTierItem(
    tier: String,
    pointsRequired: String,
    currentPoints: String,
    description: String,
    isUnlocked: Boolean,
    progress: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSurfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = tier, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isUnlocked) StitchPrimaryFixed else StitchSurfaceContainer
                ) {
                    Text(
                        text = pointsRequired,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isUnlocked) StitchPrimaryContainer else StitchOnSurfaceVariant
                    )
                }
            }
            Text(text = description, fontSize = 12.sp, color = StitchOnSurfaceVariant)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = StitchPrimaryContainer,
                trackColor = StitchSurfaceContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Current: $currentPoints", fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
                Text(text = "Target: $pointsRequired", fontSize = 10.5.sp, color = StitchOnSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreditHistorySheet(onDismiss: () -> Unit) {
    val historyItems = listOf(
        Triple("+30 Pts", "Jan-Feb 2026 Bill Reduction", "30 kWh net reduction verified via TNEB portal"),
        Triple("+48 Pts", "7-Day Energy Mantra Streak", "Daily check-in streak completed for star equipment & cooling"),
        Triple("+20 Pts", "Bi-Monthly Meter Reading", "Verified OCR capture with timestamped telemetry")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = StitchSurfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                        text = "Credit History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Verified transaction ledger (98 Total Credits)",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = StitchOnSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchSurfaceContainer)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    historyItems.forEachIndexed { index, (pts, title, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                                Text(text = desc, fontSize = 11.5.sp, color = StitchOnSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = StitchPrimaryFixed.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = pts,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchPrimaryContainer
                                )
                            }
                        }
                        if (index < historyItems.size - 1) {
                            HorizontalDivider(thickness = 0.5.dp, color = StitchSurfaceContainer)
                        }
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer)
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = StitchPrimaryContainer,
    unfocusedBorderColor = StitchOutline,
    cursorColor = StitchPrimaryContainer,
    focusedLabelColor = StitchPrimaryContainer,
    unfocusedLabelColor = StitchOnSurfaceVariant
)
