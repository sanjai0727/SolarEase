package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TnebBillStatusViewModel : ViewModel() {

    private val _serviceConnectionNumber = MutableStateFlow("")
    val serviceConnectionNumber: StateFlow<String> = _serviceConnectionNumber.asStateFlow()

    private val _registeredMobileNumber = MutableStateFlow("")
    val registeredMobileNumber: StateFlow<String> = _registeredMobileNumber.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    fun updateServiceConnectionNumber(number: String) {
        _serviceConnectionNumber.value = number
        validate()
    }

    fun updateRegisteredMobileNumber(number: String) {
        // Only allow digits for mobile number, max 10 chars usually, but let's just keep it simple and filter to digits
        val digitsOnly = number.filter { it.isDigit() }
        _registeredMobileNumber.value = digitsOnly
        validate()
    }

    private fun validate() {
        val scn = _serviceConnectionNumber.value.trim()
        val rmn = _registeredMobileNumber.value.trim()

        val isValidScn = scn.isNotEmpty()
        // Basic Indian mobile number validation (starts with 6-9, 10 digits) or just 10 digits
        val isValidRmn = rmn.length == 10 && rmn.all { it.isDigit() }

        _isValid.value = isValidScn && isValidRmn
    }

    fun checkBillStatus() {
        if (_isValid.value) {
            val input = BillStatusInput(
                serviceConnectionNumber = _serviceConnectionNumber.value.trim(),
                registeredMobileNumber = _registeredMobileNumber.value.trim()
            )
            // Phase 1: Set temporary loading/validation state
            _isChecking.value = true
        }
    }

    fun resetValidationState() {
        _isChecking.value = false
    }
}
