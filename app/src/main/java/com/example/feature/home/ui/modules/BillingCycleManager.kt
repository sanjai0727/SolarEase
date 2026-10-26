package com.example.feature.home.ui.modules

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Single source of truth for billing cycle calculation and historical comparison across SolarEase.
 * Follows core rules:
 * 1. Cycle consumption is compared, NOT cumulative meter readings.
 * 2. Bill-status data defines completed billing periods.
 * 3. The period immediately following the latest completed cycle is the CURRENT ACTIVE cycle.
 * 4. Current active cycle consumption is calculated from daily meter readings (current - opening).
 * 5. Normalized daily rate comparisons prevent misleading claims when cycle durations differ.
 * 6. Bill-based energy credits are verified ONLY from completed cycle reductions.
 */
object BillingCycleManager {

    private val MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    /**
     * Constructs the chronological list of billing cycles (oldest to newest).
     * The last item is the CURRENT active cycle.
     */
    fun buildChronologicalCycles(
        billData: TnebConsumerBillData?,
        baseline: MeterCycleBaseline?,
        readings: List<MeterReadingRecord>,
        metrics: CurrentCycleEnergyMetrics?
    ): List<BillingCycleItem> {
        val bills = billData?.bills.orEmpty()

        if (bills.isNotEmpty()) {
            return buildCyclesFromBills(bills, baseline, readings, metrics)
        }

        // Fallback baseline when no bills connected yet (preserves demo data & user daily readings)
        return buildFallbackBaselineCycles(metrics, baseline, readings)
    }

    private fun buildCyclesFromBills(
        bills: List<TnebBillRecord>,
        baseline: MeterCycleBaseline?,
        readings: List<MeterReadingRecord>,
        metrics: CurrentCycleEnergyMetrics?
    ): List<BillingCycleItem> {
        // Bills from TNEB cache are typically newest first: bills[0] is latest
        val chronologicalBills = bills.reversed()
        val completedCycles = mutableListOf<BillingCycleItem>()

        for (i in chronologicalBills.indices) {
            val bill = chronologicalBills[i]
            val priorBill = if (i > 0) chronologicalBills[i - 1] else null

            val units = extractCycleUnits(bill, priorBill)
            val periodLabel = formatBiMonthlyPeriod(bill.period, bill.assessmentDate)
            val durationDays = calculateCycleDays(bill, priorBill)
            val dailyRate = if (durationDays > 0) units / durationDays else 0.0

            completedCycles.add(
                BillingCycleItem(
                    id = bill.id.ifBlank { "BILL-$i-${bill.assessmentDate}" },
                    periodLabel = periodLabel,
                    periodStartDate = priorBill?.assessmentDate.orEmpty(),
                    periodEndDate = bill.assessmentDate,
                    consumptionKwh = units,
                    durationDays = durationDays,
                    dailyRate = dailyRate,
                    source = BillingCycleSource.BILL,
                    status = BillingCycleStatus.COMPLETED
                )
            )
        }

        // Derive the Current Active Cycle immediately following the latest completed cycle
        val latestCompleted = completedCycles.lastOrNull()
        val nextPeriodLabel = if (latestCompleted != null) {
            deriveNextPeriod(latestCompleted.periodLabel)
        } else {
            "Aug–Oct"
        }

        val activeCycle = buildActiveCycle(nextPeriodLabel, baseline, readings, metrics)
        return completedCycles + activeCycle
    }

    private fun buildFallbackBaselineCycles(
        metrics: CurrentCycleEnergyMetrics?,
        baseline: MeterCycleBaseline?,
        readings: List<MeterReadingRecord>
    ): List<BillingCycleItem> {
        // Prompt conceptual baseline:
        // Feb–Apr: 92 kWh
        // Apr–Jun: 27 kWh
        // Jun–Aug: 55 kWh
        // Aug–Oct: Current active cycle (42 kWh live from daily readings, or live user metrics)
        val cycle1 = BillingCycleItem(
            id = "DEMO-1",
            periodLabel = "Feb–Apr",
            consumptionKwh = 92.0,
            durationDays = 60,
            dailyRate = 92.0 / 60.0,
            source = BillingCycleSource.ESTIMATED,
            status = BillingCycleStatus.COMPLETED
        )
        val cycle2 = BillingCycleItem(
            id = "DEMO-2",
            periodLabel = "Apr–Jun",
            consumptionKwh = 27.0,
            durationDays = 60,
            dailyRate = 27.0 / 60.0,
            source = BillingCycleSource.ESTIMATED,
            status = BillingCycleStatus.COMPLETED
        )
        val cycle3 = BillingCycleItem(
            id = "DEMO-3",
            periodLabel = "Jun–Aug",
            consumptionKwh = 55.0,
            durationDays = 60,
            dailyRate = 55.0 / 60.0,
            source = BillingCycleSource.ESTIMATED,
            status = BillingCycleStatus.COMPLETED
        )

        val activeCycle = buildActiveCycle(
            periodLabel = "Aug–Oct",
            baseline = baseline,
            readings = readings,
            metrics = metrics,
            fallbackConsumption = 170.0,
            fallbackDays = 40
        )

        return listOf(cycle1, cycle2, cycle3, activeCycle)
    }

    fun calculateCycleConsumptionFromReadings(
        readings: List<MeterReadingRecord>,
        baseline: MeterCycleBaseline?
    ): Pair<Double, Boolean> {
        if (readings.isEmpty()) {
            return Pair(0.0, false)
        }

        val sorted = readings.sortedBy { it.readingDate }
        val openingReading = when {
            baseline != null && baseline.baselineReading > 0.0 -> baseline.baselineReading
            else -> sorted.first().readingValue
        }
        val latestReading = sorted.last().readingValue

        val consumed = max(0.0, latestReading - openingReading)
        return Pair(consumed, true)
    }

    fun resolveCycleStartAndEnd(
        periodLabel: String,
        referenceDate: Date = Date()
    ): Pair<Date, Date> {
        val cal = Calendar.getInstance()
        cal.time = referenceDate
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        val parts = periodLabel.trim().split("–", "-")
        if (parts.size == 2) {
            val startMonthIdx = parseMonthIndex(parts[0].trim())
            val endMonthIdx = parseMonthIndex(parts[1].trim())

            if (startMonthIdx >= 0 && endMonthIdx >= 0) {
                var startYear = currentYear
                if (startMonthIdx > endMonthIdx) {
                    if (currentMonth in 0..endMonthIdx) {
                        startYear = currentYear - 1
                    }
                } else if (currentMonth < startMonthIdx) {
                    startYear = currentYear - 1
                }

                val startCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, startYear)
                    set(Calendar.MONTH, startMonthIdx)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val endYear = if (startMonthIdx > endMonthIdx) startYear + 1 else startYear
                val endCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, endYear)
                    set(Calendar.MONTH, endMonthIdx)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, maxDay)
                }

                return Pair(startCal.time, endCal.time)
            }
        }

        val end = referenceDate
        val start = Date(end.time - (60L * 24 * 60 * 60 * 1000))
        return Pair(start, end)
    }

    fun calculateElapsedDays(
        periodLabel: String,
        baselineDateStr: String?,
        currentDate: Date = Date(),
        cycleLengthDays: Int = 60
    ): Int {
        val (cycleStart, cycleEnd) = resolveCycleStartAndEnd(periodLabel, currentDate)
        val parsedBaseline = baselineDateStr?.let { parseDate(it) }

        // elapsedDays must be calculated only inside the current cycle.
        // It must never accidentally count days from an older billing cycle.
        val effectiveStart = if (parsedBaseline != null &&
            !parsedBaseline.before(cycleStart) &&
            !parsedBaseline.after(cycleEnd)
        ) {
            parsedBaseline
        } else {
            cycleStart
        }

        val targetEnd = if (currentDate.before(effectiveStart)) {
            effectiveStart
        } else if (currentDate.after(cycleEnd)) {
            cycleEnd
        } else {
            currentDate
        }

        val diffMs = targetEnd.time - effectiveStart.time
        val rawDays = max(1, (diffMs / (1000L * 60 * 60 * 24)).toInt())
        return rawDays.coerceIn(1, cycleLengthDays)
    }

    fun buildActiveCycle(
        periodLabel: String,
        baseline: MeterCycleBaseline?,
        readings: List<MeterReadingRecord>,
        metrics: CurrentCycleEnergyMetrics?,
        fallbackConsumption: Double = 0.0,
        fallbackDays: Int = 1
    ): BillingCycleItem {
        val sortedReadings = readings.sortedBy { it.readingDate }
        val latestRecord = sortedReadings.lastOrNull()

        val (consumedFromReadings, hasReadingsFromList) = calculateCycleConsumptionFromReadings(readings, baseline)

        val (consumedKwh, hasReadings) = when {
            hasReadingsFromList -> Pair(consumedFromReadings, true)
            metrics != null && metrics.unitsConsumed > 0.0 -> Pair(metrics.unitsConsumed, true)
            fallbackConsumption > 0.0 -> Pair(fallbackConsumption, true)
            else -> Pair(0.0, false)
        }

        val openingReading = when {
            baseline != null && baseline.baselineReading > 0.0 -> baseline.baselineReading
            sortedReadings.isNotEmpty() -> sortedReadings.first().readingValue
            else -> 42120.0
        }

        val currentReadingVal = latestRecord?.readingValue ?: (openingReading + consumedKwh)

        val latestDate = latestRecord?.let { parseDate(it.readingDate) } ?: Date()
        val elapsedDays = calculateElapsedDays(
            periodLabel = periodLabel,
            baselineDateStr = baseline?.baselineDate,
            currentDate = latestDate,
            cycleLengthDays = 60
        )

        val dailyRate = if (elapsedDays > 0) consumedKwh / elapsedDays else 0.0

        return BillingCycleItem(
            id = "CURRENT-${periodLabel.replace("–", "-")}",
            periodLabel = periodLabel,
            periodStartDate = baseline?.baselineDate.orEmpty(),
            periodEndDate = "Present",
            consumptionKwh = consumedKwh,
            durationDays = elapsedDays,
            dailyRate = dailyRate,
            source = BillingCycleSource.DAILY_READINGS,
            status = BillingCycleStatus.CURRENT,
            openingMeterReading = openingReading,
            latestMeterReading = currentReadingVal,
            hasMeterReadings = hasReadings
        )
    }

    /**
     * Compares any selected cycle with its immediately preceding chronological cycle.
     */
    fun compareCycles(
        selectedCycle: BillingCycleItem,
        previousCycle: BillingCycleItem?
    ): BillingCycleComparison {
        if (previousCycle == null) {
            return BillingCycleComparison(
                selectedCycle = selectedCycle,
                previousCycle = null,
                state = BillingComparisonState.COMPLETED_NO_CHANGE,
                differenceUnits = 0.0,
                savedUnits = 0.0,
                savedCredits = 0,
                primaryMessage = "Initial Recorded Cycle",
                secondaryMessage = "${selectedCycle.consumptionKwh.toInt()} kWh",
                comparisonNote = "First available billing period",
                isConsumptionIncreased = false
            )
        }

        if (selectedCycle.status == BillingCycleStatus.CURRENT) {
            return compareCurrentActiveCycle(selectedCycle, previousCycle)
        }

        return compareCompletedCycles(selectedCycle, previousCycle)
    }

    private fun compareCurrentActiveCycle(
        current: BillingCycleItem,
        previous: BillingCycleItem
    ): BillingCycleComparison {
        if (!current.hasMeterReadings) {
            return BillingCycleComparison(
                selectedCycle = current,
                previousCycle = previous,
                state = BillingComparisonState.CURRENT_NO_READINGS,
                differenceUnits = 0.0,
                savedUnits = 0.0,
                savedCredits = 0,
                primaryMessage = "Record your meter reading to track this cycle",
                secondaryMessage = "Opening reading: ${current.openingMeterReading?.toInt() ?: 0} kWh",
                comparisonNote = "Log daily readings on Home to track consumption in real-time",
                rateComparisonText = "",
                isConsumptionIncreased = false
            )
        }

        val cUnits = current.consumptionKwh
        val pUnits = previous.consumptionKwh
        val cRate = current.dailyRate
        val pRate = previous.dailyRate
        val rateText = String.format(Locale.US, "%.2f kWh/day vs %.2f kWh/day", cRate, pRate)

        // 1. ABSOLUTE ACCUMULATED CONSUMPTION COMPARISON (PRIMARY RULE):
        // Cumulative meter reading consumption within a cycle can only increase.
        // If current accumulated consumption >= previous cycle consumption:
        // status = CURRENT_HIGHER (or HIGHER)
        // difference = currentConsumption - previousCycleConsumption
        // Daily rate must NEVER override this absolute comparison.
        if (cUnits >= pUnits) {
            val excess = cUnits - pUnits
            return BillingCycleComparison(
                selectedCycle = current,
                previousCycle = previous,
                state = BillingComparisonState.CURRENT_HIGHER,
                differenceUnits = excess,
                savedUnits = 0.0,
                savedCredits = 0,
                primaryMessage = "↑ +${excess.toInt()} kWh • Consumption Increased",
                secondaryMessage = "${cUnits.toInt()} kWh consumed so far • Based on daily meter readings",
                comparisonNote = if (excess > 0.0) {
                    "Already +${excess.toInt()} kWh higher than previous cycle (${previous.periodLabel})"
                } else {
                    "Already equal to previous cycle total (${previous.periodLabel})"
                },
                rateComparisonText = rateText,
                isConsumptionIncreased = true
            )
        }

        // 2. IF CURRENT CONSUMPTION IS BELOW PREVIOUS CYCLE:
        // Do NOT immediately claim "saved" because current cycle is still active.
        // Identify it as currently below previous cycle and explain cycle is in progress.
        val diff = pUnits - cUnits
        return BillingCycleComparison(
            selectedCycle = current,
            previousCycle = previous,
            state = BillingComparisonState.CURRENT_LOWER,
            differenceUnits = diff,
            savedUnits = 0.0,
            savedCredits = 0,
            primaryMessage = "Currently below previous cycle",
            secondaryMessage = "${cUnits.toInt()} kWh consumed so far • Based on daily meter readings",
            comparisonNote = "${diff.toInt()} kWh below previous cycle • Current cycle still in progress",
            rateComparisonText = rateText,
            isConsumptionIncreased = false
        )
    }

    private fun compareCompletedCycles(
        current: BillingCycleItem,
        previous: BillingCycleItem
    ): BillingCycleComparison {
        val cUnits = current.consumptionKwh
        val pUnits = previous.consumptionKwh
        val diff = pUnits - cUnits

        val isHistorical = (current.source == BillingCycleSource.BILL || current.source == BillingCycleSource.ESTIMATED)

        return when {
            diff > 0.0 -> {
                val saved = diff
                val credits = if (isHistorical) 0 else saved.roundToInt()
                BillingCycleComparison(
                    selectedCycle = current,
                    previousCycle = previous,
                    state = BillingComparisonState.COMPLETED_SAVED,
                    differenceUnits = saved,
                    savedUnits = saved,
                    savedCredits = credits,
                    primaryMessage = "↓ ${saved.toInt()} kWh • Saved",
                    secondaryMessage = if (credits > 0) "+$credits Energy Credits" else "Historical Baseline",
                    comparisonNote = "${saved.toInt()} kWh lower than ${previous.periodLabel}",
                    rateComparisonText = "",
                    isConsumptionIncreased = false
                )
            }
            diff < 0.0 -> {
                val excess = abs(diff)
                BillingCycleComparison(
                    selectedCycle = current,
                    previousCycle = previous,
                    state = BillingComparisonState.COMPLETED_INCREASED,
                    differenceUnits = excess,
                    savedUnits = 0.0,
                    savedCredits = 0,
                    primaryMessage = "↑ +${excess.toInt()} kWh • Consumption Increased",
                    secondaryMessage = "No Energy Credits",
                    comparisonNote = "Consumption increased by ${excess.toInt()} kWh compared with the previous cycle",
                    rateComparisonText = "",
                    isConsumptionIncreased = true
                )
            }
            else -> {
                BillingCycleComparison(
                    selectedCycle = current,
                    previousCycle = previous,
                    state = BillingComparisonState.COMPLETED_NO_CHANGE,
                    differenceUnits = 0.0,
                    savedUnits = 0.0,
                    savedCredits = 0,
                    primaryMessage = "→ 0 kWh • No Change",
                    secondaryMessage = "No Energy Credits",
                    comparisonNote = "0 kWh difference compared with the previous cycle",
                    rateComparisonText = "",
                    isConsumptionIncreased = false
                )
            }
        }
    }

    /**
     * Energy Credits must only be awarded from verified consumption reduction occurring
     * after SolarEase onboarding. Historical billing data is for baseline/analytics only
     * and MUST NOT be converted into Energy Credits.
     */
    fun calculateVerifiedBillCredits(cycles: List<BillingCycleItem>): Int {
        return 0
    }

    /**
     * Dynamically derives the next bi-monthly period label without hard-coding.
     * e.g. "Jun–Aug" -> "Aug–Oct", "Aug–Oct" -> "Oct–Dec", "Oct–Dec" -> "Dec–Feb", etc.
     */
    fun deriveNextPeriod(currentPeriodLabel: String): String {
        val trimmed = currentPeriodLabel.trim()
        val parts = trimmed.split("–", "-")
        if (parts.size == 2) {
            val endMonthStr = parts[1].trim()
            val endMonthIdx = parseMonthIndex(endMonthStr)
            if (endMonthIdx >= 0) {
                val nextStartIdx = endMonthIdx
                val nextEndIdx = (endMonthIdx + 2) % 12
                return "${MONTH_NAMES[nextStartIdx]}–${MONTH_NAMES[nextEndIdx]}"
            }
        }
        return "Aug–Oct"
    }

    private fun parseMonthIndex(monthStr: String): Int {
        for (i in MONTH_NAMES.indices) {
            if (monthStr.startsWith(MONTH_NAMES[i], ignoreCase = true)) {
                return i
            }
        }
        return -1
    }

    fun formatBiMonthlyPeriod(rawPeriod: String, rawDate: String): String {
        val trimmed = rawPeriod.trim()
        if (trimmed.isNotBlank()) {
            val monthMatches = Regex("(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", RegexOption.IGNORE_CASE)
                .findAll(trimmed)
                .map { it.value.lowercase().replaceFirstChar { c -> c.uppercase() } }
                .toList()

            if (monthMatches.size >= 2) {
                return "${monthMatches[0]}–${monthMatches[1]}"
            } else if (monthMatches.size == 1 && (trimmed.contains("202") || !trimmed.contains("–"))) {
                // Preserves actual billing period representation (e.g. "Jun 2026" or "Jun")
                return trimmed
            }
        }

        val date = parseDate(rawDate.ifBlank { rawPeriod })
        val calendar = Calendar.getInstance()
        val month = if (date != null) {
            calendar.time = date
            calendar.get(Calendar.MONTH)
        } else {
            parseMonthIndex(trimmed)
        }

        return when (month) {
            0 -> "Nov–Jan"
            1 -> "Dec–Feb"
            2 -> "Jan–Mar"
            3 -> "Feb–Apr"
            4 -> "Mar–May"
            5 -> "Apr–Jun"
            6 -> "May–Jul"
            7 -> "Jun–Aug"
            8 -> "Jul–Sep"
            9 -> "Aug–Oct"
            10 -> "Sep–Nov"
            11 -> "Oct–Dec"
            else -> "Jun–Aug"
        }
    }

    internal fun extractCycleUnits(current: TnebBillRecord, prior: TnebBillRecord?): Double {
        val unitsVal = current.units.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        if (unitsVal != null && unitsVal > 0.0) {
            return unitsVal
        }

        val currKwh = current.kwh.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
        val prevKwh = prior?.kwh?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()

        if (currKwh != null && prevKwh != null && currKwh > prevKwh && (currKwh > 500.0 || prevKwh > 500.0)) {
            return currKwh - prevKwh
        }
        return unitsVal ?: 0.0
    }

    internal fun calculateCycleDays(current: TnebBillRecord, prior: TnebBillRecord?): Int {
        if (prior != null) {
            val currDate = parseDate(current.assessmentDate.ifBlank { current.entryDate })
            val priorDate = parseDate(prior.assessmentDate.ifBlank { prior.entryDate })
            if (currDate != null && priorDate != null) {
                val diffMs = currDate.time - priorDate.time
                val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                if (days in 25..120) {
                    return days
                }
            }
        }
        return 60
    }

    private fun calculateDaysBetween(startDateStr: String, endDateStr: String): Int {
        val start = parseDate(startDateStr)
        val end = parseDate(endDateStr) ?: Date()
        if (start != null) {
            val diffMs = end.time - start.time
            return max(1, (diffMs / (1000 * 60 * 60 * 24)).toInt())
        }
        return 1
    }

    private fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val formats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("dd.MM.yyyy", Locale.US),
            SimpleDateFormat("dd MMM yyyy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US)
        )
        for (f in formats) {
            try {
                val d = f.parse(dateStr.trim())
                if (d != null) return d
            } catch (_: Exception) {}
        }
        return null
    }
}
