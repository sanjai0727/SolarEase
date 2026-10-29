package com.example.feature.meterreading

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.meterreading.detector.MeterOcrDetector
import com.example.feature.meterreading.model.MeterOcrStatus
import com.example.feature.meterreading.model.MeterReadingCandidate
import com.example.feature.meterreading.model.MeterScanResult
import com.example.feature.meterreading.ui.MeterScannerUiState
import com.example.feature.meterreading.ui.MeterScannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MeterScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeMeterOcrDetector(var resultCandidate: MeterReadingCandidate) : MeterOcrDetector {
        override suspend fun recognizeText(bitmap: Bitmap, baselineKwh: Double?): MeterScanResult {
            return MeterScanResult(
                candidate = resultCandidate,
                croppedBitmap = bitmap,
                fullRawText = resultCandidate.rawText
            )
        }
        override fun close() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MeterReadingCache.clear(null)
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            consumerName = "Sanjai",
            tariffCode = "TANGEDCO LT-1A · DOMESTIC",
            isDuesNil = true,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-98765",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    units = "302"
                )
            )
        )
        MeterReadingCache.syncBaselineFromTnebBill(billData)
    }

    @After
    fun tearDown() {
        MeterReadingCache.clear(null)
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isReady() {
        val viewModel = MeterScannerViewModel(FakeMeterOcrDetector(
            MeterReadingCandidate(
                rawText = "12500 kWh",
                numericValue = 12500.0,
                displayValue = "12500"
            )
        ))

        assertTrue(viewModel.uiState.value is MeterScannerUiState.Ready)
    }

    @Test
    fun onEditableTextChanged_updatesTextAndValidates() {
        val fakeBitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(FakeMeterOcrDetector(
            MeterReadingCandidate(
                rawText = "12500 kWh",
                numericValue = 12500.0,
                displayValue = "12500"
            )
        ))

        val method = viewModel.javaClass.getDeclaredField("_uiState")
        method.isAccessible = true
        val stateFlow = method.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<MeterScannerUiState>
        stateFlow.value = MeterScannerUiState.Review(
            croppedBitmap = fakeBitmap,
            candidate = MeterReadingCandidate("12500", 12500.0, "12500"),
            editableText = "12500"
        )

        viewModel.onEditableTextChanged("12550")

        val state = viewModel.uiState.value as MeterScannerUiState.Review
        assertEquals("12550", state.editableText)
    }

    @Test
    fun cropAdjustment_flow_allowsUserToAdjustFrameAndReRunOcr() = runTest(testDispatcher) {
        val fullFakeBitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(
            ocrDetector = FakeMeterOcrDetector(
                MeterReadingCandidate(
                    rawText = "16596.7 kWh",
                    numericValue = 16596.7,
                    displayValue = "16596.7",
                    hasDecimal = true
                )
            ),
            ioDispatcher = testDispatcher
        )

        // Inject fullCapturedBitmap
        val fullBmpField = viewModel.javaClass.getDeclaredField("fullCapturedBitmap")
        fullBmpField.isAccessible = true
        fullBmpField.set(viewModel, fullFakeBitmap)

        // Start crop adjustment
        viewModel.startCropAdjustment()

        val adjustState = viewModel.uiState.value
        assertTrue("State should transition to AdjustCrop", adjustState is MeterScannerUiState.AdjustCrop)
        assertEquals(fullFakeBitmap, (adjustState as MeterScannerUiState.AdjustCrop).fullBitmap)

        // User adjusts crop rectangle directly over LCD display
        val userSelectedRect = RectF(0.20f, 0.25f, 0.80f, 0.45f)
        viewModel.applyAdjustedCrop(userSelectedRect)
        advanceUntilIdle()

        val reviewState = viewModel.uiState.value
        assertTrue("State should return to Review after applying crop", reviewState is MeterScannerUiState.Review)
        assertEquals("16596.7", (reviewState as MeterScannerUiState.Review).editableText)
        assertEquals(16596.7, reviewState.candidate.numericValue, 0.001)
    }

    @Test
    fun confirmReading_decimalValue_persistsWithoutTruncationOrRounding() = runTest(testDispatcher) {
        val fakeBitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(
            ocrDetector = FakeMeterOcrDetector(
                MeterReadingCandidate(
                    rawText = "16596.7 kWh",
                    numericValue = 16596.7,
                    displayValue = "16596.7",
                    hasDecimal = true
                )
            ),
            ioDispatcher = testDispatcher
        )

        val method = viewModel.javaClass.getDeclaredField("_uiState")
        method.isAccessible = true
        val stateFlow = method.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<MeterScannerUiState>
        stateFlow.value = MeterScannerUiState.Review(
            croppedBitmap = fakeBitmap,
            candidate = MeterReadingCandidate("16596.7", 16596.7, "16596.7", hasDecimal = true),
            editableText = "16596.7"
        )

        var confirmedValue: Double? = null
        viewModel.confirmReading { confirmed ->
            confirmedValue = confirmed
        }

        advanceUntilIdle()

        // Verify state is Success
        val state = viewModel.uiState.value
        assertTrue(state is MeterScannerUiState.Success)
        assertEquals(16596.7, (state as MeterScannerUiState.Success).savedReadingValue, 0.001)
        assertEquals(16596.7, confirmedValue ?: 0.0, 0.001)

        // Verify MeterReadingCache stored exact decimal value 16596.7!
        val lastReading = MeterReadingCache.getLastReading()
        assertNotNull(lastReading)
        assertEquals(16596.7, lastReading!!.readingValue, 0.001)
    }

    @Test
    fun confirmReading_persistsToMeterReadingCache_andTriggersSuccess() = runTest(testDispatcher) {
        val fakeBitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(
            ocrDetector = FakeMeterOcrDetector(
                MeterReadingCandidate(
                    rawText = "12582 kWh",
                    numericValue = 12582.0,
                    displayValue = "12582"
                )
            ),
            ioDispatcher = testDispatcher
        )

        val method = viewModel.javaClass.getDeclaredField("_uiState")
        method.isAccessible = true
        val stateFlow = method.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<MeterScannerUiState>
        stateFlow.value = MeterScannerUiState.Review(
            croppedBitmap = fakeBitmap,
            candidate = MeterReadingCandidate("12582", 12582.0, "12582"),
            editableText = "12582"
        )

        var confirmedValue: Double? = null
        viewModel.confirmReading { confirmed ->
            confirmedValue = confirmed
        }

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MeterScannerUiState.Success)
        assertEquals(12582.0, (state as MeterScannerUiState.Success).savedReadingValue, 0.001)
        assertEquals(12582.0, confirmedValue ?: 0.0, 0.001)

        val lastReading = MeterReadingCache.getLastReading()
        assertNotNull(lastReading)
        assertEquals(12582.0, lastReading!!.readingValue, 0.001)

        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(132.0, metrics!!.unitsConsumed, 0.001)
        assertTrue(metrics.isTodayLogged)
    }

    @Test
    fun confirmReading_lowerThanPrevious_showsValidationError() = runTest(testDispatcher) {
        val fakeBitmap = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(
            ocrDetector = FakeMeterOcrDetector(
                MeterReadingCandidate(
                    rawText = "10000 kWh",
                    numericValue = 10000.0,
                    displayValue = "10000"
                )
            ),
            ioDispatcher = testDispatcher
        )

        val method = viewModel.javaClass.getDeclaredField("_uiState")
        method.isAccessible = true
        val stateFlow = method.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<MeterScannerUiState>
        stateFlow.value = MeterScannerUiState.Review(
            croppedBitmap = fakeBitmap,
            candidate = MeterReadingCandidate("10000", 10000.0, "10000"),
            editableText = "10000" // Lower than baseline 12450.0!
        )

        viewModel.confirmReading {}
        advanceUntilIdle()

        val state = viewModel.uiState.value as MeterScannerUiState.Review
        assertNotNull(state.validationError)
        assertTrue(state.validationError!!.contains("cannot be lower", ignoreCase = true))
    }

    @Test
    fun processBitmapForTest_mapsViewFinderToImageCropAccurately() = runTest(testDispatcher) {
        val testBitmap = Bitmap.createBitmap(3000, 4000, Bitmap.Config.ARGB_8888)
        val viewModel = MeterScannerViewModel(
            ocrDetector = FakeMeterOcrDetector(
                MeterReadingCandidate(
                    rawText = "16596.7 kWh",
                    numericValue = 16596.7,
                    displayValue = "16596.7",
                    hasDecimal = true
                )
            ),
            ioDispatcher = testDispatcher
        )

        // Viewfinder reticle in 1080x2400 view
        val viewFinder = RectF(0.08f, 0.35f, 0.92f, 0.55f)
        viewModel.processBitmapForTest(
            bitmap = testBitmap,
            viewFinderRectNormalized = viewFinder,
            viewWidthPx = 1080f,
            viewHeightPx = 2400f
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should transition to Review", state is MeterScannerUiState.Review)
        val review = state as MeterScannerUiState.Review

        // The mapped crop should be centered horizontally [0.248, 0.752] and [0.35, 0.55] vertically
        assertEquals(0.248f, review.cropRectNormalized.left, 0.005f)
        assertEquals(0.350f, review.cropRectNormalized.top, 0.005f)
        assertEquals(0.752f, review.cropRectNormalized.right, 0.005f)
        assertEquals(0.550f, review.cropRectNormalized.bottom, 0.005f)

        // Ensure the cropped bitmap matches the crop rectangle dimensions:
        // width = 3000 * (0.752 - 0.248) = ~1512 px
        // height = 4000 * (0.55 - 0.35) = ~800 px
        assertTrue(review.croppedBitmap.width in 1500..1520)
        assertTrue(review.croppedBitmap.height in 790..810)
    }
}
