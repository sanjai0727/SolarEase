package com.example.feature.profile.ui

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.ebservice.util.ConsumerNumberParser
import com.example.feature.ebservice.util.cleanIndianMobile
import com.example.feature.ebservice.util.formatConsumerNumber
import com.example.feature.ebservice.util.formatIndianMobile
import com.example.feature.ebservice.util.isValidIndianMobile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileElectricityDetailsTest {

    private lateinit var context: Context
    private lateinit var application: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        application = context as Application
        UserSession.clear(context)
        UserSession.init(context)
    }

    @After
    fun tearDown() {
        UserSession.clear(context)
    }

    @Test
    fun test1_consumerNumberAndTnebMobilePersistAcrossAppRestart() {
        // Set up initial user with authentication mobile
        val initialUser = User(
            id = "USR-001",
            name = "Test User",
            username = "testuser",
            mobileNumber = "+91 91234 56789"
        )
        UserSession.saveUser(context, initialUser)

        val viewModel = ProfileViewModel(application)

        var saveSuccess = false
        var saveError: String? = null
        viewModel.updateElectricityDetails("09-440-233-135", "9876543210") { success, err ->
            saveSuccess = success
            saveError = err
        }

        assertTrue("Expected save to succeed, but got error: $saveError", saveSuccess)
        assertNull(saveError)

        val updatedUser = UserSession.currentUser
        assertNotNull(updatedUser)
        assertEquals("09440233135", updatedUser?.ebServiceNumber)
        assertEquals("9876543210", updatedUser?.tnebRegisteredMobile)
        // Authentication mobile MUST NOT be altered
        assertEquals("+91 91234 56789", updatedUser?.mobileNumber)

        // Simulate app kill and cold restart
        UserSession.currentUser = null
        UserSession.init(context)

        val restoredUser = UserSession.currentUser
        assertNotNull(restoredUser)
        assertEquals("09440233135", restoredUser?.ebServiceNumber)
        assertEquals("9876543210", restoredUser?.tnebRegisteredMobile)
        assertEquals("+91 91234 56789", restoredUser?.mobileNumber)
    }

    @Test
    fun test2_separationOfAuthenticationMobileAndTnebRegisteredMobile() {
        val user = User(
            id = "USR-002",
            name = "John Doe",
            username = "johndoe",
            mobileNumber = "+91 90000 00001" // Authentication Mobile
        )
        UserSession.saveUser(context, user)

        val viewModel = ProfileViewModel(application)
        viewModel.updateElectricityDetails("09440233135", "9888877777") { success, _ ->
            assertTrue(success)
        }

        val currentUser = UserSession.currentUser
        assertNotNull(currentUser)
        // Check complete independence
        assertEquals("+91 90000 00001", currentUser?.mobileNumber)
        assertEquals("9888877777", currentUser?.tnebRegisteredMobile)
    }

    @Test
    fun test3_editConsumerNumberPersistsAndRetainsAuthMobile() {
        val user = User(
            id = "USR-003",
            name = "Jane Doe",
            username = "janedoe",
            mobileNumber = "+91 95555 44444",
            ebServiceNumber = "09440233135",
            tnebRegisteredMobile = "9876543210"
        )
        UserSession.saveUser(context, user)

        val viewModel = ProfileViewModel(application)
        
        // User taps Edit and changes consumer number
        var editSuccess = false
        viewModel.updateElectricityDetails("09440233136", "9876543210") { success, _ ->
            editSuccess = success
        }
        assertTrue(editSuccess)

        val updatedUser = UserSession.currentUser
        assertNotNull(updatedUser)
        assertEquals("09440233136", updatedUser?.ebServiceNumber)
        assertEquals("9876543210", updatedUser?.tnebRegisteredMobile)
        assertEquals("+91 95555 44444", updatedUser?.mobileNumber)

        // Check format
        assertEquals("09-440-233-136", formatConsumerNumber(updatedUser?.ebServiceNumber ?: ""))
        assertEquals("+91 98765 43210", formatIndianMobile(updatedUser?.tnebRegisteredMobile ?: ""))
    }

    @Test
    fun test4_consumerNumberValidation() {
        // Valid 11-digit consumer number
        val res11 = ConsumerNumberParser.parse("09440233135")
        assertTrue(res11.isSuccess)
        assertEquals("Chennai South", res11.getOrNull()?.regionName)

        // Valid 12-digit consumer number
        val res12 = ConsumerNumberParser.parse("044540062222")
        assertTrue(res12.isSuccess)
        assertEquals("Erode", res12.getOrNull()?.regionName)

        // Formatted with dashes
        val resDash = ConsumerNumberParser.parse("09-440-233-135")
        assertTrue(resDash.isSuccess)
        assertEquals("Chennai South", resDash.getOrNull()?.regionName)

        // Invalid region code
        val resInvalidRegion = ConsumerNumberParser.parse("99440233135")
        assertFalse(resInvalidRegion.isSuccess)

        // Invalid length
        val resTooShort = ConsumerNumberParser.parse("12345")
        assertFalse(resTooShort.isSuccess)
    }

    @Test
    fun test5_indianMobileValidation() {
        assertTrue(isValidIndianMobile("9876543210"))
        assertTrue(isValidIndianMobile("+91 98765 43210"))
        assertTrue(isValidIndianMobile("09876543210"))
        assertTrue(isValidIndianMobile("6123456789"))
        assertTrue(isValidIndianMobile("7123456789"))
        assertTrue(isValidIndianMobile("8123456789"))

        // Invalid: Starts with 5 or fewer digits
        assertFalse(isValidIndianMobile("5123456789"))
        assertFalse(isValidIndianMobile("1234567890"))
        assertFalse(isValidIndianMobile("98765"))
    }
}
