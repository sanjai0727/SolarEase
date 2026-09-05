package com.example.feature.home.ui.modules

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TnebBillStatusWebView(
    viewModel: TnebBillStatusViewModel,
    modifier: Modifier = Modifier
) {
    // Keep a single instance of WebView across recompositions
    val webView = rememberWebViewWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    val submitTrigger by viewModel.submitTrigger.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val captchaInput by viewModel.captchaInput.collectAsState()

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_RELOAD_STARTED")
            webView.loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
        }
    }

    LaunchedEffect(submitTrigger) {
        if (submitTrigger > 0) {
            android.util.Log.d("BillStatusDiagnostic", "SUBMIT_TRIGGER_RECEIVED")
            val safeScn = serviceConnectionNumber.replace("'", "\\'").replace("\"", "\\\"")
            val safeMob = registeredMobileNumber.replace("'", "\\'").replace("\"", "\\\"")
            val safeCap = captchaInput.replace("'", "\\'").replace("\"", "\\\"")
            
            android.util.Log.d("BillStatusDiagnostic", "SUBMIT_JS_STARTED")
            
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://www.tnebltd.gov.in")
            val cookieList = cookies?.split(";")?.map { it.substringBefore("=").trim() } ?: emptyList()
            val hasJSessionId = cookieList.contains("JSESSIONID")
            val fingerprint = cookies?.hashCode()?.toString(16) ?: "null"
            android.util.Log.d("BillStatusDiagnostic", "--- SESSION DIAGNOSTICS ---\nCOOKIE_COUNT=${cookieList.size}\nJSESSIONID_PRESENT=$hasJSessionId\nCOOKIE_NAMES=${cookieList.joinToString()}\nSESSION_FINGERPRINT=$fingerprint")
            
            val submitJs = """
                javascript:(function() {
                    try {
                        var diag = [];
                        diag.push('CURRENT_URL=' + window.location.href);
                        diag.push('FORM_COUNT=' + document.forms.length);
                        var f = document.forms[0];
                        if (f) {
                            diag.push('FORM_ID=' + f.id);
                            diag.push('FORM_ACTION=' + f.action);
                            diag.push('FORM_METHOD=' + f.method);
                        }
                        diag.push('SERVICENO_PRESENT=' + (document.getElementById('serviceno') != null));
                        diag.push('MOB_PRESENT=' + (document.getElementById('mob') != null));
                        diag.push('CAP_PRESENT=' + (document.getElementById('cap') != null));
                        var subBtn = document.getElementById('submit3');
                        diag.push('SUBMIT3_PRESENT=' + (subBtn != null));
                        if (subBtn) {
                            diag.push('SUBMIT_TAG=' + subBtn.tagName);
                            diag.push('SUBMIT_TYPE=' + subBtn.type);
                            diag.push('SUBMIT_FORM_ID=' + (subBtn.form ? subBtn.form.id : 'none'));
                            diag.push('SUBMIT_DISABLED=' + subBtn.disabled);
                            diag.push('SUBMIT_ONCLICK_PRESENT=' + (subBtn.onclick != null));
                        }
                        diag.push('VIEWSTATE_PRESENT=' + (document.getElementsByName('javax.faces.ViewState').length > 0));
                        var hidden = [];
                        var inputs = document.getElementsByTagName('input');
                        for (var i = 0; i < inputs.length; i++) {
                            if (inputs[i].type == 'hidden') hidden.push(inputs[i].name);
                        }
                        diag.push('HIDDEN_FIELD_NAMES=' + hidden.join(','));
                        
                        if (f) {
                            f.addEventListener('submit', function(e) {
                                AndroidCaptchaBridge.onDiagnosticLog('FORM_SUBMIT_EVENT_RECEIVED=true\nFORM_SUBMIT_ACTION=' + f.action);
                            });
                        }
                        AndroidCaptchaBridge.onDiagnosticLog("--- PRE-SUBMISSION DIAGNOSTICS ---\n" + diag.join('\n'));

                        document.getElementById('serviceno').value = '$safeScn';
                        document.getElementById('mob').value = '$safeMob';
                        document.getElementById('cap').value = '$safeCap';
                        if (subBtn) {
                            console.log('BillStatusDiagnostic: SUBMIT_BUTTON_FOUND');
                            subBtn.click();
                            console.log('BillStatusDiagnostic: SUBMIT_CLICK_EXECUTED');
                        } else {
                            console.log('BillStatusDiagnostic: SUBMIT_BUTTON_NOT_FOUND');
                        }
                    } catch(e) {
                        console.log('BillStatusDiagnostic: SUBMIT_JS_EXCEPTION ' + e.toString());
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(submitJs, null)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                webView.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Security: We do not disable SSL validation.

                    addJavascriptInterface(
                        AndroidCaptchaBridge(viewModel),
                        "AndroidCaptchaBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            android.util.Log.d("BillStatusDiagnostic", "PAGE_STARTED: ${url?.take(50)}")
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("BillStatusDiagnostic", "PAGE_FINISHED: ${url.take(50)}")
                            
                            val currentState = viewModel.submitState.value
                            
                            if (currentState == SubmitState.SUBMITTING) {
                                val currentSubmissionId = viewModel.submitTrigger.value
                                android.util.Log.d("BillStatusDiagnostic", "HTML_EXTRACTION_STARTED")
                                // We just submitted the form. Pass the resulting HTML to the native analyzer.
                                val checkResultJs = """
                                    javascript:(function() {
                                        try {
                                            var diag = [];
                                            diag.push('DOC_TITLE=' + document.title);
                                            diag.push('DOC_URL=' + window.location.href);
                                            diag.push('BODY_LENGTH=' + (document.body ? document.body.innerText.length : 0));
                                            diag.push('HTML_LENGTH=' + document.documentElement.outerHTML.length);
                                            diag.push('FORM_COUNT=' + document.forms.length);
                                            for (var i = 0; i < document.forms.length; i++) {
                                                diag.push('FORM_' + i + '_ID=' + document.forms[i].id);
                                            }
                                            var tables = document.getElementsByTagName('table');
                                            diag.push('TABLE_COUNT=' + tables.length);
                                            for (var i = 0; i < tables.length; i++) {
                                                diag.push('TABLE_' + i + '_ID=' + tables[i].id + ' CLASS=' + tables[i].className);
                                            }
                                            diag.push('MARKER_consDetails=' + (document.getElementById('consDetails') != null));
                                            diag.push('MARKER_ui-datatable=' + (document.querySelector('.ui-datatable') != null));
                                            diag.push('MARKER_billData=' + (document.getElementById('billData') != null));
                                            diag.push('MARKER_billStatus=' + (document.getElementById('billStatus') != null));
                                            diag.push('MARKER_javax.faces.ViewState=' + (document.getElementsByName('javax.faces.ViewState').length > 0));
                                            diag.push('MARKER_primefaces=' + document.documentElement.outerHTML.toLowerCase().includes('primefaces'));
                                            
                                            var labels = ['Consumer Name', 'Consumer No', 'Service No', 'Service Connection', 'Bill Amount', 'Amount Payable', 'Receipt No', 'Receipt Date', 'Bill Date', 'Current Reading', 'Previous Reading', 'Consumption', 'Units', 'Tariff', 'Due Date', 'Status'];
                                            var bodyText = document.body ? document.body.innerText : '';
                                            for (var i = 0; i < labels.length; i++) {
                                                diag.push('LABEL_' + labels[i].replace(/ /g, '_') + '=' + bodyText.includes(labels[i]));
                                            }
                                            
                                            var errors = ['.ui-messages-error', '.ui-messages-error-summary', '.ui-messages-error-detail'];
                                            for (var i = 0; i < errors.length; i++) {
                                                var els = document.querySelectorAll(errors[i]);
                                                diag.push('ERR_' + errors[i] + '_EXISTS=' + (els.length > 0));
                                                if (els.length > 0) {
                                                    var txt = els[0].innerText.trim();
                                                    diag.push('ERR_' + errors[i] + '_NON_EMPTY=' + (txt.length > 0));
                                                    diag.push('ERR_' + errors[i] + '_LENGTH=' + txt.length);
                                                    var cls = 'NONE';
                                                    if (txt.toLowerCase().includes('captcha')) cls = 'CAPTCHA_ERROR';
                                                    else if (txt.toLowerCase().includes('invalid') || txt.toLowerCase().includes('match')) cls = 'INVALID_DETAILS_ERROR';
                                                    else if (txt.length > 0) cls = 'OTHER_ERROR';
                                                    diag.push('ERR_CLASS=' + cls);
                                                }
                                            }
                                            AndroidCaptchaBridge.onDiagnosticLog("--- RESULT DOM DIAGNOSTICS ---\n" + diag.join('\n'));
                                        } catch (e) {
                                            AndroidCaptchaBridge.onDiagnosticLog("RESULT_DIAG_ERROR=" + e.toString());
                                        }
                                        var html = document.documentElement.outerHTML;
                                        AndroidCaptchaBridge.onHtmlResponseReceived(html, $currentSubmissionId);
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(checkResultJs, null)
                            } else if (currentState == SubmitState.IDLE) {
                                val cookieManager = android.webkit.CookieManager.getInstance()
                                val cookies = cookieManager.getCookie("https://www.tnebltd.gov.in")
                                val fingerprint = cookies?.hashCode()?.toString(16) ?: "null"
                                android.util.Log.d("BillStatusDiagnostic", "--- CAPTCHA SESSION DIAGNOSTICS ---\nCAPTCHA_SESSION_FINGERPRINT=$fingerprint")
                                
                                // Inject JavaScript to extract the CAPTCHA image
                                val js = """
                                    javascript:(function() {
                                        function processCaptcha(img) {
                                            try {
                                                var canvas = document.createElement('canvas');
                                                canvas.width = img.naturalWidth || img.width || 120;
                                                canvas.height = img.naturalHeight || img.height || 40;
                                                var ctx = canvas.getContext('2d');
                                                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                                                var dataURL = canvas.toDataURL('image/png');
                                                AndroidCaptchaBridge.onCaptchaExtracted(dataURL);
                                            } catch(e) {
                                                // Handle cross-origin issues or errors silently as per security rules
                                            }
                                        }
    
                                        var img = document.getElementById('imgCaptchaId');
                                        if (img) {
                                            if (img.complete && img.naturalWidth > 0) {
                                                processCaptcha(img);
                                            } else {
                                                img.onload = function() { processCaptcha(img); };
                                            }
                                        }
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(js, null)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                android.util.Log.d("BillStatusDiagnostic", "WEBVIEW_ERROR: ${error?.errorCode}")
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame == true) {
                                android.util.Log.d("BillStatusDiagnostic", "HTTP_ERROR: ${errorResponse?.statusCode}")
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            android.util.Log.d("BillStatusDiagnostic", "SSL_ERROR: ${error?.primaryError}")
                            // STRICT SSL RULE: Do NOT call handler.proceed().
                            handler?.cancel()
                            viewModel.onPageLoadError()
                        }
                    }

                    loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                }
            },
            update = { view ->
                // Handled imperatively via LaunchedEffect above
            }
        )
    }
}

/**
 * JavaScript interface to pass the CAPTCHA base64 string to the ViewModel.
 */
class AndroidCaptchaBridge(private val viewModel: TnebBillStatusViewModel) {
    @JavascriptInterface
    fun onCaptchaExtracted(base64Data: String) {
        viewModel.onCaptchaReceived(base64Data)
    }
    
    @JavascriptInterface
    fun onDiagnosticLog(log: String) {
        android.util.Log.d("BillStatusDiagnostic", "\n$log")
    }

    @JavascriptInterface
    fun onHtmlResponseReceived(html: String, submissionId: Int) {
        android.util.Log.d("BillStatusDiagnostic", "HTML_BRIDGE_RECEIVED, length: ${html.length}")
        viewModel.analyzeSubmissionResponse(html, submissionId)
    }
}

/**
 * Creates and remembers a WebView, cleaning it up when it leaves the composition.
 */
@Composable
fun rememberWebViewWithLifecycle(): WebView {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webView = remember {
        WebView(context)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    return webView
}

