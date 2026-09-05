package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    
    private var currentSubmissionId = 0
    private var captchaRefreshJob: kotlinx.coroutines.Job? = null

    fun updateServiceConnectionNumber(number: String) {
        val filtered = number.filter { it.isDigit() }.take(11)
        _serviceConnectionNumber.value = filtered
        validate()
    }

    fun updateRegisteredMobileNumber(number: String) {
        val filtered = number.filter { it.isDigit() }.take(10)
        _registeredMobileNumber.value = filtered
        validate()
    }

    fun updateCaptchaInput(input: String) {
        _captchaInput.value = input
    }

    private fun validate() {
        val isValidScn = _serviceConnectionNumber.value.length == 11
        val isValidRmn = _registeredMobileNumber.value.length == 10
        _isValid.value = isValidScn && isValidRmn
    }

    fun checkBillStatus() {
        if (_isValid.value) {
            if (_captchaImageBase64.value == null) {
                // Phase 2A: Load TNPDCL and get CAPTCHA
                requestCaptchaRefresh()
            } else if (_captchaInput.value.isNotEmpty()) {
                // Phase 2B: Submit form
                android.util.Log.d("BillStatusDiagnostic", "SUBMISSION_STARTED")
                _submitState.value = SubmitState.SUBMITTING
                currentSubmissionId++
                _submitTrigger.value = currentSubmissionId
            }
        }
    }

    fun resetValidationState() {
        captchaRefreshJob?.cancel()
        _isChecking.value = false
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
    }

    fun onCaptchaReceived(base64Data: String) {
        captchaRefreshJob?.cancel()
        // Strip the data URL prefix if present
        val cleanBase64 = if (base64Data.startsWith("data:image")) {
            base64Data.substringAfter("base64,")
        } else {
            base64Data
        }
        _captchaImageBase64.value = cleanBase64
    }

    fun onPageLoadError() {
        android.util.Log.d("BillStatusDiagnostic", "ON_PAGE_LOAD_ERROR called")
        captchaRefreshJob?.cancel()
        _pageLoadError.value = true
        _isChecking.value = true // Ensure error UI is shown
        if (_submitState.value == SubmitState.SUBMITTING) {
            android.util.Log.d("BillStatusDiagnostic", "SUBMISSION_ERROR due to page load error")
            _submitState.value = SubmitState.ERROR
        }
    }

    fun requestCaptchaRefresh() {
        android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_REFRESH_STARTED")
        // Clear current CAPTCHA to show loading state
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
        _isChecking.value = true
        
        // Trigger the WebView to reload
        _reloadTrigger.value++
        android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_RELOAD_TRIGGERED: ${_reloadTrigger.value}")
        
        captchaRefreshJob?.cancel()
        captchaRefreshJob = viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 seconds timeout
            if (_captchaImageBase64.value == null) {
                android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_REFRESH_TIMEOUT reached")
                onPageLoadError()
            }
        }
    }

    fun analyzeSubmissionResponse(html: String, submissionId: Int) {
        android.util.Log.d("BillStatusDiagnostic", "ANALYSIS_STARTED with HTML length: ${html.length}")
        if (submissionId != currentSubmissionId) {
            android.util.Log.d("BillStatusDiagnostic", "STALE_CALLBACK ignored. Expected: $currentSubmissionId, Got: $submissionId")
            return
        }

        // Simple native parsing to avoid JS quirks
        val lowerHtml = html.lowercase()
        
        // Check for specific PrimeFaces error markers that indicate invalid details
        if (html.contains("ui-messages-error-summary") || html.contains("ui-messages-error-detail")) {
            // We must extract the actual error text to ensure it's not just an empty container
            val errorTextStart = html.indexOf("ui-messages-error-summary")
            val errorSnippet = if (errorTextStart != -1) {
                html.substring(errorTextStart, minOf(errorTextStart + 300, html.length))
            } else ""
            
            val detailTextStart = html.indexOf("ui-messages-error-detail")
            val detailSnippet = if (detailTextStart != -1) {
                html.substring(detailTextStart, minOf(detailTextStart + 300, html.length))
            } else ""

            val combinedSnippet = (errorSnippet + detailSnippet).lowercase()
            
            // Check if there is actual error text content
            if (combinedSnippet.contains("captcha")) {
                android.util.Log.d("BillStatusDiagnostic", "ANALYSIS_RESULT_INVALID_CAPTCHA")
                _submitState.value = SubmitState.INVALID_CAPTCHA
                return
            } else if (combinedSnippet.contains("invalid") || combinedSnippet.contains("mismatch") || combinedSnippet.contains("does not match") || combinedSnippet.contains("not found")) {
                android.util.Log.d("BillStatusDiagnostic", "ANALYSIS_RESULT_INVALID_DETAILS")
                _submitState.value = SubmitState.INVALID_DETAILS
                return
            }
        }

        // Look for reliable success indicators in the HTML
        // A successful response generates a bill detail datatable or panel.
        // The confirmed structural marker for the TNPDCL success page is 'consdetails'.
        val isSuccess = lowerHtml.contains("consdetails")
                        
        if (isSuccess) {
            android.util.Log.d("BillStatusDiagnostic", "ANALYSIS_RESULT_SUCCESS")
            _submitState.value = SubmitState.SUCCESS
        } else {
            // If we didn't find clear errors or clear success, assume it's the TNPDCL "Connection Failed" or similar page.
            // As per requirements, do NOT show a SolarEase error, transition to dashboard with placeholders.
            android.util.Log.d("BillStatusDiagnostic", "ANALYSIS_RESULT_DASHBOARD (Fallback)")
            _submitState.value = SubmitState.SUCCESS
        }
    }
}
