package com.example.feature.recommends.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.*
import com.example.feature.recommends.data.MantraPreferences
import com.example.feature.recommends.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max

class RecommendsViewModel : ViewModel() {

    private val _selectedCycleIndex = MutableStateFlow<Int?>(null)
    val selectedCycleIndex: StateFlow<Int?> = _selectedCycleIndex.asStateFlow()

    private val _uiState = MutableStateFlow(RecommendsUiState())
    val uiState: StateFlow<RecommendsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    fun initPreferences(context: Context) {
        MantraPreferences.init(context)
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
        val cyclesFlow = combine(
            TnebBillCache.billData,
            MeterReadingCache.baseline,
            MeterReadingCache.readings,
            MeterReadingCache.metrics
        ) { billData, baseline, readings, metrics ->
            BillingCycleManager.buildChronologicalCycles(billData, baseline, readings, metrics)
        }

        viewModelScope.launch {
            combine(
                cyclesFlow,
                MantraPreferences.mantrasState,
                MantraPreferences.streakState,
                _selectedCycleIndex
            ) { cycles, mantras, streak, selectedIndex ->
                buildUiState(
                    cycles = cycles,
                    mantras = mantras,
                    streak = streak,
                    explicitIndex = selectedIndex
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleMantraCheckIn(context: Context? = null, mantraId: Int) {
        MantraPreferences.toggleCheckIn(context, mantraId)
    }

    private fun buildUiState(
        cycles: List<BillingCycleItem>,
        mantras: List<MantraItem>,
        streak: Int,
        explicitIndex: Int?
    ): RecommendsUiState {
        val user = UserSession.currentUser
        val userAddress = user?.houseAddress?.takeIf { it.isNotBlank() } ?: "Valmiki Nagar, Chennai"

        // Determine default selection if not explicitly selected:
        // If active cycle has meter readings or it's demo, default to active cycle (cycles.lastIndex).
        // If active cycle has no readings yet but completed bills exist, default to the latest completed bill.
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

        // Bill-verified Energy Credits: ONLY awarded from verified post-onboarding savings.
        // Historical billing data is NEVER backfilled into credit balances.
        val storedCredits = MantraPreferences.getEarnedCredits()
        val verifiedCredits = BillingCycleManager.calculateVerifiedBillCredits(cycles)
        val earnedCredits = storedCredits + verifiedCredits

        val nextTarget = 200
        val progressCredits = earnedCredits
        val toUnlock = max(0, nextTarget - progressCredits)

        val energyCredits = EnergyCreditsData(
            totalCredits = earnedCredits,
            nextRewardTarget = nextTarget,
            progressCredits = progressCredits,
            creditsToUnlock = toUnlock
        )

        val checkedCount = mantras.count { it.isCheckedIn }

        // Backwards-compatible BillingCycleSavings projection
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
            streakDays = streak,
            billingCycleSavings = legacySavings,
            mantras = mantras,
            availableCycles = cycles,
            selectedCycleIndex = effectiveIndex,
            cycleComparison = comparison
        )
    }
}
