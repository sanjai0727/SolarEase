package com.example.feature.recommends.data

import android.content.Context
import android.content.SharedPreferences
import com.example.feature.recommends.model.MantraItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MantraPreferences {
    private const val PREFS_NAME = "SolarEaseMantraPrefs"
    private const val KEY_CHECKED_IDS = "checked_mantra_ids"
    private const val KEY_CHECK_IN_TIMES_PREFIX = "mantra_time_"
    private const val KEY_STREAK_DAYS = "streak_days"
    private const val KEY_EARNED_CREDITS = "earned_energy_credits"
    private const val KEY_MANTRAS_EXPANDED_DATE = "mantras_expanded_date"
    private const val KEY_MANTRAS_IS_EXPANDED = "mantras_is_expanded"
    private const val KEY_MANTRAS_CHECKED_DATE = "mantras_checked_date"

    val defaultDefinitions = listOf(
        MantraItem(
            id = 1,
            number = "01",
            title = "Switch off equipment after use",
            description = "Turn off lights, fans, televisions and other appliances when leaving a room.",
            potentialSavings = "~50 Wh/day potential saving",
            frequencyBadge = "Daily",
            iconEmoji = "💡",
            iconBgColorHex = 0xFFFFFBEB, // amber-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 2,
            number = "02",
            title = "Regular cleaning of AC filters & using natural lighting",
            description = "Keep AC filters clean for better airflow efficiency and use daylight during the day.",
            potentialSavings = "~100 Wh/day potential saving",
            frequencyBadge = "Weekly",
            iconEmoji = "📺",
            iconBgColorHex = 0xFFECFEFF, // cyan-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 3,
            number = "03",
            title = "Use star-rated equipment",
            description = "Upgrade to high-efficiency, BEE star-rated appliances to lower electricity bills.",
            potentialSavings = "Up to 25% lower consumption",
            frequencyBadge = "One-time",
            iconEmoji = "⭐",
            iconBgColorHex = 0xFFFFFBEB, // amber-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 4,
            number = "04",
            title = "Usage of LED study/utility lamps",
            description = "Replace conventional bulbs with energy-efficient LED alternatives.",
            potentialSavings = "~180 Wh/day potential saving",
            frequencyBadge = "Daily",
            iconEmoji = "💡",
            iconBgColorHex = 0xFFFEFCE8, // yellow-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 5,
            number = "05",
            title = "Regular defrosting of the refrigerator",
            description = "Prevent ice build-up, ensure proper sealing and maintain optimal temperature.",
            potentialSavings = "~250 Wh/day potential saving",
            frequencyBadge = "Monthly",
            iconEmoji = "🧊",
            iconBgColorHex = 0xFFEFF6FF, // blue-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 6,
            number = "06",
            title = "Usage of heavy equipment in off-peak hours",
            description = "Operate high-load appliances like water pumps, washing machines outside peak hours.",
            potentialSavings = "~175 Wh/day potential saving",
            frequencyBadge = "Daily",
            iconEmoji = "🧺",
            iconBgColorHex = 0xFFF0FDFA, // teal-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 7,
            number = "07",
            title = "Avoid leaving equipment in standby mode",
            description = "Unplug chargers, set-top boxes and electronics when not in active use.",
            potentialSavings = "~50 Wh/day potential saving",
            frequencyBadge = "Daily",
            iconEmoji = "🔌",
            iconBgColorHex = 0xFFFFF7ED, // orange-50
            isCheckedIn = false,
            isOneTimeAction = false
        ),
        MantraItem(
            id = 8,
            number = "08",
            title = "Replace frequently used lighting with Solar LED",
            description = "Use solar-powered LED lighting for spaces with high daily lighting usage.",
            potentialSavings = null,
            frequencyBadge = "One-time",
            iconEmoji = "☀️",
            iconBgColorHex = 0xFFECFDF5, // emerald-50
            isCheckedIn = false,
            isOneTimeAction = true
        ),
        MantraItem(
            id = 9,
            number = "09",
            title = "Optimize cooking and water heating practices",
            description = "Use efficient cooking practices and maintain suitable eco-friendly water-heater temperatures.",
            potentialSavings = null,
            frequencyBadge = "Daily",
            iconEmoji = "🍲",
            iconBgColorHex = 0xFFFFF1F2, // rose-50
            isCheckedIn = false,
            isOneTimeAction = false
        )
    )

    private val _mantrasState = MutableStateFlow<List<MantraItem>>(defaultDefinitions)
    val mantrasState: StateFlow<List<MantraItem>> = _mantrasState.asStateFlow()

    private val _streakState = MutableStateFlow(0)
    val streakState: StateFlow<Int> = _streakState.asStateFlow()

    private val _mantrasExpandedState = MutableStateFlow(true)
    val mantrasExpandedState: StateFlow<Boolean> = _mantrasExpandedState.asStateFlow()

    private var appContext: Context? = null
    private var initialized = false

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun init(context: Context? = null) {
        if (context != null) {
            appContext = context.applicationContext
            if (!initialized) {
                initialized = true
                loadFromPrefs(appContext!!)
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadFromPrefs(context: Context, overrideToday: String? = null) {
        try {
            val prefs = getPrefs(context)
            loadExpansionState(prefs, overrideToday)
            loadCheckInState(prefs, overrideToday)
        } catch (_: Exception) {
            _mantrasState.value = defaultDefinitions
        }
    }

    private fun loadCheckInState(prefs: SharedPreferences, overrideToday: String? = null) {
        val today = overrideToday ?: getTodayDateString()
        val savedDate = prefs.getString(KEY_MANTRAS_CHECKED_DATE, null)
        if (savedDate == today) {
            val checkedSet = prefs.getStringSet(KEY_CHECKED_IDS, emptySet()) ?: emptySet()
            val loadedStreak = prefs.getInt(KEY_STREAK_DAYS, 0)
            _streakState.value = loadedStreak

            val updatedList = defaultDefinitions.map { item ->
                val isChecked = checkedSet.contains(item.id.toString())
                val savedTime = prefs.getString("$KEY_CHECK_IN_TIMES_PREFIX${item.id}", null)
                val timeDisplay = if (isChecked) {
                    savedTime ?: item.checkInTimeDisplay ?: "Today, checked in"
                } else null

                item.copy(
                    isCheckedIn = isChecked,
                    checkInTimeDisplay = timeDisplay
                )
            }
            _mantrasState.value = updatedList
        } else {
            // New local calendar day: reset check-ins to default
            _mantrasState.value = defaultDefinitions
            _streakState.value = 0
            val editor = prefs.edit()
                .putString(KEY_MANTRAS_CHECKED_DATE, today)
                .remove(KEY_CHECKED_IDS)
            for (item in defaultDefinitions) {
                editor.remove("$KEY_CHECK_IN_TIMES_PREFIX${item.id}")
            }
            editor.apply()
        }
    }

    private fun loadExpansionState(prefs: SharedPreferences, overrideToday: String? = null) {
        val today = overrideToday ?: getTodayDateString()
        val savedDate = prefs.getString(KEY_MANTRAS_EXPANDED_DATE, null)
        if (savedDate == today) {
            _mantrasExpandedState.value = prefs.getBoolean(KEY_MANTRAS_IS_EXPANDED, true)
        } else {
            // New local calendar day: reset to expanded by default
            _mantrasExpandedState.value = true
            prefs.edit()
                .putString(KEY_MANTRAS_EXPANDED_DATE, today)
                .putBoolean(KEY_MANTRAS_IS_EXPANDED, true)
                .apply()
        }
    }

    fun checkDateAndResetIfNewDay(context: Context? = null, overrideDate: String? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return
        try {
            val prefs = getPrefs(ctx)
            loadExpansionState(prefs, overrideDate)
            loadCheckInState(prefs, overrideDate)
        } catch (_: Exception) {}
    }

    fun toggleMantrasExpanded(context: Context? = null): Boolean {
        val ctx = context?.applicationContext ?: appContext
        val today = getTodayDateString()
        val newState = !_mantrasExpandedState.value
        _mantrasExpandedState.value = newState

        if (ctx != null) {
            try {
                getPrefs(ctx).edit()
                    .putString(KEY_MANTRAS_EXPANDED_DATE, today)
                    .putBoolean(KEY_MANTRAS_IS_EXPANDED, newState)
                    .apply()
            } catch (_: Exception) {}
        }
        return newState
    }

    fun setMantrasExpanded(context: Context? = null, isExpanded: Boolean, date: String? = null) {
        val ctx = context?.applicationContext ?: appContext
        val targetDate = date ?: getTodayDateString()
        _mantrasExpandedState.value = isExpanded

        if (ctx != null) {
            try {
                getPrefs(ctx).edit()
                    .putString(KEY_MANTRAS_EXPANDED_DATE, targetDate)
                    .putBoolean(KEY_MANTRAS_IS_EXPANDED, isExpanded)
                    .apply()
            } catch (_: Exception) {}
        }
    }

    fun getEarnedCredits(context: Context? = null): Int {
        val ctx = context?.applicationContext ?: appContext ?: return 0
        return try {
            getPrefs(ctx).getInt(KEY_EARNED_CREDITS, 0)
        } catch (_: Exception) {
            0
        }
    }

    fun addEarnedCredits(context: Context? = null, credits: Int) {
        val ctx = context?.applicationContext ?: appContext ?: return
        try {
            val prefs = getPrefs(ctx)
            val current = prefs.getInt(KEY_EARNED_CREDITS, 0)
            prefs.edit().putInt(KEY_EARNED_CREDITS, current + credits).apply()
        } catch (_: Exception) {}
    }

    fun toggleCheckIn(context: Context? = null, mantraId: Int, date: String? = null): Boolean {
        val currentList = _mantrasState.value.ifEmpty { defaultDefinitions }
        val target = currentList.find { it.id == mantraId } ?: return false

        val today = date ?: getTodayDateString()
        val newCheckedState = !target.isCheckedIn
        val newTimeDisplay = if (newCheckedState) {
            val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            "Today, $timeFmt"
        } else null

        val updatedList = currentList.map { item ->
            if (item.id == mantraId) {
                item.copy(
                    isCheckedIn = newCheckedState,
                    checkInTimeDisplay = newTimeDisplay
                )
            } else item
        }
        _mantrasState.value = updatedList

        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            try {
                val prefs = getPrefs(ctx)
                val checkedIds = updatedList.filter { it.isCheckedIn }.map { it.id.toString() }.toSet()
                val editor = prefs.edit()
                    .putString(KEY_MANTRAS_CHECKED_DATE, today)
                    .putStringSet(KEY_CHECKED_IDS, checkedIds)
                if (newCheckedState && newTimeDisplay != null) {
                    editor.putString("$KEY_CHECK_IN_TIMES_PREFIX$mantraId", newTimeDisplay)
                } else {
                    editor.remove("$KEY_CHECK_IN_TIMES_PREFIX$mantraId")
                }
                editor.apply()
            } catch (_: Exception) {}
        }
        return newCheckedState
    }

    fun resetToDefaults() {
        _mantrasState.value = defaultDefinitions
        _streakState.value = 0
        _mantrasExpandedState.value = true
        initialized = false
    }
}
