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

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TnebBillStatusWebView(
    viewModel: TnebBillStatusViewModel,
    modifier: Modifier = Modifier
) {
    // Keep a single instance of WebView across recompositions
    val webView = rememberWebViewWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    var lastTrigger by remember { mutableIntStateOf(reloadTrigger) }

    val submitTrigger by viewModel.submitTrigger.collectAsState()
    var lastSubmitTrigger by remember { mutableIntStateOf(submitTrigger) }
    
    val submitState by viewModel.submitState.collectAsState()
    
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val captchaInput by viewModel.captchaInput.collectAsState()

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
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            
                            val currentState = viewModel.submitState.value
                            
                            if (currentState == SubmitState.SUBMITTING) {
                                // We just submitted the form. Check the resulting DOM for errors or success.
                                val checkResultJs = """
                                    javascript:(function() {
                                        try {
                                            var errorSummary = document.querySelector('.ui-messages-error-summary');
                                            var errorDetail = document.querySelector('.ui-messages-error-detail');
                                            var errorText = (errorSummary ? errorSummary.innerText : '') + ' ' + (errorDetail ? errorDetail.innerText : '');
                                            
                                            if (errorText) {
                                                if (errorText.toLowerCase().includes('captcha')) {
                                                    AndroidCaptchaBridge.onSubmissionResult('INVALID_CAPTCHA');
                                                } else {
                                                    AndroidCaptchaBridge.onSubmissionResult('INVALID_DETAILS');
                                                }
                                            } else {
                                                // If no error, assume success in Phase 2B. 
                                                // We can look for table elements as a strong indicator.
                                                var hasTable = document.querySelector('.ui-datatable') != null || document.querySelector('table') != null;
                                                if (hasTable || document.body.innerText.toLowerCase().includes('bill')) {
                                                    AndroidCaptchaBridge.onSubmissionResult('SUCCESS');
                                                } else {
                                                    AndroidCaptchaBridge.onSubmissionResult('ERROR');
                                                }
                                            }
                                        } catch(e) {
                                            AndroidCaptchaBridge.onSubmissionResult('ERROR');
                                        }
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(checkResultJs, null)
                            } else if (currentState == SubmitState.IDLE) {
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
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            // STRICT SSL RULE: Do NOT call handler.proceed().
                            handler?.cancel()
                            viewModel.onPageLoadError()
                        }
                    }

                    loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                }
            },
            update = { view ->
                // Reload the WebView exactly once per user refresh action
                if (reloadTrigger > lastTrigger) {
                    lastTrigger = reloadTrigger
                    view.reload()
                }
                
                // Submit the form
                if (submitTrigger > lastSubmitTrigger) {
                    lastSubmitTrigger = submitTrigger
                    // Safely escape values before injection to prevent XSS or JS injection attacks
                    val safeScn = serviceConnectionNumber.replace("'", "\\'").replace("\"", "\\\"")
                    val safeMob = registeredMobileNumber.replace("'", "\\'").replace("\"", "\\\"")
                    val safeCap = captchaInput.replace("'", "\\'").replace("\"", "\\\"")
                    
                    val submitJs = """
                        javascript:(function() {
                            try {
                                document.getElementById('serviceno').value = '$safeScn';
                                document.getElementById('mob').value = '$safeMob';
                                document.getElementById('cap').value = '$safeCap';
                                document.getElementById('submit3').click();
                            } catch(e) {
                                AndroidCaptchaBridge.onSubmissionResult('ERROR');
                            }
                        })();
                    """.trimIndent()
                    view.evaluateJavascript(submitJs, null)
                }
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
    fun onSubmissionResult(result: String) {
        viewModel.onSubmissionResult(result)
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

