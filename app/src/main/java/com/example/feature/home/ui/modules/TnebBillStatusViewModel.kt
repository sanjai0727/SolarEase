package com.example.feature.home.ui.modules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SubmitState {
    IDLE,
    SUBMITTING,
    SUCCESS,
    INVALID_CAPTCHA,
    INVALID_DETAILS,
    PARSING_ERROR,
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

    // Structured real TNEB bill data
    private val _billData = MutableStateFlow<TnebConsumerBillData?>(null)
    val billData: StateFlow<TnebConsumerBillData?> = _billData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentSubmissionId = 0
    private var captchaRefreshJob: kotlinx.coroutines.Job? = null

    init {
        val cached = TnebBillCache.billData.value
        if (cached != null) {
            _billData.value = cached
            if (cached.consumerNumber.isNotBlank()) {
                _serviceConnectionNumber.value = cached.consumerNumber.filter { it.isDigit() }.take(11)
            }
        } else {
            val user = com.example.core.session.UserSession.currentUser
            if (!user?.ebServiceNumber.isNullOrBlank()) {
                _serviceConnectionNumber.value = user.ebServiceNumber.filter { it.isDigit() }.take(11)
            }
            if (!user?.mobileNumber.isNullOrBlank()) {
                _registeredMobileNumber.value = user.mobileNumber.filter { it.isDigit() }.take(10)
            }
        }
        validate()
    }

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
                Log.d("TNEB", "[TNEB] CAPTCHA verified, submitting form...")
                Log.d("BillStatusDiagnostic", "SUBMISSION_STARTED")
                _errorMessage.value = null
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
        _errorMessage.value = null
        _billData.value = null
    }

    fun onCaptchaReceived(base64Data: String) {
        captchaRefreshJob?.cancel()
        val cleanBase64 = if (base64Data.startsWith("data:image")) {
            base64Data.substringAfter("base64,")
        } else {
            base64Data
        }
        _captchaImageBase64.value = cleanBase64
    }

    fun onPageLoadError() {
        Log.e("TNEB", "[TNEB] Page load error occurred in WebView")
        android.util.Log.d("BillStatusDiagnostic", "ON_PAGE_LOAD_ERROR called")
        captchaRefreshJob?.cancel()
        _pageLoadError.value = true
        _isChecking.value = true
        if (_submitState.value == SubmitState.SUBMITTING) {
            _errorMessage.value = "The official TNPDCL utility portal is temporarily unreachable."
            _submitState.value = SubmitState.ERROR
        }
    }

    fun requestCaptchaRefresh() {
        android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_REFRESH_STARTED")
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
        _errorMessage.value = null
        _isChecking.value = true

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

    /**
     * Primary callback: Receives structured JSON extracted directly from the live WebView DOM.
     */
    fun onParsedDataReceived(json: String, submissionId: Int) {
        Log.d("TNEB", "[TNEB] Received DOM-extracted JSON payload (length: ${json.length})")
        if (submissionId != currentSubmissionId) {
            Log.d("TNEB", "[TNEB] Stale DOM callback ignored. Expected: $currentSubmissionId, Got: $submissionId")
            return
        }

        val parsedData = TnebBillStatusHtmlParser.parseJson(json)
        if (parsedData != null && (parsedData.bills.isNotEmpty() || parsedData.consumerName.isNotBlank() || parsedData.tariffCode.isNotBlank())) {
            Log.d("TNEB", "[TNEB] UI received real bill data")
            _billData.value = parsedData
            TnebBillCache.save(null, parsedData)
            _errorMessage.value = null
            _submitState.value = SubmitState.SUCCESS
        } else {
            Log.w("TNEB", "[TNEB] JSON parse yielded insufficient data, awaiting or falling back to raw HTML")
        }
    }

    /**
     * Secondary / fallback callback: Receives the raw HTML response and parses it if JSON was not received or failed.
     */
    fun analyzeSubmissionResponse(html: String, submissionId: Int) {
        Log.d("TNEB", "[TNEB] HTML received, length: ${html.length}")
        if (submissionId != currentSubmissionId) {
            Log.d("TNEB", "[TNEB] Stale HTML callback ignored. Expected: $currentSubmissionId, Got: $submissionId")
            return
        }

        // If JSON extraction already succeeded and populated billData, skip duplicate processing
        if (_billData.value != null && _submitState.value == SubmitState.SUCCESS) {
            Log.d("TNEB", "[TNEB] Bill data already successfully populated from DOM extraction")
            return
        }

        val lowerHtml = html.lowercase()

        // 1. Check for specific PrimeFaces error markers (invalid captcha, invalid details)
        if (html.contains("ui-messages-error-summary") || html.contains("ui-messages-error-detail")) {
            val errorTextStart = html.indexOf("ui-messages-error-summary")
            val errorSnippet = if (errorTextStart != -1) {
                html.substring(errorTextStart, minOf(errorTextStart + 300, html.length))
            } else ""

            val detailTextStart = html.indexOf("ui-messages-error-detail")
            val detailSnippet = if (detailTextStart != -1) {
                html.substring(detailTextStart, minOf(detailTextStart + 300, html.length))
            } else ""

            val combinedSnippet = (errorSnippet + detailSnippet).lowercase()

            if (combinedSnippet.contains("captcha")) {
                Log.e("TNEB", "[TNEB] Portal returned INVALID_CAPTCHA")
                _errorMessage.value = "Invalid CAPTCHA code entered. Please try again."
                _submitState.value = SubmitState.INVALID_CAPTCHA
                return
            } else if (combinedSnippet.contains("invalid") || combinedSnippet.contains("mismatch") ||
                combinedSnippet.contains("does not match") || combinedSnippet.contains("not found")
            ) {
                Log.e("TNEB", "[TNEB] Portal returned INVALID_DETAILS")
                _errorMessage.value = "Invalid Service Connection Number or Registered Mobile Number."
                _submitState.value = SubmitState.INVALID_DETAILS
                return
            }
        }

        // 2. Check for bill page markers and parse HTML
        val isBillPage = lowerHtml.contains("consdetails") || lowerHtml.contains("consumption charges and collection details")
        if (isBillPage) {
            Log.d("TNEB", "[TNEB] Bill page detected in HTML response")
            val parsedData = TnebBillStatusHtmlParser.parseHtml(html)
            if (parsedData != null && (parsedData.bills.isNotEmpty() || parsedData.consumerName.isNotBlank() || parsedData.tariffCode.isNotBlank())) {
                Log.d("TNEB", "[TNEB] UI received real bill data")
                _billData.value = parsedData
                TnebBillCache.save(null, parsedData)
                _errorMessage.value = null
                _submitState.value = SubmitState.SUCCESS
            } else {
                Log.e("TNEB", "[TNEB] Parsing failed: Bill details could not be read from HTML")
                _errorMessage.value = "Bill details could not be read"
                _submitState.value = SubmitState.PARSING_ERROR
            }
        } else {
            Log.e("TNEB", "[TNEB] Parsing failed: Neither bill page nor standard error recognized")
            _errorMessage.value = "Bill details could not be read"
            _submitState.value = SubmitState.ERROR
        }
    }
}
