package com.dockeredly.app.browser.chromium

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest as WebkitPermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dockeredly.app.browser.core.BrowserEngine
import com.dockeredly.app.browser.core.BrowserEngineCallbacks
import com.dockeredly.app.browser.core.BrowserEngineState
import com.dockeredly.app.browser.core.BrowserError
import com.dockeredly.app.browser.core.BrowserPermissionKind
import com.dockeredly.app.browser.core.BrowserPermissionRequest
import com.dockeredly.app.browser.core.DownloadRequest
import com.dockeredly.app.browser.core.EngineAvailability
import com.dockeredly.app.browser.core.FileChooserRequest
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.util.ExternalLinkClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [BrowserEngine] backed by Android System WebView (Chromium). Persistence relies on
 * WebView's normal on-disk cookie/storage database for whatever process this instance
 * lives in; per-web-app isolation is provided at the process level (see
 * [com.dockeredly.app.browser.core.BrowserProcessSlots]) rather than per-instance, since
 * WebView exposes no per-instance storage partitioning API.
 */
@SuppressLint("SetJavaScriptEnabled")
class ChromiumEngine(
    private val appContext: Context,
    private val profileId: String,
) : BrowserEngine {

    override val engineType: RenderEngine = RenderEngine.CHROMIUM

    private val _state = MutableStateFlow(BrowserEngineState())
    override val state: StateFlow<BrowserEngineState> = _state.asStateFlow()

    override var callbacks: BrowserEngineCallbacks? = null

    private var webView: WebView? = null

    override fun getView(context: Context): View {
        webView?.let { return it }

        val (shouldClearCache, shouldReset) = ChromiumProfileOps.consumePending(appContext, profileId)

        val view = WebView(context.applicationContext).apply {
            settings.applyStandardConfig()
            webViewClient = InnerWebViewClient()
            webChromeClient = InnerWebChromeClient()
            setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
                callbacks?.onDownloadRequested(
                    DownloadRequest(url, userAgent, contentDisposition, mimeType, contentLength),
                )
            }
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)

        if (shouldReset) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            view.clearCache(true)
            view.clearHistory()
            view.clearFormData()
        } else if (shouldClearCache) {
            view.clearCache(true)
        }

        webView = view
        return view
    }

    private fun WebSettings.applyStandardConfig() {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        mediaPlaybackRequiresUserGesture = false
        allowFileAccess = false
        allowContentAccess = true
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = WebSettings.LOAD_DEFAULT
    }

    override fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    override fun goBack(): Boolean {
        val view = webView ?: return false
        return if (view.canGoBack()) {
            view.goBack()
            true
        } else {
            false
        }
    }

    override fun goForward(): Boolean {
        val view = webView ?: return false
        return if (view.canGoForward()) {
            view.goForward()
            true
        } else {
            false
        }
    }

    override fun reload() {
        webView?.reload()
    }

    override fun stop() {
        webView?.stopLoading()
    }

    override fun onPause() {
        webView?.onPause()
    }

    override fun onResume() {
        webView?.onResume()
    }

    override fun destroy() {
        CookieManager.getInstance().flush()
        webView?.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = object : WebViewClient() {}
            destroy()
        }
        webView = null
    }

    override fun clearCache() {
        val view = webView
        if (view != null) {
            view.clearCache(true)
        } else {
            ChromiumProfileOps.markClearCache(appContext, profileId)
        }
    }

    override fun resetProfileData() {
        val view = webView
        if (view != null) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            view.clearCache(true)
            view.clearHistory()
            view.clearFormData()
        } else {
            ChromiumProfileOps.markReset(appContext, profileId)
        }
    }

    private inner class InnerWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
            val url = request.url.toString()
            if (ExternalLinkClassifier.shouldOpenExternally(url)) {
                callbacks?.onExternalLinkRequested(url)
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            _state.value = _state.value.copy(
                url = url.orEmpty(),
                isLoading = true,
                error = null,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            )
        }

        override fun onPageFinished(view: WebView, url: String?) {
            _state.value = _state.value.copy(
                url = url.orEmpty(),
                isLoading = false,
                canGoBack = view.canGoBack(),
                canGoForward = view.canGoForward(),
            )
        }

        override fun onReceivedError(
            view: WebView,
            request: android.webkit.WebResourceRequest,
            error: android.webkit.WebResourceError,
        ) {
            if (!request.isForMainFrame) return
            _state.value = _state.value.copy(
                isLoading = false,
                error = BrowserError(
                    description = error.description?.toString() ?: "Failed to load page",
                    isConnectivity = error.errorCode == ERROR_HOST_LOOKUP ||
                        error.errorCode == ERROR_CONNECT ||
                        error.errorCode == ERROR_TIMEOUT,
                    failingUrl = request.url.toString(),
                ),
            )
        }
    }

    private inner class InnerWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            _state.value = _state.value.copy(progress = newProgress, isLoading = newProgress < 100)
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            _state.value = _state.value.copy(title = title.orEmpty())
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            val request = object : FileChooserRequest {
                override val allowMultiple = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                override val mimeTypes: List<String> = fileChooserParams.acceptTypes?.filter { it.isNotBlank() } ?: emptyList()
                override fun onPicked(uris: List<Uri>) {
                    filePathCallback.onReceiveValue(uris.toTypedArray())
                }
                override fun onCancelled() {
                    filePathCallback.onReceiveValue(null)
                }
            }
            callbacks?.onFileChooserRequested(request)
            return true
        }

        override fun onPermissionRequest(request: WebkitPermissionRequest) {
            val kinds = request.resources.mapNotNull {
                when (it) {
                    WebkitPermissionRequest.RESOURCE_VIDEO_CAPTURE -> BrowserPermissionKind.CAMERA
                    WebkitPermissionRequest.RESOURCE_AUDIO_CAPTURE -> BrowserPermissionKind.MICROPHONE
                    else -> null
                }
            }
            if (kinds.isEmpty()) {
                request.deny()
                return
            }
            val wrapped = object : BrowserPermissionRequest {
                override val origin: String = request.origin.toString()
                override val kinds = kinds
                override fun grant() = request.grant(request.resources)
                override fun deny() = request.deny()
            }
            callbacks?.onPermissionRequested(wrapped)
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback,
        ) {
            val wrapped = object : BrowserPermissionRequest {
                override val origin: String = origin
                override val kinds = listOf(BrowserPermissionKind.LOCATION)
                override fun grant() = callback.invoke(origin, true, false)
                override fun deny() = callback.invoke(origin, false, false)
            }
            callbacks?.onPermissionRequested(wrapped)
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            callbacks?.onFullscreenViewRequested(view)
        }

        override fun onHideCustomView() {
            callbacks?.onFullscreenViewRequested(null)
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message,
        ): Boolean {
            // Popups load in the same web app window rather than spawning a second
            // engine instance, keeping one browser session per web app.
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            transport.webView = view
            resultMsg.sendToTarget()
            return true
        }
    }

    companion object {
        fun checkAvailability(context: Context): EngineAvailability = try {
            val pkg = WebView.getCurrentWebViewPackage()
            if (pkg == null) {
                EngineAvailability.Unavailable("No WebView provider is installed or enabled on this device.")
            } else {
                EngineAvailability.Available
            }
        } catch (t: Throwable) {
            EngineAvailability.Unavailable(t.message ?: "System WebView failed to initialize.")
        }

        fun clearCacheForProfile(context: Context, profileId: String) {
            ChromiumProfileOps.markClearCache(context, profileId)
        }

        fun destroyProfile(context: Context, profileId: String) {
            ChromiumProfileOps.markReset(context, profileId)
        }
    }
}
