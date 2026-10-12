package com.example.feature.home.ui.modules

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.network.dto.BaselineResponseDto
import com.example.core.network.dto.MeterReadingResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

object MeterReadingCache {

    private const val TAG = "MeterReadingCache"
    private const val PREFS_NAME = "SolarEaseMeterPrefs"
    private const val KEY_BASELINE = "active_cycle_baseline"
    private const val KEY_READINGS = "current_cycle_readings"
    private const val KEY_ARCHIVED_CYCLES = "archived_cycle_readings"
    private fun logD(msg: String) { try { Log.d(TAG, msg) } catch (_: Throwable) {} }
    private fun logW(msg: String, tr: Throwable? = null) { try { Log.w(TAG, msg, tr) } catch (_: Throwable) {} }
    private fun logE(msg: String, tr: Throwable? = null) { try { Log.e(TAG, msg, tr) } catch (_: Throwable) {} }

    private var appContext: Context? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _baseline = MutableStateFlow<MeterCycleBaseline?>(null)
    val baseline: StateFlow<MeterCycleBaseline?> = _baseline.asStateFlow()

    private val _readings = MutableStateFlow<List<MeterReadingRecord>>(emptyList())
    val readings: StateFlow<List<MeterReadingRecord>> = _readings.asStateFlow()

    private val _metrics = MutableStateFlow<CurrentCycleEnergyMetrics?>(null)
    val metrics: StateFlow<CurrentCycleEnergyMetrics?> = _metrics.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val baseRaw = prefs.getString(KEY_BASELINE, null)
            if (!baseRaw.isNullOrBlank()) {
                _baseline.value = json.decodeFromString<MeterCycleBaseline>(baseRaw)
            }

            val readingsRaw = prefs.getString(KEY_READINGS, null)
            if (!readingsRaw.isNullOrBlank()) {
                _readings.value = json.decodeFromString<List<MeterReadingRecord>>(readingsRaw)
            }

            recalculateMetrics()
            logD("Restored meter cache: baseline=${_baseline.value?.baselineReading}, readings=${_readings.value.size}")
        } catch (e: Exception) {
            logE("Failed to restore meter reading cache", e)
        }
    }

    private fun saveToPrefs() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (_baseline.value != null) {
                editor.putString(KEY_BASELINE, json.encodeToString(_baseline.value))
            } else {
                editor.remove(KEY_BASELINE)
            }
            editor.putString(KEY_READINGS, json.encodeToString(_readings.value))
            editor.apply()
        } catch (e: Exception) {
            logE("Failed to persist meter readings", e)
        }
    }

    /**
     * Synchronizes the baseline starting point from the connected TNEB bill data.
     * The previous bill's kWh reading becomes the baseline starting point of the current cycle.
     * The previous billed units (e.g. 302 kWh) remain purely historical baseline data.
     */
    fun syncBaselineFromTnebBill(billData: TnebConsumerBillData) {
        val latest = billData.latestBill ?: return

        // Extract meter reading in kWh from bill
        val billKwh = latest.kwh.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        val prevBilledUnits = latest.units.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        val billDateRaw = latest.assessmentDate.ifBlank { latest.entryDate.ifBlank { latest.dueDate } }
        val formattedDate = normalizeDateString(billDateRaw)
        val cycleId = "BILL-${latest.id.ifBlank { billDateRaw.replace("/", "").replace("-", "") }}"
        val resolvedTariff = TariffResolver.resolve(billData.tariffCode.ifBlank { billData.serviceCategory })
        val tariff = resolvedTariff.displayLabel

        if (billKwh == null || billKwh <= 0.0) {
            logW("Connected bill has no valid cumulative kWh meter reading. kwh='${latest.kwh}'")
            // Even if kWh is missing in TNEB table, we can still establish the baseline cycle identifier
            val currentBase = _baseline.value
            if (currentBase == null || currentBase.cycleId != cycleId) {
                val newBase = MeterCycleBaseline(
                    cycleId = cycleId,
                    baselineDate = formattedDate,
                    baselineReading = 0.0,
                    previousBilledUnits = prevBilledUnits,
                    tariffCode = tariff,
                    serviceConnectionNumber = billData.consumerNumber
                )
                _baseline.value = newBase
                saveToPrefs()
            }
            return
        }

        val currentBase = _baseline.value
        if (currentBase != null && currentBase.cycleId != cycleId) {
            // A new utility bill is available! New billing cycle rollover:
            // The new bill becomes the baseline for the next cycle.
            // Archive previous cycle readings and reset current-cycle tracking.
            logD("New billing cycle detected. Rollover from ${currentBase.cycleId} to $cycleId")
            archiveCycle(currentBase.cycleId, _readings.value)
            _readings.value = emptyList()
        }

        val newBase = MeterCycleBaseline(
            cycleId = cycleId,
            baselineDate = formattedDate,
            baselineReading = billKwh,
            previousBilledUnits = prevBilledUnits,
            tariffCode = tariff,
            serviceConnectionNumber = billData.consumerNumber
        )
        _baseline.value = newBase
        recalculateMetrics()
        saveToPrefs()
        logD("Synchronized baseline: $billKwh kWh on $formattedDate (Previous units: $prevBilledUnits kWh)")
    }

    /**
     * Synchronizes authoritative baseline received from the backend server.
     */
    fun syncBaselineFromServer(serverBase: BaselineResponseDto) {
        val currentBase = _baseline.value
        if (currentBase != null && currentBase.cycleId != serverBase.cycleId) {
            logD("Server baseline cycle rollover from ${currentBase.cycleId} to ${serverBase.cycleId}")
            archiveCycle(currentBase.cycleId, _readings.value)
            _readings.value = emptyList()
        }

        val formattedDate = normalizeDateString(serverBase.baselineDate)
        val newBase = MeterCycleBaseline(
            cycleId = serverBase.cycleId,
            baselineDate = formattedDate,
            baselineReading = serverBase.baselineReading,
            previousBilledUnits = serverBase.previousBilledUnits,
            tariffCode = serverBase.tariffCode ?: "LT-IA · Domestic",
            serviceConnectionNumber = serverBase.serviceConnectionNumber
        )
        _baseline.value = newBase
        recalculateMetrics()
        saveToPrefs()
        logD("Synchronized server baseline: ${serverBase.baselineReading} kWh on $formattedDate")
    }

    /**
     * Merges server readings authoritatively into local cache and recomputes consumption sequences.
     */
    fun mergeServerReadings(serverReadings: List<MeterReadingResponseDto>) {
        val base = _baseline.value ?: return
        val currentMap = _readings.value.associateBy { it.readingDate }.toMutableMap()

        for (serverItem in serverReadings) {
            val existing = currentMap[serverItem.readingDate]
            val record = if (existing != null) {
                existing.copy(
                    readingValue = serverItem.readingValue,
                    billingCycleId = serverItem.billingCycleId,
                    notes = serverItem.notes ?: existing.notes
                )
            } else {
                MeterReadingRecord(
                    id = serverItem.id,
                    billingCycleId = serverItem.billingCycleId,
                    readingValue = serverItem.readingValue,
                    readingDate = serverItem.readingDate,
                    displayDate = formatIsoToDisplay(serverItem.readingDate),
                    notes = serverItem.notes
                )
            }
            currentMap[serverItem.readingDate] = record
        }

        val updatedList = recomputeSequence(currentMap.values.toList(), base.baselineReading)
        _readings.value = updatedList
        recalculateMetrics()
        saveToPrefs()
        logD("Merged ${serverReadings.size} readings from server into cache")
    }

    /**
     * Explicitly set baseline reading (e.g. if previous bill was missing reading or for testing).
     */
    fun setManualBaseline(
        reading: Double,
        date: String,
        cycleId: String = "MANUAL-${System.currentTimeMillis()}",
        previousUnits: Double? = null
    ) {
        val base = MeterCycleBaseline(
            cycleId = cycleId,
            baselineDate = date,
            baselineReading = reading,
            previousBilledUnits = previousUnits
        )
        _baseline.value = base
        recalculateMetrics()
    }

    /**
     * Test or data-import helper to directly set readings and baseline in cache.
     */
    fun setReadingsForTesting(readings: List<MeterReadingRecord>, base: MeterCycleBaseline? = null) {
        _baseline.value = base
        _readings.value = readings
        recalculateMetrics()
    }

    /**
     * Saves today's meter reading with strict validation.
     * Prevents negative readings, readings lower than baseline, readings lower than previous,
     * and duplicate submissions for the same day.
     */
    fun saveReading(
        readingValue: Double,
        readingDateIso: String? = null
    ): MeterValidationResult {
        val base = _baseline.value
            ?: return MeterValidationResult.Error("Please connect your TNEB bill before entering meter readings.")

        val dateKey = readingDateIso ?: getTodayIsoDate()
        val displayDate = formatIsoToDisplay(dateKey)

        if (readingValue < 0.0) {
            return MeterValidationResult.Error("Meter reading cannot be negative.")
        }

        if (base.baselineReading > 0.0 && readingValue < base.baselineReading) {
            val baseFormatted = String.format(Locale.getDefault(), "%,.0f", base.baselineReading)
            return MeterValidationResult.Error("Current reading cannot be lower than your last bill reading ($baseFormatted kWh).")
        }

        val currentList = _readings.value

        // Check for duplicate on the same date
        val existingToday = currentList.firstOrNull { it.readingDate == dateKey }
        if (existingToday != null) {
            return MeterValidationResult.DuplicateToday(existingToday)
        }

        // Check against the last recorded reading
        val lastReading = currentList.maxByOrNull { it.readingDate }
        val prevReadingVal = lastReading?.readingValue ?: base.baselineReading
        if (readingValue < prevReadingVal) {
            val prevFormatted = String.format(Locale.getDefault(), "%,.0f", prevReadingVal)
            return if (lastReading != null) {
                MeterValidationResult.LowerThanPrevious(
                    enteredReading = readingValue,
                    previousReading = lastReading,
                    message = "Meter reading cannot be lower than the previous recorded reading ($prevFormatted kWh)."
                )
            } else {
                MeterValidationResult.Error("Meter reading cannot be lower than your last bill reading ($prevFormatted kWh).")
            }
        }

        val unitsSinceBase = max(0.0, readingValue - base.baselineReading)
        val dailyUnits = max(0.0, readingValue - prevReadingVal)

        val newRecord = MeterReadingRecord(
            billingCycleId = base.cycleId,
            readingValue = readingValue,
            readingDate = dateKey,
            displayDate = displayDate,
            dailyConsumption = dailyUnits,
            unitsSinceBaseline = unitsSinceBase
        )

        val updatedList = recomputeSequence(currentList + newRecord, base.baselineReading)
        _readings.value = updatedList
        recalculateMetrics()
        saveToPrefs()

        logD("Saved reading: $readingValue kWh on $dateKey (Daily: $dailyUnits, Cycle: $unitsSinceBase)")
        try {
            val userId = com.example.core.session.UserSession.currentUser?.id?.takeIf { it.isNotBlank() }
                ?: com.example.core.session.UserSession.currentUser?.username?.takeIf { it.isNotBlank() }
                ?: "default_user"
            com.example.feature.recommends.data.RewardStorage.recordDailyActivity(
                userId = userId,
                activityId = "meter_reading_${newRecord.id}",
                date = dateKey
            )
        } catch (_: Throwable) {}
        return MeterValidationResult.Success(updatedList.first { it.id == newRecord.id })
    }

    /**
     * Safely updates today's existing meter reading.
     */
    fun updateTodayReading(
        readingValue: Double,
        readingDateIso: String? = null
    ): MeterValidationResult {
        val base = _baseline.value
            ?: return MeterValidationResult.Error("No active billing cycle found.")

        val currentList = _readings.value
        val todayKey = readingDateIso ?: getTodayIsoDate()
        var existingIndex = currentList.indexOfFirst { it.readingDate == todayKey }
        if (existingIndex == -1 && readingDateIso == null && currentList.isNotEmpty()) {
            existingIndex = currentList.lastIndex
        }

        if (existingIndex == -1) {
            return MeterValidationResult.Error("No reading found for today to edit.")
        }

        val targetKey = currentList[existingIndex].readingDate

        if (readingValue < 0.0) {
            return MeterValidationResult.Error("Meter reading cannot be negative.")
        }

        if (base.baselineReading > 0.0 && readingValue < base.baselineReading) {
            val baseFormatted = String.format(Locale.getDefault(), "%,.0f", base.baselineReading)
            return MeterValidationResult.Error("Updated reading cannot be lower than your last bill reading ($baseFormatted kWh).")
        }

        // Preceding reading (before target date)
        val priorReadings = currentList.filter { it.readingDate < targetKey }
        val prevVal = priorReadings.maxByOrNull { it.readingDate }?.readingValue ?: base.baselineReading

        if (readingValue < prevVal) {
            val prevFormatted = String.format(Locale.getDefault(), "%,.0f", prevVal)
            return MeterValidationResult.Error("Updated reading cannot be lower than prior reading ($prevFormatted kWh).")
        }

        // Subsequent readings (after target date, if any)
        val subsequentReadings = currentList.filter { it.readingDate > targetKey }
        val nextVal = subsequentReadings.minByOrNull { it.readingDate }?.readingValue
        if (nextVal != null && readingValue > nextVal) {
            val nextFormatted = String.format(Locale.getDefault(), "%,.0f", nextVal)
            return MeterValidationResult.Error("Updated reading cannot be higher than subsequent reading ($nextFormatted kWh).")
        }

        val existing = currentList[existingIndex]
        val updated = existing.copy(
            readingValue = readingValue,
            timestamp = System.currentTimeMillis()
        )

        val mutableList = currentList.toMutableList()
        mutableList[existingIndex] = updated
        val recomputed = recomputeSequence(mutableList, base.baselineReading)
        _readings.value = recomputed
        recalculateMetrics()
        saveToPrefs()

        logD("Updated today's reading to $readingValue kWh")
        return MeterValidationResult.Success(recomputed.first { it.id == existing.id })
    }

    /**
     * Corrects a historical reading recorded on a previous date.
     * Validates that:
     * 1. readingValue >= 0.0
     * 2. readingValue >= lowerBound (previous reading or baseline)
     * 3. readingValue <= upperBound (subsequent reading, if one exists)
     * Recomputes all sequential consumption values and energy metrics.
     * Preserves audit metadata (isCorrected, correctionReason, correctedAt).
     */
    fun correctHistoricalReading(
        readingDateIso: String,
        newReadingValue: Double,
        reason: String? = null
    ): MeterValidationResult {
        val base = _baseline.value
            ?: return MeterValidationResult.Error("No active billing cycle found.")

        if (newReadingValue < 0.0) {
            return MeterValidationResult.Error("Meter reading cannot be negative.")
        }

        if (base.baselineReading > 0.0 && newReadingValue < base.baselineReading) {
            val baseFormatted = String.format(Locale.getDefault(), "%,.0f", base.baselineReading)
            return MeterValidationResult.Error("Corrected reading cannot be lower than your last bill reading ($baseFormatted kWh).")
        }

        val currentList = _readings.value
        val existingIndex = currentList.indexOfFirst { it.readingDate == readingDateIso }
        if (existingIndex == -1) {
            return MeterValidationResult.Error("No reading found for date $readingDateIso to correct.")
        }

        val priorReadings = currentList.filter { it.readingDate < readingDateIso }
        val lowerBound = priorReadings.maxByOrNull { it.readingDate }?.readingValue ?: base.baselineReading
        val lowerBoundDate = priorReadings.maxByOrNull { it.readingDate }?.displayDate?.ifBlank { priorReadings.maxByOrNull { it.readingDate }?.readingDate } ?: base.baselineDate

        if (newReadingValue < lowerBound) {
            val lowerFormatted = String.format(Locale.getDefault(), "%,.0f", lowerBound)
            return MeterValidationResult.Error("Corrected reading cannot be lower than the previous recorded reading on $lowerBoundDate ($lowerFormatted kWh).")
        }

        val subsequentReadings = currentList.filter { it.readingDate > readingDateIso }
        val upperBoundRecord = subsequentReadings.minByOrNull { it.readingDate }
        if (upperBoundRecord != null && newReadingValue > upperBoundRecord.readingValue) {
            val upperFormatted = String.format(Locale.getDefault(), "%,.0f", upperBoundRecord.readingValue)
            val upperDate = upperBoundRecord.displayDate.ifBlank { upperBoundRecord.readingDate }
            return MeterValidationResult.Error("Corrected reading cannot be higher than the next recorded reading on $upperDate ($upperFormatted kWh).")
        }

        val existing = currentList[existingIndex]
        val updated = existing.copy(
            readingValue = newReadingValue,
            isCorrected = true,
            correctionReason = reason,
            correctedAt = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis()
        )

        val mutableList = currentList.toMutableList()
        mutableList[existingIndex] = updated
        val recomputed = recomputeSequence(mutableList, base.baselineReading)
        _readings.value = recomputed
        recalculateMetrics()
        saveToPrefs()

        logD("Corrected historical reading on $readingDateIso from ${existing.readingValue} to $newReadingValue kWh (Reason: $reason)")
        return MeterValidationResult.Success(recomputed.first { it.id == existing.id })
    }

    /**
     * Resolves the surrounding valid bounds for a reading on a given date.
     */
    fun getSurroundingBounds(readingDateIso: String): SurroundingBounds {
        val base = _baseline.value
        val fallbackBase = base?.baselineReading ?: 0.0
        val fallbackDate = base?.baselineDate ?: "Last Bill"

        val currentList = _readings.value.sortedBy { it.readingDate }
        val priorReadings = currentList.filter { it.readingDate < readingDateIso }
        val lowerBound = priorReadings.maxByOrNull { it.readingDate }?.readingValue ?: fallbackBase
        val lowerBoundDate = priorReadings.maxByOrNull { it.readingDate }?.displayDate?.ifBlank { priorReadings.maxByOrNull { it.readingDate }?.readingDate } ?: fallbackDate

        val subsequentReadings = currentList.filter { it.readingDate > readingDateIso }
        val upperBoundRecord = subsequentReadings.minByOrNull { it.readingDate }
        val upperBound = upperBoundRecord?.readingValue
        val upperBoundDate = upperBoundRecord?.displayDate?.ifBlank { upperBoundRecord.readingDate }

        return SurroundingBounds(
            lowerBound = lowerBound,
            lowerBoundDate = lowerBoundDate,
            upperBound = upperBound,
            upperBoundDate = upperBoundDate
        )
    }

    fun getReadingForDate(readingDateIso: String): MeterReadingRecord? {
        return _readings.value.firstOrNull { it.readingDate == readingDateIso }
    }

    fun getLastReading(): MeterReadingRecord? {
        return _readings.value.maxByOrNull { it.readingDate }
    }

    /**
     * Recomputes daily consumption deltas and cumulative units since baseline
     * sequentially for all recorded readings in the active cycle.
     */
    private fun recomputeSequence(list: List<MeterReadingRecord>, baseReading: Double): List<MeterReadingRecord> {
        val sorted = list.sortedBy { it.readingDate }
        return sorted.mapIndexed { index, record ->
            val prevVal = if (index == 0) baseReading else sorted[index - 1].readingValue
            val daily = max(0.0, record.readingValue - prevVal)
            val unitsSinceBase = max(0.0, record.readingValue - baseReading)
            record.copy(
                dailyConsumption = daily,
                unitsSinceBaseline = unitsSinceBase
            )
        }
    }

    /**
     * Computes the current-cycle consumption metrics.
     * Consumed units = Current reading - previous bill baseline reading.
     */
    private fun recalculateMetrics() {
        val base = _baseline.value
        if (base == null) {
            _metrics.value = null
            return
        }

        val currentList = _readings.value
        val latestRecord = currentList.maxByOrNull { it.readingDate }

        val todayKey = getTodayIsoDate()
        val todayRecord = currentList.firstOrNull { it.readingDate == todayKey }
        val isTodayLogged = todayRecord != null

        if (latestRecord == null) {
            // Bill connected, but no current reading entered yet
            _metrics.value = CurrentCycleEnergyMetrics(
                billingCycleId = base.cycleId,
                baselineReading = base.baselineReading,
                baselineDate = base.baselineDate,
                currentReading = base.baselineReading,
                currentReadingDate = base.baselineDate,
                unitsConsumed = 0.0,
                daysElapsed = 0,
                avgUnitsPerDay = 0.0,
                estimatedBill = 0.0,
                isTodayLogged = false,
                todayReading = null,
                tariffCategory = TariffResolver.resolve(base.tariffCode).displayLabel,
                billingPeriodDisplay = "${base.baselineDate} – Present"
            )
            return
        }

        val unitsConsumed = max(0.0, latestRecord.readingValue - base.baselineReading)
        val daysElapsed = max(1, calculateDaysBetween(base.baselineDate, latestRecord.readingDate))
        val avgUnitsPerDay = if (daysElapsed > 0) unitsConsumed / daysElapsed else unitsConsumed

        // Calculate estimated bill using exact TNEBBillCalculator
        val billResult = TNEBBillCalculator.calculateTNEBBill(unitsConsumed)
        val estimatedBill = billResult.totalEnergyCharge

        _metrics.value = CurrentCycleEnergyMetrics(
            billingCycleId = base.cycleId,
            baselineReading = base.baselineReading,
            baselineDate = base.baselineDate,
            currentReading = latestRecord.readingValue,
            currentReadingDate = latestRecord.displayDate.ifBlank { latestRecord.readingDate },
            unitsConsumed = unitsConsumed,
            daysElapsed = daysElapsed,
            avgUnitsPerDay = avgUnitsPerDay,
            estimatedBill = estimatedBill,
            isTodayLogged = isTodayLogged,
            todayReading = todayRecord?.readingValue,
            tariffCategory = TariffResolver.resolve(base.tariffCode).displayLabel,
            billingPeriodDisplay = "${base.baselineDate} – ${latestRecord.displayDate.ifBlank { "Present" }}"
        )
    }

    /**
     * Generates compact time-series points for the dashboard graph.
     * Point 0 is the baseline meter reading on baseline date.
     * Subsequent points are daily meter readings.
     */
    fun getGraphPoints(): List<MeterGraphPoint> {
        val base = _baseline.value ?: return emptyList()
        val points = mutableListOf<MeterGraphPoint>()

        // 1. Starting anchor point: Baseline bill reading
        points.add(
            MeterGraphPoint(
                dateLabel = formatToShortDate(base.baselineDate),
                readingValue = base.baselineReading,
                unitsConsumed = 0.0,
                dailyUnits = 0.0,
                isBaseline = true,
                fullDate = base.baselineDate,
                subLabel = "Last Bill",
                readingDateIso = formatDateToIso(base.baselineDate)
            )
        )

        // 2. User's recorded daily meter readings
        val currentReadings = _readings.value
        for (i in currentReadings.indices) {
            val record = currentReadings[i]
            val consumed = max(0.0, record.readingValue - base.baselineReading)
            val isLast = (i == currentReadings.lastIndex)
            points.add(
                MeterGraphPoint(
                    dateLabel = formatToShortDate(record.displayDate.ifBlank { record.readingDate }),
                    readingValue = record.readingValue,
                    unitsConsumed = consumed,
                    dailyUnits = record.dailyConsumption,
                    isBaseline = false,
                    fullDate = record.displayDate.ifBlank { record.readingDate },
                    subLabel = if (isLast) "Today" else "",
                    readingDateIso = record.readingDate
                )
            )
        }

        return points
    }

    /**
     * Resolves the requested meter reading progression window for CURRENT, 2M, 4M, and 6M views.
     * - CURRENT: Anchored to latest paid bill -> current meter reading.
     * - 2M / 4M / 6M: Uses actual historical bill and meter readings, support window shifting.
     * - Disables navigation arrows appropriately when older or newer data does not exist.
     * - Returns isInsufficientData = true when not enough data exists (zero fabricated data).
     */
    fun getProgressionWindow(
        range: MeterGraphRange,
        windowOffset: Int = 0,
        providedBillData: TnebConsumerBillData? = null
    ): MeterProgressionWindow {
        if (range == MeterGraphRange.CURRENT) {
            val base = _baseline.value
            val currentPoints = getGraphPoints()
            if (base == null || base.baselineReading <= 0.0) {
                return MeterProgressionWindow(
                    range = MeterGraphRange.CURRENT,
                    windowOffset = 0,
                    periodTitle = "Current Cycle",
                    points = emptyList(),
                    latestReading = 0.0,
                    hasOlder = false,
                    hasNewer = false,
                    isInsufficientData = true
                )
            }

            val latestVal = currentPoints.lastOrNull()?.readingValue ?: base.baselineReading
            val startLabel = formatToShortDate(base.baselineDate)
            val endLabel = currentPoints.lastOrNull()?.dateLabel ?: startLabel
            val title = if (startLabel.equals(endLabel, ignoreCase = true)) startLabel else "$startLabel – $endLabel"

            return MeterProgressionWindow(
                range = MeterGraphRange.CURRENT,
                windowOffset = 0,
                periodTitle = title,
                points = currentPoints,
                latestReading = latestVal,
                hasOlder = false,
                hasNewer = false,
                isInsufficientData = currentPoints.isEmpty()
            )
        }

        // For historical ranges: 2M, 4M, 6M
        val billData = providedBillData ?: TnebBillCache.billData.value
        val rawBills = billData?.bills ?: emptyList()

        data class HistoricalBillItem(
            val date: Date,
            val readingValue: Double,
            val displayDate: String,
            val fullDate: String,
            val isBaseline: Boolean
        )

        val billItems = mutableListOf<HistoricalBillItem>()

        for (bill in rawBills) {
            val kwhVal = bill.kwh.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
            if (kwhVal != null && kwhVal > 0.0) {
                val dateRaw = bill.assessmentDate.ifBlank { bill.entryDate.ifBlank { bill.dueDate } }
                val parsedD = parseAnyDate(dateRaw)
                if (parsedD != null) {
                    billItems.add(
                        HistoricalBillItem(
                            date = parsedD,
                            readingValue = kwhVal,
                            displayDate = formatToShortDate(dateRaw),
                            fullDate = normalizeDateString(dateRaw),
                            isBaseline = false
                        )
                    )
                }
            }
        }

        // Include active baseline if valid
        val base = _baseline.value
        if (base != null && base.baselineReading > 0.0) {
            val baseDate = parseAnyDate(base.baselineDate)
            if (baseDate != null && billItems.none { it.readingValue == base.baselineReading && kotlin.math.abs(it.date.time - baseDate.time) < 86400000L * 3 }) {
                billItems.add(
                    HistoricalBillItem(
                        date = baseDate,
                        readingValue = base.baselineReading,
                        displayDate = formatToShortDate(base.baselineDate),
                        fullDate = base.baselineDate,
                        isBaseline = true
                    )
                )
            }
        }

        // Deduplicate and sort chronologically (earliest to latest)
        val sortedBills = billItems.distinctBy { it.date.time / 86400000L }
            .sortedBy { it.date.time }

        val cyclesRequired = when (range) {
            MeterGraphRange.M2 -> 1
            MeterGraphRange.M4 -> 2
            MeterGraphRange.M6 -> 3
            MeterGraphRange.CURRENT -> 0
        }

        val minBillsNeeded = cyclesRequired + 1
        if (sortedBills.size < minBillsNeeded) {
            return MeterProgressionWindow(
                range = range,
                windowOffset = 0,
                periodTitle = range.label,
                points = emptyList(),
                latestReading = 0.0,
                hasOlder = false,
                hasNewer = false,
                isInsufficientData = true
            )
        }

        val totalPossibleWindows = sortedBills.size - cyclesRequired
        val safeOffset = windowOffset.coerceIn(0, totalPossibleWindows - 1)

        val endingBillIndex = sortedBills.lastIndex - safeOffset
        val startingBillIndex = endingBillIndex - cyclesRequired

        val startBill = sortedBills[startingBillIndex]
        val endBill = sortedBills[endingBillIndex]

        val hasOlder = startingBillIndex > 0
        val hasNewer = safeOffset > 0

        // Extract all bill points in this window
        val windowBills = sortedBills.subList(startingBillIndex, endingBillIndex + 1)

        // Include any archived readings or current readings between startBill and endBill
        val archived = (getArchivedReadings() + _readings.value).filter { rec ->
            val d = parseAnyDate(rec.readingDate)
            d != null && d.time >= startBill.date.time && d.time <= endBill.date.time
        }

        // Build complete monthly timeline from startBill's month to endBill's month
        val startCal = Calendar.getInstance().apply {
            time = startBill.date
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            time = endBill.date
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val monthsInWindow = mutableListOf<Calendar>()
        val tempCal = startCal.clone() as Calendar
        while (!tempCal.after(endCal)) {
            monthsInWindow.add(tempCal.clone() as Calendar)
            tempCal.add(Calendar.MONTH, 1)
        }

        val allWindowPoints = mutableListOf<MeterGraphPoint>()
        for (calItem in monthsInWindow) {
            val matchingBill = windowBills.firstOrNull { isSameMonth(it.date, calItem) }
            val matchingReading = archived.firstOrNull { rec ->
                val d = parseAnyDate(rec.readingDate)
                d != null && isSameMonth(d, calItem)
            }

            val shortMonth = SimpleDateFormat("MMM", Locale.US).format(calItem.time)
            val fullMonth = SimpleDateFormat("MMMM yyyy", Locale.US).format(calItem.time)

            if (matchingBill != null) {
                allWindowPoints.add(
                    MeterGraphPoint(
                        dateLabel = shortMonth,
                        readingValue = matchingBill.readingValue,
                        unitsConsumed = max(0.0, matchingBill.readingValue - startBill.readingValue),
                        dailyUnits = null,
                        isBaseline = (matchingBill == startBill),
                        fullDate = matchingBill.fullDate.ifBlank { formatToMonthDayYear(matchingBill.date) },
                        subLabel = if (calItem == monthsInWindow.first()) "Start" else if (calItem == monthsInWindow.last()) "End" else "",
                        hasReading = true,
                        readingDateIso = formatDateToIso(matchingBill.fullDate)
                    )
                )
            } else if (matchingReading != null) {
                val recDate = parseAnyDate(matchingReading.readingDate) ?: calItem.time
                allWindowPoints.add(
                    MeterGraphPoint(
                        dateLabel = shortMonth,
                        readingValue = matchingReading.readingValue,
                        unitsConsumed = max(0.0, matchingReading.readingValue - startBill.readingValue),
                        dailyUnits = matchingReading.dailyConsumption,
                        isBaseline = false,
                        fullDate = matchingReading.displayDate.ifBlank { formatToMonthDayYear(recDate) },
                        subLabel = "",
                        hasReading = true,
                        readingDateIso = matchingReading.readingDate
                    )
                )
            } else {
                // Month within historical window has no recorded reading: preserve position on timeline without fake reading
                allWindowPoints.add(
                    MeterGraphPoint(
                        dateLabel = shortMonth,
                        readingValue = 0.0,
                        unitsConsumed = 0.0,
                        dailyUnits = null,
                        isBaseline = false,
                        fullDate = fullMonth,
                        subLabel = "",
                        hasReading = false
                    )
                )
            }
        }

        val validReadingsCount = allWindowPoints.count { it.hasReading }
        val periodTitle = "${formatToMonthYear(startBill.date)} – ${formatToMonthYear(endBill.date)}"

        return MeterProgressionWindow(
            range = range,
            windowOffset = safeOffset,
            periodTitle = periodTitle,
            points = allWindowPoints,
            latestReading = endBill.readingValue,
            hasOlder = hasOlder,
            hasNewer = hasNewer,
            isInsufficientData = validReadingsCount < 2
        )
    }

    private fun isSameMonth(d: Date, c: Calendar): Boolean {
        val cal = Calendar.getInstance().apply { time = d }
        return cal.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
               cal.get(Calendar.MONTH) == c.get(Calendar.MONTH)
    }

    private fun formatToMonthDayYear(d: Date): String {
        return SimpleDateFormat("MMMM d", Locale.US).format(d)
    }

    private fun formatToMonthYear(d: Date): String {
        return SimpleDateFormat("MMM yyyy", Locale.US).format(d)
    }

    /**
     * Calculates true daily energy consumption (kWh) from consecutive meter readings.
     * Example:
     * Sep 25 = 12,450
     * Sep 26 = 12,455 -> 5 kWh
     * Sep 27 = 12,461 -> 6 kWh
     * Sep 28 = 12,469 -> 8 kWh
     */
    fun getDailyConsumptionPoints(): List<DailyConsumptionPoint> {
        val base = _baseline.value
        val allReadings = (getArchivedReadings() + _readings.value)
            .distinctBy { it.readingDate }
            .sortedBy { it.readingDate }

        data class Checkpoint(val dateIso: String, val displayDate: String, val fullDate: String, val reading: Double)

        val checkpoints = mutableListOf<Checkpoint>()

        if (base != null && base.baselineReading > 0.0) {
            val baseIso = formatDateToIso(base.baselineDate)
            checkpoints.add(
                Checkpoint(
                    dateIso = baseIso,
                    displayDate = formatToShortDate(base.baselineDate),
                    fullDate = base.baselineDate,
                    reading = base.baselineReading
                )
            )
        }

        for (rec in allReadings) {
            checkpoints.add(
                Checkpoint(
                    dateIso = rec.readingDate,
                    displayDate = formatToShortDate(rec.displayDate.ifBlank { rec.readingDate }),
                    fullDate = rec.displayDate.ifBlank { rec.readingDate },
                    reading = rec.readingValue
                )
            )
        }

        // Deduplicate checkpoints by date, keeping the highest reading for each day
        val deduped = checkpoints.groupBy { it.dateIso }
            .mapValues { entry -> entry.value.maxByOrNull { it.reading }!! }
            .values
            .sortedBy { it.dateIso }

        if (deduped.size < 2) return emptyList()

        val dailyPoints = mutableListOf<DailyConsumptionPoint>()
        for (i in 1 until deduped.size) {
            val prev = deduped[i - 1]
            val curr = deduped[i]
            val diff = max(0.0, curr.reading - prev.reading)
            dailyPoints.add(
                DailyConsumptionPoint(
                    dateIso = curr.dateIso,
                    displayDate = curr.displayDate,
                    fullDate = curr.fullDate,
                    dailyKwh = diff,
                    cumulativeReading = curr.reading
                )
            )
        }

        return dailyPoints
    }

    fun getArchivedReadings(): List<MeterReadingRecord> {
        val ctx = appContext ?: return emptyList()
        return try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingRaw = prefs.getString(KEY_ARCHIVED_CYCLES, null)
            if (!existingRaw.isNullOrBlank()) {
                val archivedMap = json.decodeFromString<Map<String, List<MeterReadingRecord>>>(existingRaw)
                archivedMap.values.flatten()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatDateToIso(dateStr: String): String {
        val d = parseAnyDate(dateStr)
        return if (d != null) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
        } else dateStr
    }

    private fun archiveCycle(cycleId: String, readings: List<MeterReadingRecord>) {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existingRaw = prefs.getString(KEY_ARCHIVED_CYCLES, null)
            val archivedMap = if (!existingRaw.isNullOrBlank()) {
                json.decodeFromString<MutableMap<String, List<MeterReadingRecord>>>(existingRaw)
            } else {
                mutableMapOf()
            }
            archivedMap[cycleId] = readings
            prefs.edit().putString(KEY_ARCHIVED_CYCLES, json.encodeToString(archivedMap)).apply()
        } catch (e: Exception) {
            logE("Failed to archive previous cycle readings", e)
        }
    }

    fun getTodayIsoDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun formatIsoToDisplay(iso: String): String {
        return try {
            val inSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outSdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
            val parsed = inSdf.parse(iso)
            if (parsed != null) outSdf.format(parsed) else iso
        } catch (_: Exception) {
            iso
        }
    }

    private fun formatToShortDate(dateStr: String): String {
        return try {
            val formats = listOf("dd MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MMM-yyyy")
            for (fmt in formats) {
                try {
                    val sdf = SimpleDateFormat(fmt, Locale.US)
                    val d = sdf.parse(dateStr)
                    if (d != null) {
                        return SimpleDateFormat("d MMM", Locale.US).format(d)
                    }
                } catch (_: Exception) {}
            }
            dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    private fun normalizeDateString(dateStr: String): String {
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd", "dd-MMM-yyyy", "dd MMM yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) {
                    return SimpleDateFormat("dd MMM yyyy", Locale.US).format(d)
                }
            } catch (_: Exception) {}
        }
        return dateStr.ifBlank {
            SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        }
    }

    private fun calculateDaysBetween(startDateStr: String, endDateStr: String): Int {
        return try {
            val start = parseAnyDate(startDateStr)
            val end = parseAnyDate(endDateStr)
            if (start != null && end != null) {
                val diffMs = max(0L, end.time - start.time)
                (diffMs / (1000 * 60 * 60 * 24)).toInt()
            } else 1
        } catch (_: Exception) {
            1
        }
    }

    private fun parseAnyDate(str: String): Date? {
        val formats = listOf("dd MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MMM-yyyy", "dd-MM-yyyy")
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(str)
                if (d != null) return d
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * Returns all saved meter readings across the current cycle and archived cycles,
     * deduplicated and sorted chronologically.
     */
    fun getAllSavedReadings(): List<MeterReadingRecord> {
        val current = _readings.value
        val archived = getArchivedReadings()
        val combined = (current + archived)
            .distinctBy { it.id }
            .distinctBy { it.readingDate }
            .sortedWith(compareBy({ it.readingDate }, { it.timestamp }))
        return combined
    }

    /**
     * Resolves the Sunday–Saturday weekly meter reading bar chart data.
     * - weekOffset = 0: current calendar week (Sunday to Saturday)
     * - weekOffset > 0: past weeks (prevent future weeks)
     * - Weekly baseline:
     *     1. Latest valid reading on or before the week's Sunday.
     *     2. If none, first valid reading in the displayed week (anchored at 0 kWh).
     * - Plots relative values above baseline (cumulative - baseline).
     * - Safe handling for decreased readings (no negative consumption, flagged for review).
     * - No fabricated readings for missing days.
     */
    fun getWeeklyGraphData(
        weekOffset: Int = 0,
        anchorDate: Date? = null
    ): WeeklyMeterGraphData {
        val cal = Calendar.getInstance(Locale.getDefault())
        if (anchorDate != null) cal.time = anchorDate
        cal.firstDayOfWeek = Calendar.SUNDAY
        cal.add(Calendar.WEEK_OF_YEAR, -weekOffset)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayCals = (0..6).map { dayIdx ->
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, dayIdx)
            c
        }

        val sundayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dayCals.first().time)
        val saturdayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dayCals.last().time)

        val startLabel = SimpleDateFormat("d MMM", Locale.US).format(dayCals.first().time)
        val endLabel = SimpleDateFormat("d MMM", Locale.US).format(dayCals.last().time)
        val periodTitle = if (weekOffset == 0) {
            "This Week · $startLabel – $endLabel"
        } else {
            "$startLabel – $endLabel"
        }

        val allReadings = getAllSavedReadings()
        val weekReadings = allReadings.filter { it.readingDate in sundayIso..saturdayIso }

        // Baseline resolution
        val priorReading = allReadings.filter { it.readingDate < sundayIso }.lastOrNull()
        val billBase = _baseline.value
        val billBaseIso = billBase?.baselineDate?.let { formatDateToIso(it) }

        var baselineReading: Double? = null
        var baselineDate: String? = null
        var baselineDescription = ""
        var isMidweekBaseline = false
        var midweekBaselineRecord: MeterReadingRecord? = null

        if (priorReading != null) {
            baselineReading = priorReading.readingValue
            baselineDate = formatToShortDate(priorReading.displayDate.ifBlank { priorReading.readingDate })
            baselineDescription = "Baseline from previous reading on $baselineDate"
        } else if (billBase != null && billBase.baselineReading > 0.0 && billBaseIso != null && billBaseIso <= sundayIso) {
            baselineReading = billBase.baselineReading
            baselineDate = formatToShortDate(billBase.baselineDate)
            baselineDescription = "Baseline from bill on $baselineDate"
        } else if (weekReadings.isNotEmpty()) {
            midweekBaselineRecord = weekReadings.first()
            baselineReading = midweekBaselineRecord.readingValue
            baselineDate = formatToShortDate(midweekBaselineRecord.displayDate.ifBlank { midweekBaselineRecord.readingDate })
            baselineDescription = "Midweek baseline on $baselineDate"
            isMidweekBaseline = true
        }

        val days = dayCals.mapIndexed { _, dayCal ->
            val dayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(dayCal.time)
            val dayLabel = SimpleDateFormat("EEE", Locale.US).format(dayCal.time)
            val displayDate = SimpleDateFormat("d MMM", Locale.US).format(dayCal.time)
            val fullDate = SimpleDateFormat("EEEE, d MMM yyyy", Locale.US).format(dayCal.time)

            val readingOnDay = weekReadings.firstOrNull { it.readingDate == dayIso }

            if (readingOnDay != null) {
                val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(readingOnDay.timestamp))
                val prevValid = allReadings.filter { it.readingDate < dayIso }.lastOrNull()

                val isDecreasedFromPrev = prevValid != null && readingOnDay.readingValue < prevValid.readingValue
                val intervalDays = if (prevValid != null) {
                    calculateDaysBetween(prevValid.readingDate, dayIso)
                } else null

                val intervalConsumption = if (prevValid != null) {
                    if (isDecreasedFromPrev) null
                    else max(0.0, readingOnDay.readingValue - prevValid.readingValue)
                } else null

                val isAnchor = (isMidweekBaseline && readingOnDay.id == midweekBaselineRecord?.id)
                val isDecreasedFromBaseline = baselineReading != null && readingOnDay.readingValue < baselineReading

                val relativeKwh = when {
                    isAnchor -> 0.0
                    baselineReading != null -> {
                        if (isDecreasedFromBaseline) null
                        else max(0.0, readingOnDay.readingValue - baselineReading)
                    }
                    else -> null
                }

                WeeklyDayBarData(
                    dayOfWeekIndex = dayCal.get(Calendar.DAY_OF_WEEK),
                    dayLabel = dayLabel,
                    dateIso = dayIso,
                    displayDate = displayDate,
                    fullDate = fullDate,
                    reading = readingOnDay,
                    cumulativeKwh = readingOnDay.readingValue,
                    relativeKwh = relativeKwh,
                    intervalConsumption = intervalConsumption,
                    intervalDays = intervalDays,
                    previousReadingDate = prevValid?.displayDate?.ifBlank { prevValid.readingDate },
                    previousReadingValue = prevValid?.readingValue,
                    isBaselinePoint = isAnchor,
                    isDecreasedReading = isDecreasedFromPrev || isDecreasedFromBaseline,
                    formattedTime = timeStr
                )
            } else {
                WeeklyDayBarData(
                    dayOfWeekIndex = dayCal.get(Calendar.DAY_OF_WEEK),
                    dayLabel = dayLabel,
                    dateIso = dayIso,
                    displayDate = displayDate,
                    fullDate = fullDate,
                    reading = null,
                    cumulativeKwh = null,
                    relativeKwh = null,
                    intervalConsumption = null,
                    intervalDays = null,
                    isBaselinePoint = false,
                    isDecreasedReading = false
                )
            }
        }

        val hasOlder = allReadings.any { it.readingDate < sundayIso } || (billBase != null && billBase.baselineReading > 0.0 && billBaseIso != null && billBaseIso < sundayIso)
        val hasNewer = weekOffset > 0

        val latestInWeek = weekReadings.lastOrNull()?.readingValue
        val latestReading = latestInWeek ?: allReadings.filter { it.readingDate <= saturdayIso }.lastOrNull()?.readingValue ?: baselineReading

        return WeeklyMeterGraphData(
            weekOffset = weekOffset,
            startDateIso = sundayIso,
            endDateIso = saturdayIso,
            periodTitle = periodTitle,
            weeklyBaselineReading = baselineReading,
            weeklyBaselineDate = baselineDate,
            baselineDescription = baselineDescription,
            days = days,
            latestReading = latestReading,
            hasOlder = hasOlder,
            hasNewer = hasNewer,
            hasAnyReadingsInWeek = weekReadings.isNotEmpty()
        )
    }

    /**
     * Precomputes and filters the entire meter reading history for the 7D, 30 Days, and All tabs.
     * Sorted newest first for the list.
     */
    fun getFilteredReadingHistory(
        filter: ReadingDetailsFilter,
        anchorDate: Date? = null
    ): List<ReadingHistoryItem> {
        val allReadings = getAllSavedReadings()
        if (allReadings.isEmpty()) return emptyList()

        val sortedDesc = allReadings.sortedWith(
            compareByDescending<MeterReadingRecord> { it.readingDate }
                .thenByDescending { it.timestamp }
        )

        val filtered = when (filter) {
            ReadingDetailsFilter.D7 -> sortedDesc.take(7)
            ReadingDetailsFilter.D30 -> sortedDesc.take(30)
            ReadingDetailsFilter.ALL -> sortedDesc
        }

        val items = filtered.map { record ->
            val prevValid = allReadings.filter { it.readingDate < record.readingDate }.lastOrNull()
            val isDecreased = prevValid != null && record.readingValue < prevValid.readingValue
            val intervalDays = if (prevValid != null) calculateDaysBetween(prevValid.readingDate, record.readingDate) else null
            val consumption = if (prevValid != null) {
                if (isDecreased) null else max(0.0, record.readingValue - prevValid.readingValue)
            } else null

            val timeStr = SimpleDateFormat("h:mm a", Locale.US).format(Date(record.timestamp))
            val dateStr = formatIsoToDisplay(record.readingDate)

            ReadingHistoryItem(
                reading = record,
                previousReading = prevValid,
                consumptionSincePrevious = consumption,
                intervalDays = intervalDays,
                isBaseline = (prevValid == null),
                isDecreased = isDecreased,
                formattedTime = timeStr,
                dateDisplay = dateStr
            )
        }

        return items
    }

    /**
     * Clears all cached meter readings and baseline. Used during logout or tests.
     */
    fun clear(context: Context? = null) {
        _baseline.value = null
        _readings.value = emptyList()
        _metrics.value = null

        val ctx = context ?: appContext
        if (ctx != null) {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (e: Exception) {
                logE("Failed to clear meter prefs", e)
            }
        }
    }
}
