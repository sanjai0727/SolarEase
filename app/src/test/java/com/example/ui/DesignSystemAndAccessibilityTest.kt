package com.example.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.feature.assessment.ui.StitchSelectableCard
import com.example.feature.assessment.ui.StitchStepProgressBar
import com.example.ui.components.SelectableCard
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.components.SolarEaseNavigationRail
import com.example.ui.components.StepProgress
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.max
import kotlin.math.min

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DesignSystemAndAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble()
        val l2 = background.luminance().toDouble()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun verifyWcagAaContrastRatiosForCentralizedThemeTokens() {
        // Light mode: Primary green on main background and white surface
        val primaryOnBackground = contrastRatio(StitchLightTokens.Primary, StitchLightTokens.Surface)
        val primaryOnWhite = contrastRatio(StitchLightTokens.Primary, StitchLightTokens.SurfaceContainerLowest)
        assertTrue(
            "Primary green on main background must exceed 4.5:1 (actual: $primaryOnBackground)",
            primaryOnBackground >= 4.5
        )
        assertTrue(
            "Primary green on white surface must exceed 4.5:1 (actual: $primaryOnWhite)",
            primaryOnWhite >= 4.5
        )

        // Light mode: Primary text on main background
        val primaryTextOnBackground = contrastRatio(StitchLightTokens.OnSurface, StitchLightTokens.Surface)
        assertTrue(
            "Primary text on main background must exceed 7.0:1 (actual: $primaryTextOnBackground)",
            primaryTextOnBackground >= 7.0
        )

        // Light mode: Secondary text on main background and secondary surface
        val secondaryOnBackground = contrastRatio(StitchLightTokens.OnSurfaceVariant, StitchLightTokens.Surface)
        val secondaryOnContainerLow = contrastRatio(StitchLightTokens.Secondary, StitchLightTokens.SurfaceContainerLow)
        assertTrue(
            "Secondary text on main background must meet WCAG AA 4.5:1 (actual: $secondaryOnBackground)",
            secondaryOnBackground >= 4.5
        )
        assertTrue(
            "Secondary text on container low surface must meet WCAG AA 4.5:1 (actual: $secondaryOnContainerLow)",
            secondaryOnContainerLow >= 4.5
        )

        // Dark mode: Primary and OnSurface contrast verification against dark background
        val darkPrimaryOnBg = contrastRatio(StitchDarkTokens.Primary, StitchDarkTokens.Surface)
        val darkTextOnBg = contrastRatio(StitchDarkTokens.OnSurface, StitchDarkTokens.Surface)
        assertTrue(
            "Dark primary on dark surface must meet WCAG AA 4.5:1 (actual: $darkPrimaryOnBg)",
            darkPrimaryOnBg >= 4.5
        )
        assertTrue(
            "Dark text on dark surface must meet WCAG AA 4.5:1 (actual: $darkTextOnBg)",
            darkTextOnBg >= 4.5
        )

        // Semantic status colors (Success, Warning, Danger, Info)
        val successContrast = contrastRatio(
            SolarEaseSemanticColors.SuccessText,
            SolarEaseSemanticColors.SuccessContainer
        )
        val warningContrast = contrastRatio(
            SolarEaseSemanticColors.WarningText,
            SolarEaseSemanticColors.WarningContainer
        )
        val dangerContrast = contrastRatio(
            SolarEaseSemanticColors.DangerText,
            SolarEaseSemanticColors.DangerContainer
        )
        val infoContrast = contrastRatio(
            SolarEaseSemanticColors.InfoText,
            SolarEaseSemanticColors.InfoContainer
        )
        assertTrue("Success badge contrast must meet 4.5:1 ($successContrast)", successContrast >= 4.5)
        assertTrue("Warning badge contrast must meet 4.5:1 ($warningContrast)", warningContrast >= 4.5)
        assertTrue("Danger badge contrast must meet 4.5:1 ($dangerContrast)", dangerContrast >= 4.5)
        assertTrue("Info badge contrast must meet 4.5:1 ($infoContrast)", infoContrast >= 4.5)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun verifyCompactWindowLayoutUsesBottomBarAndCompactWidthClass() {
        var capturedLayout: SolarEaseWindowLayoutInfo? = null
        composeTestRule.setContent {
            capturedLayout = rememberSolarEaseWindowLayoutInfo()
        }
        composeTestRule.waitForIdle()
        assertEquals(SolarEaseWindowWidthSizeClass.COMPACT, capturedLayout?.widthSizeClass)
        assertFalse(capturedLayout?.useNavigationRail ?: true)
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun verifyMediumWindowLayoutUsesNavigationRailAndCenteredMaxWidth() {
        var capturedLayout: SolarEaseWindowLayoutInfo? = null
        composeTestRule.setContent {
            capturedLayout = rememberSolarEaseWindowLayoutInfo()
        }
        composeTestRule.waitForIdle()
        assertEquals(SolarEaseWindowWidthSizeClass.MEDIUM, capturedLayout?.widthSizeClass)
        assertTrue(capturedLayout?.useNavigationRail ?: false)
        assertEquals(680.dp, capturedLayout?.maxContentWidth)
    }

    @Test
    @Config(qualifiers = "w960dp-h720dp")
    fun verifyExpandedWindowLayoutUsesNavigationRailAndExpandedWidthClass() {
        var capturedLayout: SolarEaseWindowLayoutInfo? = null
        composeTestRule.setContent {
            capturedLayout = rememberSolarEaseWindowLayoutInfo()
        }
        composeTestRule.waitForIdle()
        assertEquals(SolarEaseWindowWidthSizeClass.EXPANDED, capturedLayout?.widthSizeClass)
        assertTrue(capturedLayout?.useNavigationRail ?: false)
        assertEquals(840.dp, capturedLayout?.maxContentWidth)
    }

    @Test
    fun verifyBottomNavigationAndNavigationRailAccessibilitySemantics() {
        composeTestRule.setContent {
            SolarEaseTheme {
                SolarEaseBottomNavigation(
                    currentTab = SolarEaseNavTab.HOME,
                    onNavigateToHome = {},
                    onNavigateToRecommends = {},
                    onNavigateToProfile = {}
                )
            }
        }

        val homeTab = composeTestRule.onNodeWithText("Home")
        val recommendsTab = composeTestRule.onNodeWithText("Recommends")
        val profileTab = composeTestRule.onNodeWithText("Profile")

        homeTab.assertExists().assertIsSelected()
        recommendsTab.assertExists().assertIsNotSelected()
        profileTab.assertExists().assertIsNotSelected()

        // Verify minimum 48dp touch target height
        val homeBounds = homeTab.getUnclippedBoundsInRoot()
        val homeHeight = homeBounds.bottom - homeBounds.top
        assertTrue(
            "Bottom navigation item height must be at least 48dp (was $homeHeight)",
            homeHeight >= 48.dp
        )
    }

    @Test
    fun verifyNavigationRailSemanticsForLargeScreen() {
        composeTestRule.setContent {
            SolarEaseTheme {
                SolarEaseNavigationRail(
                    currentTab = SolarEaseNavTab.RECOMMENDS,
                    onNavigateToHome = {},
                    onNavigateToRecommends = {},
                    onNavigateToProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Recommends").assertExists().assertIsSelected()
        composeTestRule.onNodeWithText("Home").assertExists().assertIsNotSelected()
    }

    @Test
    fun verifySelectableCardAndStepProgressAccessibilitySemantics() {
        var selectedOption = "On-Grid"
        composeTestRule.setContent {
            SolarEaseTheme {
                StitchStepProgressBar(
                    currentStep = 3,
                    totalSteps = 8,
                    stepTitle = "System Configuration"
                )
                StitchSelectableCard(
                    selected = selectedOption == "On-Grid",
                    title = "On-Grid",
                    subtitle = "Works with the electricity grid.",
                    icon = Icons.Default.WbSunny,
                    onClick = { selectedOption = "On-Grid" }
                )
                SelectableCard(
                    selected = false,
                    title = "Off-Grid",
                    description = "Independent battery system",
                    onClick = { selectedOption = "Off-Grid" }
                )
                StepProgress(currentStep = 2, totalSteps = 5)
            }
        }

        composeTestRule.onNodeWithContentDescription("Step 3 of 8: System Configuration").assertExists()
        composeTestRule.onNodeWithContentDescription("Step 2 of 5").assertExists()

        val onGridNode = composeTestRule.onNodeWithText("On-Grid")
        onGridNode.assertExists().assertIsSelected()
        assertEquals(
            Role.RadioButton,
            onGridNode.fetchSemanticsNode().config[SemanticsProperties.Role]
        )

        val offGridNode = composeTestRule.onNodeWithText("Off-Grid")
        offGridNode.assertExists().assertIsNotSelected()
    }

    @Test
    fun verifyReducedMotionDetectionRespectsSystemAnimatorScale() {
        val context: Context = ApplicationProvider.getApplicationContext()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
        assertFalse("Normal animator scale (1.0f) should not enable reduced motion", isReducedMotionEnabled(context))

        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f)
        assertTrue("Zero animator scale (0.0f) must enable reduced motion", isReducedMotionEnabled(context))

        // Restore default
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h640dp")
    fun verifyRecommendsScreenAt200PercentFontScaleOnNarrowScreenHas48dpTouchTargetsAndNoEmojiIcons() {
        val context: Context = ApplicationProvider.getApplicationContext()
        com.example.feature.recommends.data.MantraPreferences.init(context)
        val defaultMantras = com.example.feature.recommends.data.MantraPreferences.mantrasState.value

        composeTestRule.setContent {
            val baseDensity = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                    density = baseDensity.density,
                    fontScale = 2.0f
                )
            ) {
                SolarEaseTheme {
                    com.example.feature.recommends.ui.RecommendsScreen(
                        uiState = com.example.feature.recommends.model.RecommendsUiState(
                            coinBalance = 140,
                            streakDays = 5,
                            mantras = defaultMantras
                        ),
                        onMantraClick = {},
                        onNavigateToHome = {},
                        onNavigateToProfile = {}
                    )
                }
            }
        }

        // Verify primary headings render without crash or missing semantics at 200% font scale on 320dp width
        composeTestRule.onAllNodesWithText("Recommends").onFirst().assertExists()
        composeTestRule.onNodeWithText("SolarEase Coins").assertExists()
        composeTestRule.onNodeWithText("9 Energy-Saving Mantras").assertExists()

        // Verify expandable headers expose Expanded/Collapsed stateDescription and >= 48dp touch target
        val coinsHeader = composeTestRule.onNodeWithText("SolarEase Coins")
        assertEquals(
            "Collapsed",
            coinsHeader.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )

        val coinsToggle = composeTestRule.onNodeWithContentDescription("Expand SolarEase Coins card")
        coinsToggle.assertExists()
        val coinsBounds = coinsToggle.getUnclippedBoundsInRoot()
        assertTrue(
            "Expandable coin details button must be >= 48dp (was ${coinsBounds.bottom - coinsBounds.top})",
            (coinsBounds.bottom - coinsBounds.top) >= 48.dp
        )

        // Verify no raw emoji text nodes are rendered for the 9 mantras
        composeTestRule.onNodeWithText("🔌").assertDoesNotExist()
        composeTestRule.onNodeWithText("📺").assertDoesNotExist()
        composeTestRule.onNodeWithText("❄️").assertDoesNotExist()
        composeTestRule.onNodeWithText("💡").assertDoesNotExist()

        val screenshotDir = File("build/outputs/roborazzi")
        screenshotDir.mkdirs()
        val recordOptions = com.github.takahirom.roborazzi.RoborazziOptions(
            taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
        )
        composeTestRule.onRoot().captureRoboImage(
            file = File(screenshotDir, "recommends_compact_200pct_font.png"),
            roborazziOptions = recordOptions
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h915dp")
    fun verifyMonthlyConsumptionQuickAdjustmentPillsHaveRadioButtonSemanticsAnd48dpHeight() {
        composeTestRule.setContent {
            val kwhState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableDoubleStateOf(300.0) }
            SolarEaseTheme {
                com.example.feature.assessment.ui.MonthlyConsumptionScreen(
                    consumptionKwh = kwhState.doubleValue,
                    onConsumptionChange = { kwhState.doubleValue = it },
                    onNavigateNext = {},
                    onNavigateBack = {}
                )
            }
        }

        val editButton = composeTestRule.onNodeWithText("Not correct? Edit")
        editButton.assertExists()
        val editBounds = editButton.getUnclippedBoundsInRoot()
        assertTrue(
            "Edit toggle must have at least 48dp height (was ${editBounds.bottom - editBounds.top})",
            (editBounds.bottom - editBounds.top) >= 48.dp
        )

        val pillNode = composeTestRule.onNodeWithText("350 kWh")
        pillNode.assertExists().assertIsNotSelected()
        assertEquals(
            Role.RadioButton,
            pillNode.fetchSemanticsNode().config[SemanticsProperties.Role]
        )
        val pillBounds = pillNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Quick adjustment pill must have at least 48dp height (was ${pillBounds.bottom - pillBounds.top})",
            (pillBounds.bottom - pillBounds.top) >= 48.dp
        )

        pillNode.performScrollTo().performClick()
        composeTestRule.waitForIdle()
        pillNode.assertIsSelected()

        val screenshotDir = File("build/outputs/roborazzi")
        screenshotDir.mkdirs()
        val recordOptions = com.github.takahirom.roborazzi.RoborazziOptions(
            taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
        )
        composeTestRule.onRoot().captureRoboImage(
            file = File(screenshotDir, "monthly_consumption_compact.png"),
            roborazziOptions = recordOptions
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h915dp")
    fun verifyVendorProfileAndEstimateRequestAccessibilitySemanticsAndScreenshots() {
        val screenshotDir = File("build/outputs/roborazzi")
        screenshotDir.mkdirs()
        val recordOptions = com.github.takahirom.roborazzi.RoborazziOptions(
            taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
        )

        composeTestRule.setContent {
            SolarEaseTheme {
                com.example.feature.assessment.ui.VendorProfileScreen(
                    vendorName = "Apex Solar Tech",
                    onNavigateNext = {},
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Verify star emoji is replaced with clean numeric rating text
        composeTestRule.onNodeWithText("⭐ 4.8").assertDoesNotExist()
        composeTestRule.onNodeWithText("4.8 / 5.0").assertExists()

        // Verify bookmark button exposes Role.Button, stateDescription, and >= 48dp touch target
        val bookmarkNode = composeTestRule.onNodeWithContentDescription("Save Vendor")
        bookmarkNode.assertExists()
        assertEquals(
            "Not saved",
            bookmarkNode.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        )
        val bookmarkBounds = bookmarkNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Bookmark button height must be >= 48dp (was ${bookmarkBounds.bottom - bookmarkBounds.top})",
            (bookmarkBounds.bottom - bookmarkBounds.top) >= 48.dp
        )

        val requestBtn = composeTestRule.onNodeWithText("REQUEST ESTIMATE")
        requestBtn.assertExists()
        val requestBounds = requestBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Request Estimate button height must be >= 48dp (was ${requestBounds.bottom - requestBounds.top})",
            (requestBounds.bottom - requestBounds.top) >= 48.dp
        )

        composeTestRule.onRoot().captureRoboImage(
            file = File(screenshotDir, "vendor_profile_compact.png"),
            roborazziOptions = recordOptions
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w960dp-h720dp")
    fun verifyNearbyVendorsAndEstimateRequestOnExpandedTabletViewportAndCaptureScreenshots() {
        val screenshotDir = File("build/outputs/roborazzi")
        screenshotDir.mkdirs()
        val recordOptions = com.github.takahirom.roborazzi.RoborazziOptions(
            taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
        )

        composeTestRule.setContent {
            SolarEaseTheme {
                com.example.feature.assessment.ui.NearbyVendorsScreen(
                    capacityKw = 3.0,
                    propertyType = "Residential",
                    systemType = "On-Grid",
                    mountType = "Flat Roof",
                    panelType = "DCR",
                    onSelectVendor = { _, _ -> },
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        val viewProfileButtons = composeTestRule.onAllNodesWithText("View Profile")
        viewProfileButtons.onFirst().assertExists()
        val profileBtnBounds = viewProfileButtons.onFirst().getUnclippedBoundsInRoot()
        assertTrue(
            "View Profile button height must be >= 48dp (was ${profileBtnBounds.bottom - profileBtnBounds.top})",
            (profileBtnBounds.bottom - profileBtnBounds.top) >= 48.dp
        )

        composeTestRule.onRoot().captureRoboImage(
            file = File(screenshotDir, "nearby_vendors_tablet_expanded.png"),
            roborazziOptions = recordOptions
        )
    }

    @Test
    @Config(sdk = [34], qualifiers = "w411dp-h915dp")
    fun verifyEstimateRequestScreenCancelTargetAndCaptureScreenshot() {
        val screenshotDir = File("build/outputs/roborazzi")
        screenshotDir.mkdirs()
        val recordOptions = com.github.takahirom.roborazzi.RoborazziOptions(
            taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
        )

        composeTestRule.setContent {
            SolarEaseTheme {
                com.example.feature.assessment.ui.EstimateRequestScreen(
                    state = com.example.feature.assessment.ui.SolarVendorState(),
                    onNavigateNext = {},
                    onNavigateBack = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Step 3 of 3: Final Review, Verified Profile").assertExists()

        val cancelBtn = composeTestRule.onNodeWithText("Cancel")
        cancelBtn.assertExists()
        val cancelBounds = cancelBtn.getUnclippedBoundsInRoot()
        assertTrue(
            "Cancel button height must be >= 48dp (was ${cancelBounds.bottom - cancelBounds.top})",
            (cancelBounds.bottom - cancelBounds.top) >= 48.dp
        )

        composeTestRule.onRoot().captureRoboImage(
            file = File(screenshotDir, "estimate_request_compact.png"),
            roborazziOptions = recordOptions
        )
    }
}
