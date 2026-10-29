package com.example.feature.profile.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileEnergyDetailsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    private val sampleUser = User(
        id = "user_version1",
        name = "Sanjai",
        username = "sanjai_user",
        mobileNumber = "+91 98765 43210",
        houseAddress = "124 Green Energy Avenue, Chennai"
    )

    private val defaultNotifPrefs = NotificationPreferences()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        UserSession.clear(context)
        UserSession.init(context)
        UserSession.saveUser(context, sampleUser)
        TnebBillCache.init(context)
        TnebBillCache.clear(context)
    }

    @After
    fun tearDown() {
        TnebBillCache.clear(context)
        UserSession.clear(context)
    }

    @Test
    fun test1_energySavingsAndRewardsAreRemovedFromProfile() {
        composeTestRule.setContent {
            ProfileContent(
                user = sampleUser,
                notifPrefs = defaultNotifPrefs,
                selectedLanguage = "English",
                biometricLock = false,
                onSaveProfile = { _, _, _, _, _, _, _ -> },
                onSaveElectricity = { _, _, _ -> },
                onSetNotification = { _, _ -> },
                onSetLanguage = {},
                onSetBiometricLock = {},
                onSignOutClick = {}
            )
        }

        // 1. Old Energy Savings section and metrics must be completely absent
        composeTestRule.onNodeWithText("Energy Savings").assertDoesNotExist()
        composeTestRule.onNodeWithText("Accumulated across verified billing cycles").assertDoesNotExist()
        composeTestRule.onNodeWithText("CO₂ reduction").assertDoesNotExist()
        composeTestRule.onNodeWithText("Bill-based").assertDoesNotExist()

        // 2. Rewards & Credits section must be completely absent
        composeTestRule.onNodeWithText("Rewards & Credits").assertDoesNotExist()
        composeTestRule.onNodeWithText("TIER 1 ACTIVE").assertDoesNotExist()
        composeTestRule.onNodeWithText("Energy Credits: 98").assertDoesNotExist()
        composeTestRule.onNodeWithText("View Rewards").assertDoesNotExist()
        composeTestRule.onNodeWithText("Credit History").assertDoesNotExist()

        // 3. New Energy Details section must be displayed
        composeTestRule.onNodeWithText("Energy Details").assertExists()
        composeTestRule.onNodeWithText("CURRENT CONSUMPTION").assertExists()
        composeTestRule.onNodeWithText("CONSUMPTION DIFFERENCE").assertExists()
    }

    @Test
    fun test2_energyDetailsDisplaysEmptyStateWhenNoBillingData() {
        // TnebBillCache is empty
        composeTestRule.setContent {
            ProfileContent(
                user = sampleUser,
                notifPrefs = defaultNotifPrefs,
                selectedLanguage = "English",
                biometricLock = false,
                onSaveProfile = { _, _, _, _, _, _, _ -> },
                onSaveElectricity = { _, _, _ -> },
                onSetNotification = { _, _ -> },
                onSetLanguage = {},
                onSetBiometricLock = {},
                onSignOutClick = {}
            )
        }

        // When no billing data exists, both metrics should show Unavailable and graceful message
        composeTestRule.onAllNodesWithText("Unavailable").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("Consumption data unavailable").assertCountEquals(2)
        composeTestRule.onNodeWithText("Consumption data unavailable. Connect your TNEB consumer number in My Details to track verified billing usage.").assertExists()
    }

    @Test
    fun test3_energyDetailsDecreasedConsumptionCalculation() {
        // Current cycle: 245 kWh, Previous cycle: 280 kWh -> Diff: -35 kWh (Decreased)
        val billData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-001",
                    period = "Aug–Oct",
                    assessmentDate = "15/10/2026",
                    units = "245"
                ),
                TnebBillRecord(
                    id = "BILL-002",
                    period = "Jun–Aug",
                    assessmentDate = "15/08/2026",
                    units = "280"
                )
            )
        )
        TnebBillCache.save(context, billData)

        composeTestRule.setContent {
            ProfileContent(
                user = sampleUser,
                notifPrefs = defaultNotifPrefs,
                selectedLanguage = "English",
                biometricLock = false,
                onSaveProfile = { _, _, _, _, _, _, _ -> },
                onSaveElectricity = { _, _, _ -> },
                onSetNotification = { _, _ -> },
                onSetLanguage = {},
                onSetBiometricLock = {},
                onSignOutClick = {}
            )
        }

        // Metric 1: Current Consumption -> 245 kWh
        composeTestRule.onNodeWithText("245").assertExists()

        // Metric 2: Consumption Difference -> ↓ 35 kWh
        composeTestRule.onNodeWithText("↓ 35").assertExists()
        composeTestRule.onNodeWithText("↓ 35 kWh compared with Jun–Aug").assertExists()
    }

    @Test
    fun test4_energyDetailsIncreasedConsumptionCalculation() {
        // Current cycle: 245 kWh, Previous cycle: 220 kWh -> Diff: +25 kWh (Increased)
        val billData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-001",
                    period = "Aug–Oct",
                    assessmentDate = "15/10/2026",
                    units = "245"
                ),
                TnebBillRecord(
                    id = "BILL-002",
                    period = "Jun–Aug",
                    assessmentDate = "15/08/2026",
                    units = "220"
                )
            )
        )
        TnebBillCache.save(context, billData)

        composeTestRule.setContent {
            ProfileContent(
                user = sampleUser,
                notifPrefs = defaultNotifPrefs,
                selectedLanguage = "English",
                biometricLock = false,
                onSaveProfile = { _, _, _, _, _, _, _ -> },
                onSaveElectricity = { _, _, _ -> },
                onSetNotification = { _, _ -> },
                onSetLanguage = {},
                onSetBiometricLock = {},
                onSignOutClick = {}
            )
        }

        // Metric 1: Current Consumption -> 245 kWh
        composeTestRule.onNodeWithText("245").assertExists()

        // Metric 2: Consumption Difference -> ↑ 25 kWh
        composeTestRule.onNodeWithText("↑ 25").assertExists()
        composeTestRule.onNodeWithText("↑ 25 kWh compared with Jun–Aug").assertExists()
    }

    @Test
    fun test5_energyDetailsUnchangedConsumptionCalculation() {
        // Current cycle: 245 kWh, Previous cycle: 245 kWh -> Diff: 0 kWh (Unchanged)
        val billData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-001",
                    period = "Aug–Oct",
                    assessmentDate = "15/10/2026",
                    units = "245"
                ),
                TnebBillRecord(
                    id = "BILL-002",
                    period = "Jun–Aug",
                    assessmentDate = "15/08/2026",
                    units = "245"
                )
            )
        )
        TnebBillCache.save(context, billData)

        composeTestRule.setContent {
            ProfileContent(
                user = sampleUser,
                notifPrefs = defaultNotifPrefs,
                selectedLanguage = "English",
                biometricLock = false,
                onSaveProfile = { _, _, _, _, _, _, _ -> },
                onSaveElectricity = { _, _, _ -> },
                onSetNotification = { _, _ -> },
                onSetLanguage = {},
                onSetBiometricLock = {},
                onSignOutClick = {}
            )
        }

        // Metric 1: Current Consumption -> 245 kWh
        composeTestRule.onNodeWithText("245").assertExists()

        // Metric 2: Consumption Difference -> No change (0 kWh)
        composeTestRule.onNodeWithText("No change").assertExists()
        composeTestRule.onNodeWithText("(0 kWh)").assertExists()
        composeTestRule.onNodeWithText("Equal to Jun–Aug (0 kWh)").assertExists()
    }
}
