package com.dockeredly.app.browser.core

import android.content.Context
import android.net.Uri
import android.view.View
import com.dockeredly.app.domain.model.RenderEngine
import kotlinx.coroutines.flow.StateFlow

/** Live, observable state of a [BrowserEngine]'s current page. */
data class BrowserEngineState(
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: BrowserError? = null,
    val isPageFullscreen: Boolean = false,
)

data class BrowserError(
    val description: String,
    val isConnectivity: Boolean = false,
    val failingUrl: String? = null,
)

enum class BrowserPermissionKind { CAMERA, MICROPHONE, LOCATION }

/** A site's request for a sensitive permission; the host UI decides and calls [grant]/[deny]. */
interface BrowserPermissionRequest {
    val origin: String
    val kinds: List<BrowserPermissionKind>
    fun grant()
    fun deny()
}

/** A `<input type="file">` request; the host UI drives an Android picker and reports back. */
interface FileChooserRequest {
    val allowMultiple: Boolean
    val mimeTypes: List<String>
    fun onPicked(uris: List<Uri>)
    fun onCancelled()
}

data class DownloadRequest(
    val url: String,
    val userAgent: String,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long,
)

/** Callbacks a [BrowserEngine] surfaces up to whatever hosts it (the runtime activity). */
interface BrowserEngineCallbacks {
    fun onPermissionRequested(request: BrowserPermissionRequest) {}
    fun onFileChooserRequested(request: FileChooserRequest) {}
    fun onDownloadRequested(request: DownloadRequest) {}
    /** A navigation target the embedded engine can't or shouldn't render itself. */
    fun onExternalLinkRequested(url: String) {}
    /** HTML5 fullscreen video/element wants the whole screen; [view] is null when exiting. */
    fun onFullscreenViewRequested(view: View?) {}
}

/**
 * A generic embedded web rendering engine. The rest of the app talks only to this
 * interface, never to android.webkit or org.mozilla.geckoview types directly, so a third
 * engine could be added later without touching UI or domain code.
 *
 * Each instance is bound to one [profileId] for the lifetime it's used, and its browser
 * data (cookies, local storage, cache, IndexedDB) is expected to persist across
 * [detach]/[attach] cycles and process death - only [resetProfileData] destroys it.
 */
interface BrowserEngine {
    val engineType: RenderEngine
    val state: StateFlow<BrowserEngineState>

    var callbacks: BrowserEngineCallbacks?

    /** Returns the platform view to embed, creating it on first call. */
    fun getView(context: Context): View

    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun stop()

    fun onPause()
    fun onResume()

    /** Releases the platform view/runtime resources. Persistent profile data is untouched. */
    fun destroy()

    fun clearCache()

    /** Destructive: wipes cookies, storage, IndexedDB and cache for this web app's profile. */
    fun resetProfileData()
}
