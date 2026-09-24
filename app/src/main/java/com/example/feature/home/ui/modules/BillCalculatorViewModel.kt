package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class CalculatorScreenState {
    ENTRY,
    CALCULATING,
    RESULT,
    HISTORY
}

enum class BillingCycle(val displayName: String, val days: Int) {
    MONTHLY("Monthly", 30),
    BIMONTHLY("Bi-monthly", 60)
}

data class SavedCalculation(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val units: Double,
    val billResult: BillResult,
    val cycle: BillingCycle
)

class BillCalculatorViewModel : ViewModel() {

    private val _screenState = MutableStateFlow(CalculatorScreenState.ENTRY)
    val screenState: StateFlow<CalculatorScreenState> = _screenState.asStateFlow()

    private val _billingCycle = MutableStateFlow(BillingCycle.BIMONTHLY)
    val billingCycle: StateFlow<BillingCycle> = _billingCycle.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    private val _billResult = MutableStateFlow<BillResult?>(null)
    val billResult: StateFlow<BillResult?> = _billResult.asStateFlow()

    private val _calculationStep = MutableStateFlow(1)
    val calculationStep: StateFlow<Int> = _calculationStep.asStateFlow()

    private val _savedCalculations = MutableStateFlow<List<SavedCalculation>>(emptyList())
    val savedCalculations: StateFlow<List<SavedCalculation>> = _savedCalculations.asStateFlow()

    private var calculationJob: Job? = null

    fun onInputChanged(text: String) {
        val sanitized = text.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            _inputText.value = sanitized
            if (_inputError.value != null && sanitized.isNotEmpty()) {
                _inputError.value = null
            }
        }
    }

    fun selectPreset(units: Double) {
        _inputText.value = if (units % 1.0 == 0.0) units.toInt().toString() else units.toString()
        _inputError.value = null
    }

    fun clearInput() {
        _inputText.value = ""
        _inputError.value = null
    }

    fun setBillingCycle(cycle: BillingCycle) {
        _billingCycle.value = cycle
    }

    fun onKeyPress(key: String) {
        val currentText = _inputText.value

        when (key) {
            "C", "Clear" -> {
                _inputText.value = ""
                _inputError.value = null
            }
            "⌫", "Backspace" -> {
                if (currentText.isNotEmpty()) {
                    _inputText.value = currentText.dropLast(1)
                }
            }
            "." -> {
                if (!currentText.contains(".")) {
                    _inputText.value = if (currentText.isEmpty()) "0." else "$currentText."
                }
            }
            else -> {
                if (currentText == "0") {
                    _inputText.value = key
                } else {
                    _inputText.value = currentText + key
                }
            }
        }
        if (_inputError.value != null && _inputText.value.isNotEmpty()) {
            _inputError.value = null
        }
    }

    fun calculate() {
        val units = _inputText.value.toDoubleOrNull()
        if (units == null || units < 0.0) {
            _inputError.value = "Please enter valid electricity units."
            return
        }

        _inputError.value = null
        calculationJob?.cancel()
        calculationJob = viewModelScope.launch {
            _screenState.value = CalculatorScreenState.CALCULATING
            _calculationStep.value = 1
            delay(500)
            _calculationStep.value = 2
            delay(600)
            _calculationStep.value = 3
            delay(500)

            val result = TNEBBillCalculator.calculateTNEBBill(units)
            _billResult.value = result

            val newRecord = SavedCalculation(
                units = units,
                billResult = result,
                cycle = _billingCycle.value
            )
            _savedCalculations.value = listOf(newRecord) + _savedCalculations.value

            _screenState.value = CalculatorScreenState.RESULT
        }
    }

    fun cancelCalculation() {
        calculationJob?.cancel()
        _screenState.value = CalculatorScreenState.ENTRY
    }

    fun navigateToHistory() {
        _screenState.value = CalculatorScreenState.HISTORY
    }

    fun navigateToEntry() {
        _screenState.value = CalculatorScreenState.ENTRY
    }

    fun recalculate() {
        _screenState.value = CalculatorScreenState.ENTRY
    }

    fun openSavedResult(saved: SavedCalculation) {
        _billResult.value = saved.billResult
        _inputText.value = if (saved.units % 1.0 == 0.0) saved.units.toInt().toString() else saved.units.toString()
        _billingCycle.value = saved.cycle
        _screenState.value = CalculatorScreenState.RESULT
    }

    fun deleteHistoryItem(id: String) {
        _savedCalculations.value = _savedCalculations.value.filter { it.id != id }
    }

    fun clearHistory() {
        _savedCalculations.value = emptyList()
    }

    fun reset() {
        calculationJob?.cancel()
        _billResult.value = null
        _inputText.value = ""
        _inputError.value = null
        _screenState.value = CalculatorScreenState.ENTRY
    }
}
