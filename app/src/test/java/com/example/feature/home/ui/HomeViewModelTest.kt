package com.example.feature.home.ui

import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
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
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeLensApi = object : LensApi {
        override suspend fun getAppliances(): Response<List<Appliance>> =
            Response.success(emptyList())

        override suspend fun getAppliance(id: String): Response<Appliance> =
            throw NotImplementedError()

        override suspend fun createAppliance(request: ApplianceCreateRequest): Response<Appliance> =
            throw NotImplementedError()

        override suspend fun updateAppliance(id: String, request: ApplianceCreateRequest): Response<Appliance> =
            throw NotImplementedError()

        override suspend fun deleteAppliance(id: String): Response<Unit> =
            throw NotImplementedError()

        override suspend fun analyzeImage(image: MultipartBody.Part, source: RequestBody): Response<DetectionResult> =
            throw NotImplementedError()
    }

    private val fakeLensRepository = LensRepository(fakeLensApi)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TnebBillCache.clear(null)
    }

    @After
    fun tearDown() {
        TnebBillCache.clear(null)
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            lensRepository = fakeLensRepository,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testInitialStateWithoutBill() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasConnectedBill)
        assertEquals(ReapTier.PENDING, state.reapTier)
        assertNull(state.unitsConsumed)
        assertNull(state.amountDue)
        assertEquals(1, state.activeJourneyStage)
        assertFalse(state.hasCompletedAudit)
    }

    @Test
    fun testReapTierClassification() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // 1. ECH Tier (<= 500 units)
        TnebBillCache.save(
            null,
            TnebConsumerBillData(
                consumerNumber = "09234567890",
                bills = listOf(
                    TnebBillRecord(units = "450", amountDue = "1200", dueDate = "25-Oct-2026")
                )
            )
        )
        advanceUntilIdle()
        assertEquals(ReapTier.ECH, viewModel.uiState.value.reapTier)
        assertEquals(450.0, viewModel.uiState.value.unitsConsumed ?: 0.0, 0.01)
        assertTrue(viewModel.uiState.value.hasConnectedBill)
        assertEquals(2, viewModel.uiState.value.activeJourneyStage)

        // 2. EEH Tier (501..800 units)
        TnebBillCache.save(
            null,
            TnebConsumerBillData(
                consumerNumber = "09234567890",
                bills = listOf(
                    TnebBillRecord(units = "648", amountDue = "2480", dueDate = "28-Sep-2026")
                )
            )
        )
        advanceUntilIdle()
        assertEquals(ReapTier.EEH, viewModel.uiState.value.reapTier)
        assertEquals(648.0, viewModel.uiState.value.unitsConsumed ?: 0.0, 0.01)
        assertNotNull(viewModel.uiState.value.recommendedSolarCapacityKw)

        // 3. EOH Tier (801..1000 units)
        TnebBillCache.save(
            null,
            TnebConsumerBillData(
                consumerNumber = "09234567890",
                bills = listOf(
                    TnebBillRecord(units = "920", amountDue = "4500", dueDate = "15-Oct-2026")
                )
            )
        )
        advanceUntilIdle()
        assertEquals(ReapTier.EOH, viewModel.uiState.value.reapTier)

        // 4. ETH Tier (> 1000 units)
        TnebBillCache.save(
            null,
            TnebConsumerBillData(
                consumerNumber = "09234567890",
                bills = listOf(
                    TnebBillRecord(units = "1240", amountDue = "6180", dueDate = "30-Sep-2026")
                )
            )
        )
        advanceUntilIdle()
        assertEquals(ReapTier.ETH, viewModel.uiState.value.reapTier)
    }

    @Test
    fun testSolarSizingRecommendation() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val billData = TnebConsumerBillData(
            consumerNumber = "09123456789",
            bills = listOf(
                TnebBillRecord(units = "648", amountDue = "2480")
            )
        )
        TnebBillCache.save(null, billData)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val expectedMonthly = 648.0 / 2.0
        val expectedSize = EstimateCalculator.calculateRecommendedSystemSize(expectedMonthly)
        assertEquals(expectedSize, state.recommendedSolarCapacityKw ?: 0.0, 0.01)
        assertNotNull(state.estimatedSolarSavingsPerCycle)
    }
}
