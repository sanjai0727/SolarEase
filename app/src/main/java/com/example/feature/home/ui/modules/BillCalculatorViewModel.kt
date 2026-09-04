package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillCalculatorViewModel : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _billResult = MutableStateFlow<BillResult?>(null)
    val billResult: StateFlow<BillResult?> = _billResult.asStateFlow()

    fun onKeyPress(key: String) {
        val currentText = _inputText.value

        when (key) {
            "C", "Clear" -> {
                _inputText.value = ""
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
                // It's a digit
                if (currentText == "0") {
                    _inputText.value = key
                } else {
                    _inputText.value = currentText + key
                }
            }
        }
    }

    fun calculate() {
        val units = _inputText.value.toDoubleOrNull()
        if (units != null) {
            _billResult.value = TNEBBillCalculator.calculateTNEBBill(units)
        }
    }

    fun reset() {
        _billResult.value = null
        // Maintain the text value or clear it based on SolarEase standard, clearing is usually better for a fresh start.
        _inputText.value = ""
    }
}
