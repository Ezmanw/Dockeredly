package com.dockeredly.app.browser.gecko

import android.content.Context
import android.net.Uri
import android.view.View
import com.dockeredly.app.browser.core.BrowserEngine
import com.dockeredly.app.browser.core.BrowserEngineCallbacks
import com.dockeredly.app.browser.core.BrowserEngineState
import com.dockeredly.app.browser.core.BrowserError
import com.dockeredly.app.browser.core.BrowserPermissionKind
import com.dockeredly.app.browser.core.BrowserPermissionRequest
import com.dockeredly.app.browser.core.DownloadRequest
import com.dockeredly.app.browser.core.FileChooserRequest
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.util.ExternalLinkClassifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError

/**
 * [BrowserEngine] backed by Mozilla GeckoView. Each instance uses [profileId] as its
 * GeckoSession `contextId`, GeckoView's supported mechanism for partitioning cookies and
 * storage per "container" within a single shared [GeckoRuntime] - this gives Gecko web
 * apps real per-instance isolation, unlike the process-level approximation Chromium needs
 * (see [com.dockeredly.app.browser.chromium.ChromiumEngine]).
 *
 * JavaScript dialogs (alert/confirm/prompt) are dismissed immediately rather than shown
 * with custom UI; this is a deliberate, documented gap rather than a silent one.
 */
class GeckoBrowserEngine(
    private val runtime: GeckoRuntime,
    private val profileId: String,
) : BrowserEngine {

    override val engineType: RenderEngine = RenderEngine.GECKO

    private val _state = MutableStateFlow(BrowserEngineState())
    override val state: StateFlow<BrowserEngineState> = _state.asStateFlow()

    override var callbacks: BrowserEngineCallbacks? = null

    private var geckoView: GeckoView? = null
    private var session: GeckoSession? = null

    override fun getView(context: Context): View {
        geckoView?.let { return it }

        val sessionSettings = GeckoSessionSettings.Builder()
            .contextId(profileId)
            .usePrivateMode(false)
            .build()

        val newSession = GeckoSession(sessionSettings).apply {
            navigationDelegate = InnerNavigationDelegate()
            progressDelegate = InnerProgressDelegate()
            contentDelegate = InnerContentDelegate()
            permissionDelegate = InnerPermissionDelegate()
            promptDelegate = InnerPromptDelegate()
            open(runtime)
        }
        session = newSession

        val view = GeckoView(context.applicationContext).apply {
            setSession(newSession)
        }
        geckoView = view
        return view
    }

    override fun loadUrl(url: String) {
        session?.load(GeckoSession.Loader().uri(url))
    }

    override fun goBack(): Boolean {
        val s = session ?: return false
        if (!_state.value.canGoBack) return false
        s.goBack()
        return true
    }

    override fun goForward(): Boolean {
        val s = session ?: return false
        if (!_state.value.canGoForward) return false
        s.goForward()
        return true
    }

    override fun reload() {
        session?.reload()
    }

    override fun stop() {
        session?.stop()
    }

    override fun onPause() {
        session?.setActive(false)
    }

    override fun onResume() {
        session?.setActive(true)
    }

    override fun destroy() {
        geckoView?.releaseSession()
        session?.close()
        session = null
        geckoView = null
    }

    override fun clearCache() {
        // See the caveat in GeckoEngineHolder.clearCacheForProfile: this clears cache
        // runtime-wide, not just this profile's container.
        runtime.storageController.clearData(
            org.mozilla.geckoview.StorageController.ClearFlags.ALL_CACHES.toLong(),
        )
    }

    override fun resetProfileData() {
        runtime.storageController.clearData(org.mozilla.geckoview.StorageController.ClearFlags.ALL.toLong())
    }

    private inner class InnerNavigationDelegate : GeckoSession.NavigationDelegate {
        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            _state.value = _state.value.copy(canGoBack = canGoBack)
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            _state.value = _state.value.copy(canGoForward = canGoForward)
        }

        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean,
        ) {
            _state.value = _state.value.copy(url = url.orEmpty(), error = null)
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest,
        ): GeckoResult<AllowOrDeny> {
            return if (ExternalLinkClassifier.shouldOpenExternally(request.uri)) {
                callbacks?.onExternalLinkRequested(request.uri)
                GeckoResult.fromValue(AllowOrDeny.DENY)
            } else {
                GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }
        }

        override fun onLoadError(
            session: GeckoSession,
            uri: String?,
            error: WebRequestError,
        ): GeckoResult<String>? {
            _state.value = _state.value.copy(
                isLoading = false,
                error = BrowserError(
                    description = "Failed to load page (code ${error.code})",
                    isConnectivity = error.category == WebRequestError.ERROR_CATEGORY_NETWORK,
                    failingUrl = uri,
                ),
            )
            return null
        }
    }

    private inner class InnerProgressDelegate : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            _state.value = _state.value.copy(isLoading = true, progress = 0, error = null, url = url)
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            _state.value = _state.value.copy(isLoading = false, progress = 100)
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            _state.value = _state.value.copy(progress = progress)
        }
    }

    private inner class InnerContentDelegate : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            _state.value = _state.value.copy(title = title.orEmpty())
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            _state.value = _state.value.copy(isPageFullscreen = fullScreen)
            callbacks?.onFullscreenViewRequested(if (fullScreen) geckoView else null)
        }

        override fun onExternalResponse(session: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
            callbacks?.onDownloadRequested(
                DownloadRequest(
                    url = response.uri,
                    userAgent = "",
                    contentDisposition = response.headers?.get("Content-Disposition"),
                    mimeType = response.headers?.get("Content-Type"),
                    contentLength = response.body?.available()?.toLong() ?: -1L,
                ),
            )
        }

        override fun onCrash(session: GeckoSession) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = BrowserError(description = "The page content process crashed."),
            )
        }
    }

    private inner class InnerPermissionDelegate : GeckoSession.PermissionDelegate {
        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback,
        ) {
            // Actual OS runtime-permission prompting is driven by the host Activity;
            // grant here only reflects that the site itself is allowed to ask - the
            // system permission dialog is a separate, OS-owned step.
            callback.grant()
        }

        override fun onContentPermissionRequest(
            session: GeckoSession,
            perm: GeckoSession.PermissionDelegate.ContentPermission,
        ): GeckoResult<Int> {
            if (perm.permission != GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION) {
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            }
            val result = GeckoResult<Int>()
            val wrapped = object : BrowserPermissionRequest {
                override val origin: String = perm.uri
                override val kinds = listOf(BrowserPermissionKind.LOCATION)
                override fun grant() {
                    result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
                }
                override fun deny() {
                    result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
                }
            }
            callbacks?.onPermissionRequested(wrapped) ?: result.complete(
                GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY,
            )
            return result
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession,
            uri: String,
            video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback,
        ) {
            val kinds = buildList {
                if (video != null) add(BrowserPermissionKind.CAMERA)
                if (audio != null) add(BrowserPermissionKind.MICROPHONE)
            }
            if (kinds.isEmpty()) {
                callback.reject()
                return
            }
            val wrapped = object : BrowserPermissionRequest {
                override val origin: String = uri
                override val kinds = kinds
                override fun grant() = callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                override fun deny() = callback.reject()
            }
            callbacks?.onPermissionRequested(wrapped) ?: callback.reject()
        }
    }

    private inner class InnerPromptDelegate : GeckoSession.PromptDelegate {
        override fun onFilePrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.FilePrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
            val request = object : FileChooserRequest {
                override val allowMultiple =
                    prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
                override val mimeTypes: List<String> = prompt.mimeTypes?.toList() ?: emptyList()
                override fun onPicked(uris: List<Uri>) {
                    result.complete(prompt.confirm(geckoView?.context ?: return, uris.toTypedArray()))
                }
                override fun onCancelled() {
                    result.complete(prompt.dismiss())
                }
            }
            callbacks?.onFileChooserRequested(request) ?: result.complete(prompt.dismiss())
            return result
        }

        override fun onAlertPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.AlertPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> = GeckoResult.fromValue(prompt.dismiss())

        override fun onButtonPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.ButtonPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> = GeckoResult.fromValue(prompt.dismiss())

        override fun onTextPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.TextPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> = GeckoResult.fromValue(prompt.dismiss())

        override fun onPopupPrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.PopupPrompt,
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> =
            GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))
    }
}
