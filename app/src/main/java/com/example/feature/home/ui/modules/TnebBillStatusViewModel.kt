package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class SubmitState {
    IDLE,
    SUBMITTING,
    SUCCESS,
    INVALID_CAPTCHA,
    INVALID_DETAILS,
    ERROR
}

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

    private val _submitState = MutableStateFlow(SubmitState.IDLE)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _submitTrigger = MutableStateFlow(0)
    val submitTrigger: StateFlow<Int> = _submitTrigger.asStateFlow()

    fun updateServiceConnectionNumber(number: String) {
        _serviceConnectionNumber.value = number
        validate()
    }

    fun updateRegisteredMobileNumber(number: String) {
        _registeredMobileNumber.value = number
        validate()
    }

    fun updateCaptchaInput(input: String) {
        _captchaInput.value = input
    }

    private fun validate() {
        val isValidScn = _serviceConnectionNumber.value.isNotBlank()
        val isValidRmn = _registeredMobileNumber.value.length == 10 && _registeredMobileNumber.value.all { it.isDigit() }
        _isValid.value = isValidScn && isValidRmn
    }

    fun checkBillStatus() {
        if (_isValid.value) {
            if (_captchaImageBase64.value == null) {
                // Phase 2A: Load TNPDCL and get CAPTCHA
                _isChecking.value = true
                _pageLoadError.value = false
                _submitState.value = SubmitState.IDLE
            } else if (_captchaInput.value.isNotEmpty()) {
                // Phase 2B: Submit form
                _submitState.value = SubmitState.SUBMITTING
                _submitTrigger.value++
            }
        }
    }

    fun resetValidationState() {
        _isChecking.value = false
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
    }

    fun onCaptchaReceived(base64Data: String) {
        // Strip the data URL prefix if present
        val cleanBase64 = if (base64Data.startsWith("data:image")) {
            base64Data.substringAfter("base64,")
        } else {
            base64Data
        }
        _captchaImageBase64.value = cleanBase64
    }

    fun onPageLoadError() {
        _pageLoadError.value = true
        _isChecking.value = true // Ensure error UI is shown
        if (_submitState.value == SubmitState.SUBMITTING) {
            _submitState.value = SubmitState.ERROR
        }
    }

    fun requestCaptchaRefresh() {
        // Clear current CAPTCHA to show loading state
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
        // Trigger the WebView to reload
        _reloadTrigger.value++
    }

    fun onSubmissionResult(result: String) {
        when (result) {
            "SUCCESS" -> _submitState.value = SubmitState.SUCCESS
            "INVALID_CAPTCHA" -> _submitState.value = SubmitState.INVALID_CAPTCHA
            "INVALID_DETAILS" -> _submitState.value = SubmitState.INVALID_DETAILS
            "ERROR" -> _submitState.value = SubmitState.ERROR
            else -> _submitState.value = SubmitState.ERROR
        }
    }
}
