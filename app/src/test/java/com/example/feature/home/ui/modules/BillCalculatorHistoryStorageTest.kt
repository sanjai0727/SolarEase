package com.example.feature.home.ui.modules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillCalculatorHistoryStorageTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        BillCalculatorHistoryStorage.resetForTesting()
        BillCalculatorHistoryStorage.init(context)
    }

    @After
    fun tearDown() {
        BillCalculatorHistoryStorage.resetForTesting()
    }

    @Test
    fun testSaveAndRetrieveCalculation() {
        val billResult = TNEBBillCalculator.calculateTNEBBill(450.0)
        val calc = SavedCalculation(
            units = 450.0,
            billResult = billResult,
            cycle = BillingCycle.BIMONTHLY
        )

        BillCalculatorHistoryStorage.saveCalculation(calc)

        val retrieved = BillCalculatorHistoryStorage.getAll()
        assertEquals(1, retrieved.size)
        assertEquals(calc.id, retrieved[0].id)
        assertEquals(450.0, retrieved[0].units, 0.001)
        assertEquals(BillingCycle.BIMONTHLY, retrieved[0].cycle)
        assertEquals(billResult.totalEnergyCharge, retrieved[0].billResult.totalEnergyCharge, 0.001)
    }

    @Test
    fun testPersistenceAcrossReload() {
        val calc1 = SavedCalculation(
            timestamp = 1000L,
            units = 200.0,
            billResult = TNEBBillCalculator.calculateTNEBBill(200.0),
            cycle = BillingCycle.MONTHLY
        )
        val calc2 = SavedCalculation(
            timestamp = 2000L,
            units = 600.0,
            billResult = TNEBBillCalculator.calculateTNEBBill(600.0),
            cycle = BillingCycle.BIMONTHLY
        )

        BillCalculatorHistoryStorage.saveCalculation(calc1)
        BillCalculatorHistoryStorage.saveCalculation(calc2)

        // Simulate app restart / reload by reading directly from SharedPreferences
        val prefs = context.getSharedPreferences("SolarEaseBillCalculatorPrefs", Context.MODE_PRIVATE)
        val rawJson = prefs.getString("saved_calculations_history", null)
        assertNotNull(rawJson)
        assertTrue(rawJson!!.contains("600.0"))
        assertTrue(rawJson.contains("200.0"))

        // Reinitialize storage with the existing SharedPreferences
        BillCalculatorHistoryStorage.init(prefs)
        val history = BillCalculatorHistoryStorage.getAll()

        assertEquals(2, history.size)
        // Verify newest first ordering
        assertEquals(600.0, history[0].units, 0.001)
        assertEquals(200.0, history[1].units, 0.001)
        assertTrue(history[0].timestamp > history[1].timestamp)
    }

    @Test
    fun testDeleteAndClearHistory() {
        val calc1 = SavedCalculation(
            units = 150.0,
            billResult = TNEBBillCalculator.calculateTNEBBill(150.0),
            cycle = BillingCycle.MONTHLY
        )
        val calc2 = SavedCalculation(
            units = 300.0,
            billResult = TNEBBillCalculator.calculateTNEBBill(300.0),
            cycle = BillingCycle.BIMONTHLY
        )

        BillCalculatorHistoryStorage.saveCalculation(calc1)
        BillCalculatorHistoryStorage.saveCalculation(calc2)
        assertEquals(2, BillCalculatorHistoryStorage.getAll().size)

        // Delete individual calculation
        BillCalculatorHistoryStorage.deleteCalculation(calc1.id)
        val remaining = BillCalculatorHistoryStorage.getAll()
        assertEquals(1, remaining.size)
        assertEquals(calc2.id, remaining[0].id)

        // Clear all
        BillCalculatorHistoryStorage.clearHistory()
        assertTrue(BillCalculatorHistoryStorage.getAll().isEmpty())
    }
}
