package com.example.feature.home.ui.modules

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.feature.home.ui.HomeViewModel
import com.example.feature.home.ui.ReapTier
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.DetectionResult
import com.example.feature.lens.data.LensApi
import com.example.feature.lens.data.LensRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import okhttp3.RequestBody
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
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeterReadingCorrectionTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    private val fakeLensApi = object : LensApi {
        override suspend fun getAppliances(): Response<List<Appliance>> = Response.success(emptyList())
        override suspend fun getAppliance(id: String): Response<Appliance> = throw NotImplementedError()
        override suspend fun createAppliance(request: ApplianceCreateRequest): Response<Appliance> = throw NotImplementedError()
        override suspend fun updateAppliance(id: String, request: ApplianceCreateRequest): Response<Appliance> = throw NotImplementedError()
        override suspend fun deleteAppliance(id: String): Response<Unit> = throw NotImplementedError()
        override suspend fun analyzeImage(image: MultipartBody.Part, source: RequestBody): Response<DetectionResult> = throw NotImplementedError()
    }

    private val fakeLensRepository = LensRepository(fakeLensApi)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Dispatchers.setMain(testDispatcher)
        MeterReadingCache.clear(context)
        MeterReadingCache.init(context)
        TnebBillCache.clear(context)
        TnebBillCache.init(context)
    }

    @After
    fun tearDown() {
        MeterReadingCache.clear(context)
        TnebBillCache.clear(context)
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            lensRepository = fakeLensRepository,
            ioDispatcher = testDispatcher
        )
    }

    /**
     * TEST 1 — Edit today's reading
     * Previous valid: 42,100
     * Today's reading: 42,250
     * User edits today's reading to: 42,220
     * Expected: Accepted. No correction/reason dialog for today's reading.
     */
    @Test
    fun test1_editTodaysReadingDirectlyWithoutDialog() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-01")
        MeterReadingCache.saveReading(42100.0, "2026-09-15")

        val todayIso = MeterReadingCache.getTodayIsoDate()
        val todayRes = MeterReadingCache.saveReading(42250.0, todayIso)
        assertTrue(todayRes is MeterValidationResult.Success)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // User initiates edit on today's reading
        viewModel.onInitiateEditReading(todayIso)
        advanceUntilIdle()

        // Verify: No confirmation dialog is shown for today's reading!
        val state = viewModel.uiState.value
        assertNull(state.historicalEditConfirmDialog)
        assertTrue(state.isEditingReading)
        assertEquals("42250", state.meterInputText)

        // User changes reading to 42,220 and saves
        viewModel.onMeterInputChanged("42220")
        viewModel.updateTodayMeterReading()
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertFalse(updatedState.isEditingReading)
        assertNull(updatedState.meterValidationError)
        assertEquals(42220.0, MeterReadingCache.metrics.value?.currentReading ?: 0.0, 0.001)
        assertEquals(42220.0, updatedState.currentEnergyMetrics?.currentReading ?: 0.0, 0.001)
        assertEquals(120.0, MeterReadingCache.readings.value.last().dailyConsumption ?: 0.0, 0.001)
    }

    /**
     * TEST 2 — Edit previous date
     * Sep 29: 42,100
     * Sep 30: 42,150
     * Oct 1:  42,200
     * Tap Edit on Sep 30.
     * Expected: "Edit Previous Reading?" dialog appears with reasons and Cancel/Continue.
     */
    @Test
    fun test2_editPreviousDateReadingShowsConfirmationDialog() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-28")
        MeterReadingCache.saveReading(42100.0, "2026-09-29")
        MeterReadingCache.saveReading(42150.0, "2026-09-30")
        MeterReadingCache.saveReading(42200.0, "2026-10-01")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Tap edit on Sep 30 (historical date)
        viewModel.onInitiateEditReading("2026-09-30")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isEditingReading)
        assertNotNull(state.historicalEditConfirmDialog)

        val confirm = state.historicalEditConfirmDialog!!
        assertEquals("2026-09-30", confirm.reading.readingDate)
        assertEquals(42150.0, confirm.reading.readingValue, 0.001)
        assertEquals("Entered by mistake", confirm.selectedReason)
    }

    /**
     * TEST 3 — Valid historical correction
     * Change Sep 30: 42,150 -> 42,180
     * Expected: Accepted.
     * Sequence: 42,100 <= 42,180 <= 42,200.
     */
    @Test
    fun test3_validHistoricalCorrectionAccepted() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-28")
        MeterReadingCache.saveReading(42100.0, "2026-09-29")
        MeterReadingCache.saveReading(42150.0, "2026-09-30")
        MeterReadingCache.saveReading(42200.0, "2026-10-01")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onInitiateEditReading("2026-09-30")
        viewModel.onHistoricalReasonSelected("Reading was recorded incorrectly")
        viewModel.onContinueHistoricalEdit()
        advanceUntilIdle()

        val inputData = viewModel.uiState.value.historicalCorrectionInputDialog
        assertNotNull(inputData)
        assertEquals(42100.0, inputData!!.lowerBound, 0.001)
        assertEquals(42200.0, inputData.upperBound ?: 0.0, 0.001)

        // Enter valid correction: 42,180
        viewModel.onHistoricalCorrectionInputChanged("42180")
        viewModel.saveHistoricalCorrection()
        advanceUntilIdle()

        // Dialog dismissed, reading updated
        assertNull(viewModel.uiState.value.historicalCorrectionInputDialog)

        val readings = MeterReadingCache.readings.value
        val sep30 = readings.first { it.readingDate == "2026-09-30" }
        assertEquals(42180.0, sep30.readingValue, 0.001)
        assertTrue(sep30.isCorrected)
        assertEquals("Reading was recorded incorrectly", sep30.correctionReason)
        assertNotNull(sep30.correctedAt)

        // Check sequential daily consumption deltas
        // Sep 29: 42,100 - 42,000 = 100
        // Sep 30: 42,180 - 42,100 = 80
        // Oct 1:  42,200 - 42,180 = 20
        assertEquals(100.0, readings[0].dailyConsumption ?: 0.0, 0.001)
        assertEquals(80.0, readings[1].dailyConsumption ?: 0.0, 0.001)
        assertEquals(20.0, readings[2].dailyConsumption ?: 0.0, 0.001)
    }

    /**
     * TEST 4 — Invalid historical correction
     * Change Sep 30: 42,150 -> 42,050 (< 42,100) -> Rejected!
     * Change Sep 30: 42,150 -> 42,250 (> 42,200) -> Rejected!
     */
    @Test
    fun test4_invalidHistoricalCorrectionRejected() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-28")
        MeterReadingCache.saveReading(42100.0, "2026-09-29")
        MeterReadingCache.saveReading(42150.0, "2026-09-30")
        MeterReadingCache.saveReading(42200.0, "2026-10-01")

        // 1. Lower than previous valid reading (42,050 < 42,100)
        val resLower = MeterReadingCache.correctHistoricalReading("2026-09-30", 42050.0, "test")
        assertTrue(resLower is MeterValidationResult.Error)
        assertTrue((resLower as MeterValidationResult.Error).message.contains("cannot be lower than the previous recorded reading"))

        // 2. Higher than next valid reading (42,250 > 42,200)
        val resHigher = MeterReadingCache.correctHistoricalReading("2026-09-30", 42250.0, "test")
        assertTrue(resHigher is MeterValidationResult.Error)
        assertTrue((resHigher as MeterValidationResult.Error).message.contains("cannot be higher than the next recorded reading"))

        // Verify Sep 30 reading remains unchanged
        val sep30 = MeterReadingCache.readings.value.first { it.readingDate == "2026-09-30" }
        assertEquals(42150.0, sep30.readingValue, 0.001)
    }

    /**
     * TEST 5 — Accidental high reading recovery detection
     * Oct 1: 42,239 accidentally entered as 422,899
     * Oct 2: user enters 42,260
     * Expected: "Reading Lower Than Previous" dialog appears with Cancel & Correct Previous Reading.
     */
    @Test
    fun test5_accidentalHighReadingRecoveryDetection() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-28")
        // User accidentally enters 422,899 on Oct 1
        MeterReadingCache.saveReading(422899.0, "2026-10-01")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Next day (Oct 2), user enters 42,260
        viewModel.onMeterInputChanged("42260")
        viewModel.saveCurrentMeterReading()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.lowerThanPreviousDialog)
        val lowerDialog = state.lowerThanPreviousDialog!!
        assertEquals(42260.0, lowerDialog.enteredReading, 0.001)
        assertEquals(422899.0, lowerDialog.previousReading.readingValue, 0.001)
        assertEquals("2026-10-01", lowerDialog.previousReading.readingDate)
    }

    /**
     * TEST 6 — Correct accidental high reading flow
     * Tap Correct Previous Reading -> Edit Previous Reading confirmation
     * -> correct 422,899 to 42,239 -> Save
     * Then today's 42,260 should be accepted!
     */
    @Test
    fun test6_correctAccidentalHighReadingRecoveryFlow() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-28")
        MeterReadingCache.saveReading(422899.0, "2026-10-01")

        val viewModel = createViewModel()
        advanceUntilIdle()

        // 1. Enter 42,260 -> triggers Lower Than Previous dialog
        viewModel.onMeterInputChanged("42260")
        viewModel.saveCurrentMeterReading()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.lowerThanPreviousDialog)

        // 2. Tap "Correct Previous Reading"
        viewModel.onConfirmCorrectPreviousReading()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.lowerThanPreviousDialog)
        assertNotNull(viewModel.uiState.value.historicalEditConfirmDialog)
        assertEquals("2026-10-01", viewModel.uiState.value.historicalEditConfirmDialog!!.reading.readingDate)

        // 3. User selects reason and taps Continue
        viewModel.onHistoricalReasonSelected("Reading was recorded incorrectly")
        viewModel.onContinueHistoricalEdit()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.historicalCorrectionInputDialog)

        // 4. Correct 422,899 to 42,239 and save
        viewModel.onHistoricalCorrectionInputChanged("42239")
        viewModel.saveHistoricalCorrection()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.historicalCorrectionInputDialog)
        val oct1 = MeterReadingCache.readings.value.first { it.readingDate == "2026-10-01" }
        assertEquals(42239.0, oct1.readingValue, 0.001)
        assertTrue(oct1.isCorrected)

        // 5. Now entering 42,260 succeeds because 42,260 >= 42,239!
        viewModel.onMeterInputChanged("42260")
        viewModel.saveCurrentMeterReading()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.lowerThanPreviousDialog)
        assertNull(viewModel.uiState.value.meterValidationError)
        val oct2 = MeterReadingCache.readings.value.last()
        assertEquals(42260.0, oct2.readingValue, 0.001)
        assertEquals(21.0, oct2.dailyConsumption ?: 0.0, 0.001)
    }

    /**
     * TEST 7 — Recalculation
     * After correction verify:
     * - Current consumption
     * - Average/day
     * - Estimated bill
     * - Graph points
     * - Daily consumption points
     * All update immediately.
     */
    @Test
    fun test7_recalculationOfAllDependentMetricsImmediately() = runTest(testDispatcher) {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-01")
        MeterReadingCache.saveReading(42100.0, "2026-09-15") // 100 units consumed
        MeterReadingCache.saveReading(42200.0, "2026-09-30") // 200 units consumed

        val viewModel = createViewModel()
        advanceUntilIdle()

        val initialMetrics = viewModel.uiState.value.currentEnergyMetrics
        assertNotNull(initialMetrics)
        assertEquals(200.0, initialMetrics!!.unitsConsumed, 0.001)
        assertEquals(200.0, viewModel.uiState.value.unitsConsumed ?: 0.0, 0.001)

        val initialGraph = viewModel.uiState.value.meterGraphPoints
        assertEquals(3, initialGraph.size) // Baseline + 2 readings
        assertEquals(42200.0, initialGraph.last().readingValue, 0.001)

        val initialDaily = viewModel.uiState.value.dailyConsumptionPoints
        assertEquals(2, initialDaily.size)
        assertEquals(100.0, initialDaily.last().dailyKwh, 0.001)

        // Correct Sep 30 from 42,200 to 42,300
        val correctRes = MeterReadingCache.correctHistoricalReading("2026-09-30", 42300.0, "Calibration")
        assertTrue(correctRes is MeterValidationResult.Success)
        advanceUntilIdle()

        // Verify updated metrics
        val updatedMetrics = viewModel.uiState.value.currentEnergyMetrics
        assertNotNull(updatedMetrics)
        assertEquals(300.0, updatedMetrics!!.unitsConsumed, 0.001)
        assertEquals(300.0, viewModel.uiState.value.unitsConsumed ?: 0.0, 0.001)
        assertTrue(updatedMetrics.estimatedBill > initialMetrics.estimatedBill)

        // Verify updated graph
        val updatedGraph = viewModel.uiState.value.meterGraphPoints
        assertEquals(42300.0, updatedGraph.last().readingValue, 0.001)
        assertEquals(300.0, updatedGraph.last().unitsConsumed, 0.001)

        // Verify updated daily points
        val updatedDaily = viewModel.uiState.value.dailyConsumptionPoints
        assertEquals(200.0, updatedDaily.last().dailyKwh, 0.001) // 42,300 - 42,100 = 200 kWh
    }

    /**
     * TEST 8 — Persistence
     * Correct a reading.
     * Close the application completely.
     * Reopen it.
     * Expected: The corrected reading and its audit metadata remain intact.
     */
    @Test
    fun test8_persistenceAcrossColdRestart() {
        MeterReadingCache.setManualBaseline(42000.0, "2026-09-01")
        MeterReadingCache.saveReading(42100.0, "2026-09-15")
        MeterReadingCache.saveReading(42150.0, "2026-09-30")

        // Perform correction
        val correctRes = MeterReadingCache.correctHistoricalReading("2026-09-30", 42180.0, "Mistake")
        assertTrue(correctRes is MeterValidationResult.Success)

        // Verify in-memory state before cold restart
        assertEquals(42180.0, MeterReadingCache.metrics.value?.currentReading ?: 0.0, 0.001)

        // SIMULATE COLD RESTART (Process Death)
        // Reset in-memory StateFlows and re-init from SharedPreferences
        MeterReadingCache.init(context)

        // Verify restored reading
        val restoredReadings = MeterReadingCache.readings.value
        assertEquals(2, restoredReadings.size)

        val restoredSep30 = restoredReadings.first { it.readingDate == "2026-09-30" }
        assertEquals(42180.0, restoredSep30.readingValue, 0.001)
        assertTrue(restoredSep30.isCorrected)
        assertEquals("Mistake", restoredSep30.correctionReason)
        assertNotNull(restoredSep30.correctedAt)

        val restoredMetrics = MeterReadingCache.metrics.value
        assertNotNull(restoredMetrics)
        assertEquals(42180.0, restoredMetrics!!.currentReading, 0.001)
        assertEquals(180.0, restoredMetrics.unitsConsumed, 0.001)
    }
}
