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

    private val _captchaImageBase64 = MutableStateFlow<String?>(null)
    val captchaImageBase64: StateFlow<String?> = _captchaImageBase64.asStateFlow()

    private val _captchaInput = MutableStateFlow("")
    val captchaInput: StateFlow<String> = _captchaInput.asStateFlow()

    private val _pageLoadError = MutableStateFlow(false)
    val pageLoadError: StateFlow<Boolean> = _pageLoadError.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    fun updateServiceConnectionNumber(number: String) {
        _serviceConnectionNumber.value = number
        validate()
    }

    fun updateRegisteredMobileNumber(number: String) {
        val digitsOnly = number.filter { it.isDigit() }
        _registeredMobileNumber.value = digitsOnly
        validate()
    }

    fun updateCaptchaInput(input: String) {
        _captchaInput.value = input.trim()
    }

    private fun validate() {
        val scn = _serviceConnectionNumber.value.trim()
        val rmn = _registeredMobileNumber.value.trim()

        val isValidScn = scn.isNotEmpty()
        val isValidRmn = rmn.length == 10 && rmn.all { it.isDigit() }

        _isValid.value = isValidScn && isValidRmn
    }

    fun checkBillStatus() {
        if (_isValid.value) {
            _isChecking.value = true
            _pageLoadError.value = false
            _captchaImageBase64.value = null
            _captchaInput.value = ""
        }
    }

    fun resetValidationState() {
        _isChecking.value = false
        _captchaImageBase64.value = null
        _pageLoadError.value = false
        _captchaInput.value = ""
    }

    fun onCaptchaReceived(base64: String) {
        // Strip the data URL prefix if present
        val cleanBase64 = if (base64.contains(",")) {
            base64.substringAfter(",")
        } else {
            base64
        }
        _captchaImageBase64.value = cleanBase64
    }

    fun onPageLoadError() {
        _pageLoadError.value = true
        _isChecking.value = false
        _captchaImageBase64.value = null
    }

    fun requestCaptchaRefresh() {
        // Clear current CAPTCHA to show loading state
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        // Trigger the WebView to reload
        _reloadTrigger.value++
    }
}
