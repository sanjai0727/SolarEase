package com.example.feature.home.ui.modules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillCalculatorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: BillCalculatorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        BillCalculatorHistoryStorage.resetForTesting()
        viewModel = BillCalculatorViewModel()
    }

    @After
    fun tearDown() {
        BillCalculatorHistoryStorage.resetForTesting()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState() {
        assertEquals(CalculatorScreenState.ENTRY, viewModel.screenState.value)
        assertEquals("", viewModel.inputText.value)
        assertNull(viewModel.inputError.value)
        assertNull(viewModel.billResult.value)
        assertEquals(BillingCycle.BIMONTHLY, viewModel.billingCycle.value)
        assertTrue(viewModel.savedCalculations.value.isEmpty())
    }

    @Test
    fun testInputAndPresets() {
        viewModel.onInputChanged("450")
        assertEquals("450", viewModel.inputText.value)

        viewModel.selectPreset(650.0)
        assertEquals("650", viewModel.inputText.value)

        viewModel.clearInput()
        assertEquals("", viewModel.inputText.value)
    }

    @Test
    fun testBillingCycleToggle() {
        viewModel.setBillingCycle(BillingCycle.MONTHLY)
        assertEquals(BillingCycle.MONTHLY, viewModel.billingCycle.value)

        viewModel.setBillingCycle(BillingCycle.BIMONTHLY)
        assertEquals(BillingCycle.BIMONTHLY, viewModel.billingCycle.value)
    }

    @Test
    fun testValidationOnEmptyCalculate() {
        viewModel.calculate()
        assertNotNull(viewModel.inputError.value)
        assertEquals(CalculatorScreenState.ENTRY, viewModel.screenState.value)
    }

    @Test
    fun testCalculationFlow() = runTest(testDispatcher) {
        viewModel.onInputChanged("600")
        viewModel.calculate()
        testScheduler.runCurrent()

        // Advances step 1
        assertEquals(CalculatorScreenState.CALCULATING, viewModel.screenState.value)
        assertEquals(1, viewModel.calculationStep.value)

        advanceTimeBy(550)
        testScheduler.runCurrent()
        assertEquals(2, viewModel.calculationStep.value)

        advanceTimeBy(650)
        testScheduler.runCurrent()
        assertEquals(3, viewModel.calculationStep.value)

        advanceTimeBy(550)
        testScheduler.runCurrent()
        assertEquals(CalculatorScreenState.RESULT, viewModel.screenState.value)

        val result = viewModel.billResult.value
        assertNotNull(result)
        assertEquals(600.0, result!!.totalUnits, 0.001)
        assertEquals(100.0, result.freeUnits, 0.001)
        assertTrue(result.totalEnergyCharge > 0)

        // Verifies session history tracking
        val history = viewModel.savedCalculations.value
        assertEquals(1, history.size)
        assertEquals(600.0, history[0].units, 0.001)
        assertEquals(BillingCycle.BIMONTHLY, history[0].cycle)
    }

    @Test
    fun testHistoryNavigationAndDeletion() = runTest(testDispatcher) {
        viewModel.onInputChanged("300")
        viewModel.calculate()
        advanceUntilIdle()

        viewModel.navigateToHistory()
        assertEquals(CalculatorScreenState.HISTORY, viewModel.screenState.value)

        val item = viewModel.savedCalculations.value.first()
        viewModel.deleteHistoryItem(item.id)
        assertTrue(viewModel.savedCalculations.value.isEmpty())

        viewModel.navigateToEntry()
        assertEquals(CalculatorScreenState.ENTRY, viewModel.screenState.value)
    }

    @Test
    fun testMultipleCalculationsNewestFirstOrdering() = runTest(testDispatcher) {
        // First calculation: 250 units
        viewModel.onInputChanged("250")
        viewModel.calculate()
        advanceUntilIdle()

        // Second calculation: 500 units
        viewModel.onInputChanged("500")
        viewModel.calculate()
        advanceUntilIdle()

        val history = viewModel.savedCalculations.value
        assertEquals(2, history.size)
        // Newest calculation should appear first
        assertEquals(500.0, history[0].units, 0.001)
        assertEquals(250.0, history[1].units, 0.001)
        assertTrue(history[0].timestamp >= history[1].timestamp)
    }

    @Test
    fun testClearHistory() = runTest(testDispatcher) {
        viewModel.onInputChanged("200")
        viewModel.calculate()
        advanceUntilIdle()

        viewModel.onInputChanged("400")
        viewModel.calculate()
        advanceUntilIdle()

        assertEquals(2, viewModel.savedCalculations.value.size)
        viewModel.clearHistory()
        assertTrue(viewModel.savedCalculations.value.isEmpty())
    }

    @Test
    fun testOpenSavedResultFromHistory() = runTest(testDispatcher) {
        viewModel.onInputChanged("350")
        viewModel.setBillingCycle(BillingCycle.MONTHLY)
        viewModel.calculate()
        advanceUntilIdle()

        val saved = viewModel.savedCalculations.value.first()
        viewModel.navigateToEntry()
        assertEquals(CalculatorScreenState.ENTRY, viewModel.screenState.value)

        viewModel.openSavedResult(saved)
        assertEquals(CalculatorScreenState.RESULT, viewModel.screenState.value)
        assertEquals("350", viewModel.inputText.value)
        assertEquals(BillingCycle.MONTHLY, viewModel.billingCycle.value)
        assertEquals(350.0, viewModel.billResult.value?.totalUnits ?: 0.0, 0.001)
    }
}
