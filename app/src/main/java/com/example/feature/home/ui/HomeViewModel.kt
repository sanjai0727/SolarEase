package com.example.feature.home.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.RetrofitClient
import com.example.core.session.UserSession
import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.LensRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

import com.example.core.network.MeterApi
import com.example.core.network.dto.MeterReadingCreateDto
import com.example.core.network.dto.MeterReadingUpdateDto
import com.example.feature.home.ui.modules.CurrentCycleEnergyMetrics
import com.example.feature.home.ui.modules.DailyConsumptionPoint
import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterGraphPoint
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.MeterReadingRecord
import com.example.feature.home.ui.modules.MeterValidationResult
import kotlinx.coroutines.flow.combine
import java.util.Locale

/**
 * REAP 4-tier benchmark classification based on bi-monthly electricity consumption.
 */
enum class ReapTier(
    val code: String,
    val title: String,
    val bracket: String,
    val description: String,
    val targetUnits: Double?
) {
    PENDING(
        code = "PENDING",
        title = "Classification Pending",
        bracket = "Connect Bill to Classify",
        description = "Your home will be classified into one of 4 REAP benchmark tiers based on your recorded bi-monthly cycle units.",
        targetUnits = null
    ),
    ECH(
        code = "ECH",
        title = "Energy Conservation Home",
        bracket = "≤ 500 units",
        description = "Energy saver base tier. Low consumption qualifies for subsidized baseline slabs. Focus on maintaining conservation.",
        targetUnits = null
    ),
    EEH(
        code = "EEH",
        title = "Energy Efficiency Home",
        bracket = "501–800 units",
        description = "Targeted optimization tier. Focus on appliance efficiency, usage shifting, and eliminating standby waste before rooftop solar sizing.",
        targetUnits = 500.0
    ),
    EOH(
        code = "EOH",
        title = "Energy Optimisation Home",
        bracket = "801–1000 units",
        description = "Load balancing ready. Multi-room appliance loads benefit from high-efficiency inverters and scheduled operation.",
        targetUnits = 800.0
    ),
    ETH(
        code = "ETH",
        title = "Energy Transition Home",
        bracket = "> 1,000 units",
        description = "High-consumption household with significant load flexibility. Highest priority for deep efficiency, 5–8 kW rooftop solar, and BESS feasibility.",
        targetUnits = 1000.0
    )
}

/**
 * Real timeline events for the Recent Activity log.
 */
data class HomeActivityEvent(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val metricLabel: String? = null,
    val isPositive: Boolean = true
)

/**
 * Complete, immutable Home Screen state containing real aggregated data.
 */
data class HomeUiState(
    val isRefreshing: Boolean = false,
    val userName: String? = null,
    val userInitial: Char = 'S',
    val userAddress: String? = null,
    val greeting: String = "Good morning",
    val hasConnectedBill: Boolean = false,
    val billData: TnebConsumerBillData? = null,
    val unitsConsumed: Double? = null,
    val amountDue: String? = null,
    val dueDate: String? = null,
    val isPaid: Boolean = false,
    val billingPeriod: String? = null,
    val tariffCategory: String = "TANGEDCO LT-1A · DOMESTIC",
    val reapTier: ReapTier = ReapTier.PENDING,
    val activeJourneyStage: Int = 1,
    val appliances: List<Appliance> = emptyList(),
    val isLensLoading: Boolean = false,
    val recommendedSolarCapacityKw: Double? = null,
    val estimatedSolarSavingsPerCycle: Double? = null,
    val recentActivities: List<HomeActivityEvent> = emptyList(),

    // --- NEW HOME ENERGY / CURRENT CYCLE TRACKING FIELDS ---
    val meterBaseline: MeterCycleBaseline? = null,
    val currentEnergyMetrics: CurrentCycleEnergyMetrics? = null,
    val meterGraphPoints: List<MeterGraphPoint> = emptyList(),
    val dailyConsumptionPoints: List<DailyConsumptionPoint> = emptyList(),
    val hasCurrentReading: Boolean = false,
    val isTodayReadingEntered: Boolean = false,
    val todayEnteredReading: Double? = null,
    val meterInputText: String = "",
    val meterValidationError: String? = null,
    val isSavingReading: Boolean = false,
    val isEditingReading: Boolean = false
) {
    val totalConnectedLoadWatts: Float
        get() = appliances.sumOf { (it.powerRatingWatts * it.quantity).toDouble() }.toFloat()

    val totalConnectedLoadKw: Float
        get() = totalConnectedLoadWatts / 1000f

    val hasCompletedAudit: Boolean
        get() = appliances.isNotEmpty()

    val currentBillingCycleKwh: Double
        get() = if (hasCurrentReading && currentEnergyMetrics != null) {
            currentEnergyMetrics.unitsConsumed
        } else {
            unitsConsumed ?: 0.0
        }
}

class HomeViewModel(
    private val lensRepository: LensRepository = LensRepository(RetrofitClient.lensApi),
    private val meterApi: MeterApi = RetrofitClient.meterApi,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildInitialUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Synchronize baseline immediately if bill data is already restored
        TnebBillCache.billData.value?.let { bill ->
            MeterReadingCache.syncBaselineFromTnebBill(bill)
        }
        loadUserProfile()
        observeBillData()
        observeMeterData()
        loadAppliances()
    }

    private fun buildInitialUiState(): HomeUiState {
        val currentUser = UserSession.currentUser
        val displayName = currentUser?.name ?: currentUser?.username
        val firstName = displayName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        val initial = firstName?.firstOrNull()?.uppercaseChar() ?: 'S'

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingText = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val address = currentUser?.houseAddress?.takeIf { it.isNotBlank() }
        val billData = TnebBillCache.billData.value
        val baseline = MeterReadingCache.baseline.value
        val metrics = MeterReadingCache.metrics.value
        val readings = MeterReadingCache.readings.value
        val graphPts = MeterReadingCache.getGraphPoints()
        val hasReading = readings.isNotEmpty()

        val hasBill = (billData != null && (billData.bills.isNotEmpty() || billData.consumerNumber.isNotBlank()))
                || (baseline != null && baseline.serviceConnectionNumber?.isNotBlank() == true)
                || (!currentUser?.ebServiceNumber.isNullOrBlank())

        val latest = billData?.latestBill
        val parsedUnits = latest?.units?.toDoubleOrNull()
            ?: latest?.kwh?.toDoubleOrNull()
            ?: baseline?.previousBilledUnits

        val amount = when {
            billData?.isDuesNil == true -> "₹0.00"
            latest != null && latest.isPaid -> "₹0.00"
            latest != null && latest.amountDue.isNotBlank() -> {
                val raw = latest.amountDue.trim()
                if (raw.startsWith("₹")) raw else "₹$raw"
            }
            else -> "₹0.00"
        }

        val isPaid = billData?.isDuesNil == true || (latest?.isPaid == true)
        val period = latest?.period?.takeIf { it.isNotBlank() }
            ?: latest?.assessmentDate?.takeIf { it.isNotBlank() }
            ?: baseline?.baselineDate

        val tariff = billData?.tariffCode?.takeIf { it.isNotBlank() }
            ?: billData?.serviceCategory?.takeIf { it.isNotBlank() }
            ?: baseline?.tariffCode
            ?: "TANGEDCO LT-1A · DOMESTIC"

        val dailyPts = MeterReadingCache.getDailyConsumptionPoints()
        val activeUnits = if (hasReading && metrics != null) metrics.unitsConsumed else parsedUnits

        val tier = when {
            activeUnits == null -> ReapTier.PENDING
            activeUnits <= 500.0 -> ReapTier.ECH
            activeUnits <= 800.0 -> ReapTier.EEH
            activeUnits <= 1000.0 -> ReapTier.EOH
            else -> ReapTier.ETH
        }

        val recommendedSolarKw = if (activeUnits != null && activeUnits > 0) {
            EstimateCalculator.calculateRecommendedSystemSize(activeUnits / 2.0)
        } else null

        val estimatedCycleSavings = if (activeUnits != null && activeUnits > 0) {
            (activeUnits * 0.8 * 4.5).coerceAtLeast(0.0)
        } else null

        return HomeUiState(
            userName = firstName,
            userInitial = initial,
            userAddress = address,
            greeting = greetingText,
            hasConnectedBill = hasBill,
            billData = billData,
            unitsConsumed = activeUnits,
            amountDue = amount,
            dueDate = latest?.dueDate?.takeIf { it.isNotBlank() },
            isPaid = isPaid,
            billingPeriod = period,
            tariffCategory = tariff,
            reapTier = tier,
            activeJourneyStage = if (hasBill) 2 else 1,
            recommendedSolarCapacityKw = recommendedSolarKw,
            estimatedSolarSavingsPerCycle = estimatedCycleSavings,
            meterBaseline = baseline,
            currentEnergyMetrics = metrics,
            meterGraphPoints = graphPts,
            dailyConsumptionPoints = dailyPts,
            hasCurrentReading = hasReading,
            isTodayReadingEntered = metrics?.isTodayLogged ?: false,
            todayEnteredReading = metrics?.todayReading
        )
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadUserProfile()
            loadAppliances()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadUserProfile() {
        val currentUser = UserSession.currentUser
        val displayName = currentUser?.name ?: currentUser?.username
        val firstName = displayName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
        val initial = firstName?.firstOrNull()?.uppercaseChar() ?: 'S'

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingText = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }

        val address = currentUser?.houseAddress?.takeIf { it.isNotBlank() }

        _uiState.update { current ->
            current.copy(
                userName = firstName,
                userInitial = initial,
                userAddress = address,
                greeting = greetingText
            )
        }
    }

    private fun observeBillData() {
        viewModelScope.launch {
            TnebBillCache.billData.collect { billData ->
                processBillData(billData)
            }
        }
    }

    private fun observeMeterData() {
        viewModelScope.launch {
            combine(
                MeterReadingCache.baseline,
                MeterReadingCache.metrics,
                MeterReadingCache.readings
            ) { baseline, metrics, readings ->
                Triple(baseline, metrics, readings)
            }.collect { (baseline, metrics, readings) ->
                val hasReading = readings.isNotEmpty()
                val graphPts = MeterReadingCache.getGraphPoints()
                val dailyPts = MeterReadingCache.getDailyConsumptionPoints()
                _uiState.update { current ->
                    val activeUnits = if (hasReading && metrics != null) metrics.unitsConsumed else current.unitsConsumed
                    val tier = when {
                        activeUnits == null -> ReapTier.PENDING
                        activeUnits <= 500.0 -> ReapTier.ECH
                        activeUnits <= 800.0 -> ReapTier.EEH
                        activeUnits <= 1000.0 -> ReapTier.EOH
                        else -> ReapTier.ETH
                    }
                    val recKw = if (activeUnits != null && activeUnits > 0) {
                        EstimateCalculator.calculateRecommendedSystemSize(activeUnits / 2.0)
                    } else null
                    val estSavings = if (activeUnits != null && activeUnits > 0) {
                        (activeUnits * 0.8 * 4.5).coerceAtLeast(0.0)
                    } else null

                    current.copy(
                        meterBaseline = baseline,
                        currentEnergyMetrics = metrics,
                        meterGraphPoints = graphPts,
                        dailyConsumptionPoints = dailyPts,
                        hasCurrentReading = hasReading,
                        isTodayReadingEntered = metrics?.isTodayLogged ?: false,
                        todayEnteredReading = metrics?.todayReading,
                        unitsConsumed = activeUnits,
                        reapTier = tier,
                        recommendedSolarCapacityKw = recKw,
                        estimatedSolarSavingsPerCycle = estSavings
                    )
                }
            }
        }
    }

    private fun processBillData(billData: TnebConsumerBillData?) {
        if (billData == null || (billData.bills.isEmpty() && billData.consumerNumber.isBlank())) {
            val baseline = MeterReadingCache.baseline.value
            val user = UserSession.currentUser
            if ((baseline != null && baseline.serviceConnectionNumber?.isNotBlank() == true) || !user?.ebServiceNumber.isNullOrBlank()) {
                _uiState.update { current ->
                    current.copy(
                        hasConnectedBill = true,
                        meterBaseline = baseline
                    )
                }
                return
            }

            _uiState.update { current ->
                current.copy(
                    hasConnectedBill = false,
                    billData = null,
                    unitsConsumed = null,
                    amountDue = null,
                    dueDate = null,
                    isPaid = false,
                    billingPeriod = null,
                    reapTier = ReapTier.PENDING,
                    activeJourneyStage = if (current.hasCompletedAudit) 2 else 1,
                    recommendedSolarCapacityKw = null,
                    estimatedSolarSavingsPerCycle = null,
                    recentActivities = buildActivityList(null, current.appliances)
                )
            }
            return
        }

        // Establish the baseline starting point for the current billing cycle
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        val latest = billData.latestBill
        val parsedUnits = latest?.units?.toDoubleOrNull()
            ?: latest?.kwh?.toDoubleOrNull()

        val amount = when {
            billData.isDuesNil -> "₹0.00"
            latest != null && latest.isPaid -> "₹0.00"
            latest != null && latest.amountDue.isNotBlank() -> {
                val raw = latest.amountDue.trim()
                if (raw.startsWith("₹")) raw else "₹$raw"
            }
            else -> "₹0.00"
        }

        val isPaid = billData.isDuesNil || (latest?.isPaid == true)
        val period = latest?.period?.takeIf { it.isNotBlank() }
            ?: latest?.assessmentDate?.takeIf { it.isNotBlank() }

        val tariff = billData.tariffCode.takeIf { it.isNotBlank() }
            ?: billData.serviceCategory.takeIf { it.isNotBlank() }
            ?: "TANGEDCO LT-1A · DOMESTIC"

        // REAP Classification
        val tier = when {
            parsedUnits == null -> ReapTier.PENDING
            parsedUnits <= 500.0 -> ReapTier.ECH
            parsedUnits <= 800.0 -> ReapTier.EEH
            parsedUnits <= 1000.0 -> ReapTier.EOH
            else -> ReapTier.ETH
        }

        // Real Solar Sizing based on real units
        val recommendedSolarKw = if (parsedUnits != null && parsedUnits > 0) {
            // Bi-monthly units to monthly: units / 2.0
            val monthlyKwh = parsedUnits / 2.0
            EstimateCalculator.calculateRecommendedSystemSize(monthlyKwh)
        } else null

        val estimatedCycleSavings = if (parsedUnits != null && parsedUnits > 0) {
            // Rough savings estimate: ~75% - 85% of grid charges
            val avgRate = 4.5
            (parsedUnits * 0.8 * avgRate).coerceAtLeast(0.0)
        } else null

        val currentAppliances = _uiState.value.appliances
        val journeyStage = when {
            currentAppliances.isNotEmpty() -> 3 // Optimise
            parsedUnits != null -> 2 // Reduce
            else -> 1 // Measure
        }

        _uiState.update { current ->
            current.copy(
                hasConnectedBill = true,
                billData = billData,
                unitsConsumed = parsedUnits,
                amountDue = amount,
                dueDate = latest?.dueDate?.takeIf { it.isNotBlank() },
                isPaid = isPaid,
                billingPeriod = period,
                tariffCategory = tariff,
                reapTier = tier,
                activeJourneyStage = journeyStage,
                recommendedSolarCapacityKw = recommendedSolarKw,
                estimatedSolarSavingsPerCycle = estimatedCycleSavings,
                recentActivities = buildActivityList(billData, currentAppliances)
            )
        }
    }

    private fun loadAppliances() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLensLoading = true) }
            val result = withContext(ioDispatcher) {
                lensRepository.getAppliances()
            }
            result.onSuccess { applianceList ->
                _uiState.update { current ->
                    val journeyStage = when {
                        applianceList.isNotEmpty() -> 3
                        current.hasConnectedBill -> 2
                        else -> 1
                    }
                    current.copy(
                        appliances = applianceList,
                        isLensLoading = false,
                        activeJourneyStage = journeyStage,
                        recentActivities = buildActivityList(current.billData, applianceList)
                    )
                }
            }.onFailure { e ->
                Log.w("HomeViewModel", "Could not fetch appliances from server (working offline/graceful)", e)
                _uiState.update { it.copy(isLensLoading = false) }
            }
        }
    }

    private fun buildActivityList(
        billData: TnebConsumerBillData?,
        appliances: List<Appliance>
    ): List<HomeActivityEvent> {
        val list = mutableListOf<HomeActivityEvent>()

        if (billData != null && billData.latestBill != null) {
            val bill = billData.latestBill!!
            val units = bill.units.takeIf { it.isNotBlank() } ?: bill.kwh
            val amt = billData.totalDuesFormatted
            list.add(
                HomeActivityEvent(
                    id = "bill_${bill.id.ifBlank { "latest" }}",
                    title = if (bill.isPaid) "TNEB Bill Paid • Nil Dues" else "TNEB Bill retrieved",
                    subtitle = "Consumer #${billData.consumerNumber.ifBlank { billData.serviceNumber }}",
                    timeLabel = bill.assessmentDate.ifBlank { "Latest cycle" },
                    metricLabel = if (units.isNotBlank()) "$units kWh" else amt,
                    isPositive = true
                )
            )
        }

        if (appliances.isNotEmpty()) {
            val totalKw = appliances.sumOf { (it.powerRatingWatts * it.quantity).toDouble() } / 1000.0
            list.add(
                HomeActivityEvent(
                    id = "lens_audit",
                    title = "Appliance Audit logged",
                    subtitle = "${appliances.size} active loads profiled across home",
                    timeLabel = "Solar Lens",
                    metricLabel = String.format("%.2f kW", totalKw),
                    isPositive = true
                )
            )
        }

        val user = UserSession.currentUser
        if (user != null && !user.houseAddress.isNullOrBlank()) {
            list.add(
                HomeActivityEvent(
                    id = "profile_service",
                    title = "Service connection verified",
                    subtitle = user.houseAddress,
                    timeLabel = "Verified",
                    metricLabel = null,
                    isPositive = true
                )
            )
        }

        return list
    }

    fun onMeterInputChanged(text: String) {
        val sanitized = text.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _uiState.update { it.copy(meterInputText = sanitized, meterValidationError = null) }
        }
    }

    fun saveCurrentMeterReading() {
        val inputText = _uiState.value.meterInputText.trim()
        val readingVal = inputText.toDoubleOrNull()
        if (readingVal == null) {
            _uiState.update { it.copy(meterValidationError = "Please enter a valid numeric reading.") }
            return
        }

        val result = MeterReadingCache.saveReading(readingVal)
        when (result) {
            is MeterValidationResult.Success -> {
                _uiState.update { current ->
                    current.copy(
                        meterInputText = "",
                        meterValidationError = null,
                        isSavingReading = false,
                        isEditingReading = false
                    )
                }

                // Asynchronously sync to backend if user is authenticated
                viewModelScope.launch(ioDispatcher) {
                    try {
                        val token = UserSession.accessToken
                        if (!token.isNullOrBlank()) {
                            meterApi.submitReading(
                                MeterReadingCreateDto(
                                    readingValue = readingVal,
                                    readingDate = result.reading.readingDate
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("HomeViewModel", "Could not sync meter reading to backend (offline mode)", e)
                    }
                }
            }
            is MeterValidationResult.DuplicateToday -> {
                _uiState.update {
                    it.copy(
                        meterValidationError = "Today's reading (${String.format(Locale.getDefault(), "%,.0f", result.existingReading.readingValue)} kWh) has already been saved.",
                        isSavingReading = false
                    )
                }
            }
            is MeterValidationResult.Error -> {
                _uiState.update {
                    it.copy(
                        meterValidationError = result.message,
                        isSavingReading = false
                    )
                }
            }
        }
    }

    fun updateTodayMeterReading() {
        val inputText = _uiState.value.meterInputText.trim()
        val readingVal = inputText.toDoubleOrNull()
        if (readingVal == null) {
            _uiState.update { it.copy(meterValidationError = "Please enter a valid numeric reading.") }
            return
        }

        val result = MeterReadingCache.updateTodayReading(readingVal)
        when (result) {
            is MeterValidationResult.Success -> {
                _uiState.update {
                    it.copy(
                        meterInputText = "",
                        meterValidationError = null,
                        isEditingReading = false
                    )
                }

                viewModelScope.launch(ioDispatcher) {
                    try {
                        val token = UserSession.accessToken
                        if (!token.isNullOrBlank()) {
                            meterApi.updateTodayReading(
                                MeterReadingUpdateDto(readingValue = readingVal)
                            )
                        }
                    } catch (e: Exception) {
                        Log.w("HomeViewModel", "Could not sync updated reading to backend", e)
                    }
                }
            }
            is MeterValidationResult.Error -> {
                _uiState.update { it.copy(meterValidationError = result.message) }
            }
            is MeterValidationResult.DuplicateToday -> {}
        }
    }

    fun startEditingReading() {
        val currentToday = _uiState.value.todayEnteredReading
        _uiState.update {
            it.copy(
                isEditingReading = true,
                meterInputText = if (currentToday != null) {
                    if (currentToday % 1.0 == 0.0) currentToday.toInt().toString() else currentToday.toString()
                } else "",
                meterValidationError = null
            )
        }
    }

    fun cancelEditingReading() {
        _uiState.update {
            it.copy(
                isEditingReading = false,
                meterInputText = "",
                meterValidationError = null
            )
        }
    }
}
