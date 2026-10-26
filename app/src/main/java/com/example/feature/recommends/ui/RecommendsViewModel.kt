package com.example.feature.recommends.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.*
import com.example.feature.recommends.data.BillVerificationEngine
import com.example.feature.recommends.data.MantraPreferences
import com.example.feature.recommends.data.RewardStorage
import com.example.feature.recommends.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max

class RecommendsViewModel : ViewModel() {

    private val _selectedCycleIndex = MutableStateFlow<Int?>(null)
    val selectedCycleIndex: StateFlow<Int?> = _selectedCycleIndex.asStateFlow()

    private val _uiState = MutableStateFlow(RecommendsUiState())
    val uiState: StateFlow<RecommendsUiState> = _uiState.asStateFlow()

    private fun getUserId(): String {
        val user = UserSession.currentUser
        return user?.id?.takeIf { it.isNotBlank() }
            ?: user?.username?.takeIf { it.isNotBlank() }
            ?: "default_user"
    }

    init {
        observeData()
    }

    fun initPreferences(context: Context) {
        MantraPreferences.init(context)
        MantraPreferences.checkDateAndResetIfNewDay(context)
        RewardStorage.init(context)
    }

    fun selectPreviousCycle() {
        val currentIndex = _uiState.value.selectedCycleIndex
        if (currentIndex > 0) {
            _selectedCycleIndex.value = currentIndex - 1
        }
    }

    fun selectNextCycle() {
        val currentIndex = _uiState.value.selectedCycleIndex
        val maxIndex = _uiState.value.availableCycles.lastIndex
        if (currentIndex < maxIndex) {
            _selectedCycleIndex.value = currentIndex + 1
        }
    }

    fun selectCycle(index: Int) {
        val maxIndex = _uiState.value.availableCycles.lastIndex
        if (index in 0..maxIndex) {
            _selectedCycleIndex.value = index
        }
    }

    private fun observeData() {
        val userId = getUserId()

        val cyclesFlow = combine(
            TnebBillCache.billData,
            MeterReadingCache.baseline,
            MeterReadingCache.readings,
            MeterReadingCache.metrics
        ) { billData, baseline, readings, metrics ->
            // Trigger automatic bill consumption verification when bill data is available
            if (billData != null && billData.bills.isNotEmpty()) {
                BillVerificationEngine.evaluateBills(userId, billData, RewardStorage)
            }
            BillingCycleManager.buildChronologicalCycles(billData, baseline, readings, metrics)
        }

        val rewardFlow = RewardStorage.getRewardFlow(userId)

        viewModelScope.launch {
            combine(
                cyclesFlow,
                MantraPreferences.mantrasState,
                MantraPreferences.mantrasExpandedState,
                rewardFlow,
                _selectedCycleIndex
            ) { cycles, mantras, isMantrasExpanded, rewardData, selectedIndex ->
                buildUiState(
                    cycles = cycles,
                    mantras = mantras,
                    isMantrasExpanded = isMantrasExpanded,
                    rewardData = rewardData,
                    explicitIndex = selectedIndex
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleMantraCheckIn(context: Context? = null, mantraId: Int) {
        val isChecked = MantraPreferences.toggleCheckIn(context, mantraId)
        if (isChecked) {
            // First daily energy-saving activity awards streak and daily coin idempotently
            RewardStorage.recordDailyActivity(
                userId = getUserId(),
                activityId = "mantra_$mantraId"
            )
        }
    }

    fun toggleMantrasExpanded(context: Context? = null) {
        MantraPreferences.toggleMantrasExpanded(context)
    }

    private fun buildUiState(
        cycles: List<BillingCycleItem>,
        mantras: List<MantraItem>,
        isMantrasExpanded: Boolean,
        rewardData: UserRewardData,
        explicitIndex: Int?
    ): RecommendsUiState {
        val user = UserSession.currentUser
        val userAddress = user?.houseAddress?.takeIf { it.isNotBlank() } ?: "Valmiki Nagar, Chennai"

        val defaultIndex = if (cycles.isNotEmpty()) {
            val lastCycle = cycles.last()
            if (lastCycle.status == BillingCycleStatus.CURRENT && !lastCycle.hasMeterReadings && cycles.size >= 2) {
                cycles.size - 2
            } else {
                cycles.lastIndex
            }
        } else {
            0
        }

        val effectiveIndex = (explicitIndex ?: defaultIndex).coerceIn(0, max(0, cycles.lastIndex))
        val selectedCycle = cycles.getOrNull(effectiveIndex) ?: cycles.firstOrNull()
        val previousCycle = if (effectiveIndex > 0) cycles[effectiveIndex - 1] else null

        val comparison = if (selectedCycle != null) {
            BillingCycleManager.compareCycles(selectedCycle, previousCycle)
        } else null

        val streakState = rewardData.streakState
        val coinsBalance = rewardData.balance
        val nextTarget = 200
        val toUnlock = max(0, nextTarget - coinsBalance)

        val energyCredits = EnergyCreditsData(
            totalCredits = coinsBalance,
            nextRewardTarget = nextTarget,
            progressCredits = coinsBalance,
            creditsToUnlock = toUnlock
        )

        val checkedCount = mantras.count { it.isCheckedIn }

        val legacySavings = BillingCycleSavings(
            previousUnits = previousCycle?.consumptionKwh ?: 0.0,
            previousPeriod = previousCycle?.periodLabel.orEmpty(),
            previousDays = previousCycle?.durationDays ?: 60,
            previousDailyRate = previousCycle?.dailyRate ?: 0.0,
            currentUnits = selectedCycle?.consumptionKwh ?: 0.0,
            currentPeriod = selectedCycle?.periodLabel.orEmpty(),
            currentDays = selectedCycle?.durationDays ?: 60,
            currentDailyRate = selectedCycle?.dailyRate ?: 0.0,
            savedUnits = comparison?.savedUnits ?: 0.0,
            savedCredits = comparison?.savedCredits ?: 0,
            hasRealComparison = previousCycle != null,
            isConsumptionIncreased = comparison?.isConsumptionIncreased ?: false,
            differenceUnits = comparison?.differenceUnits ?: 0.0
        )

        return RecommendsUiState(
            userAddress = userAddress,
            energyCredits = energyCredits,
            todayProgressChecked = checkedCount,
            todayProgressTotal = 9,
            streakDays = streakState.currentStreak,
            streakState = streakState,
            coinBalance = coinsBalance,
            coinTransactions = rewardData.transactions,
            billVerificationResult = rewardData.latestVerificationResult,
            billingCycleSavings = legacySavings,
            mantras = mantras,
            isMantrasExpanded = isMantrasExpanded,
            availableCycles = cycles,
            selectedCycleIndex = effectiveIndex,
            cycleComparison = comparison
        )
    }
}
