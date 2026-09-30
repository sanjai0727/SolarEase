package com.example.feature.home.ui.modules

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
        val tariff = billData.tariffCode.ifBlank { billData.serviceCategory.ifBlank { "TANGEDCO LT-1A · DOMESTIC" } }

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
        saveToPrefs()
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
            return MeterValidationResult.Error("Meter reading cannot be lower than the previous recorded reading ($prevFormatted kWh).")
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

        val updatedList = (currentList + newRecord).sortedBy { it.readingDate }
        _readings.value = updatedList
        recalculateMetrics()
        saveToPrefs()

        logD("Saved reading: $readingValue kWh on $dateKey (Daily: $dailyUnits, Cycle: $unitsSinceBase)")
        return MeterValidationResult.Success(newRecord)
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

        val unitsSinceBase = max(0.0, readingValue - base.baselineReading)
        val dailyUnits = max(0.0, readingValue - prevVal)

        val existing = currentList[existingIndex]
        val updated = existing.copy(
            readingValue = readingValue,
            dailyConsumption = dailyUnits,
            unitsSinceBaseline = unitsSinceBase,
            timestamp = System.currentTimeMillis()
        )

        val mutableList = currentList.toMutableList()
        mutableList[existingIndex] = updated
        _readings.value = mutableList.sortedBy { it.readingDate }
        recalculateMetrics()
        saveToPrefs()

        logD("Updated today's reading to $readingValue kWh")
        return MeterValidationResult.Success(updated)
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
                tariffCategory = base.tariffCode,
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
            tariffCategory = base.tariffCode,
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
                subLabel = "Last Bill"
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
                    subLabel = if (isLast) "Today" else ""
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

        val allWindowPoints = mutableListOf<MeterGraphPoint>()
        for (b in windowBills) {
            allWindowPoints.add(
                MeterGraphPoint(
                    dateLabel = b.displayDate,
                    readingValue = b.readingValue,
                    unitsConsumed = max(0.0, b.readingValue - startBill.readingValue),
                    dailyUnits = null,
                    isBaseline = (b == startBill),
                    fullDate = b.fullDate,
                    subLabel = if (b == startBill) "Start" else if (b == endBill) "End" else ""
                )
            )
        }

        for (rec in archived) {
            if (allWindowPoints.none { it.readingValue == rec.readingValue }) {
                allWindowPoints.add(
                    MeterGraphPoint(
                        dateLabel = formatToShortDate(rec.displayDate.ifBlank { rec.readingDate }),
                        readingValue = rec.readingValue,
                        unitsConsumed = max(0.0, rec.readingValue - startBill.readingValue),
                        dailyUnits = rec.dailyConsumption,
                        isBaseline = false,
                        fullDate = rec.displayDate.ifBlank { rec.readingDate }
                    )
                )
            }
        }

        val sortedPoints = allWindowPoints.sortedBy { it.readingValue }
        val periodTitle = "${formatToMonthYear(startBill.date)} – ${formatToMonthYear(endBill.date)}"

        return MeterProgressionWindow(
            range = range,
            windowOffset = safeOffset,
            periodTitle = periodTitle,
            points = sortedPoints,
            latestReading = endBill.readingValue,
            hasOlder = hasOlder,
            hasNewer = hasNewer,
            isInsufficientData = sortedPoints.size < 2
        )
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
