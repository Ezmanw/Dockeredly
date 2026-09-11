package com.dockeredly.app.runtime

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dockeredly.app.R
import com.dockeredly.app.browser.core.BrowserEngine
import com.dockeredly.app.browser.core.BrowserEngineCallbacks
import com.dockeredly.app.browser.core.BrowserEngineFactory
import com.dockeredly.app.browser.core.BrowserPermissionKind
import com.dockeredly.app.browser.core.BrowserPermissionRequest
import com.dockeredly.app.browser.core.DownloadRequest
import com.dockeredly.app.browser.core.EngineAvailability
import com.dockeredly.app.browser.core.FileChooserRequest
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.ui.theme.DockeredlyTheme
import com.dockeredly.app.util.DownloadHandler
import com.dockeredly.app.util.ExternalLinkClassifier

/**
 * Hosts one web app fullscreen, chrome-free, with no address bar or tabs - the standalone
 * runtime described in the app's core concept. Never instantiated directly; launched via
 * one of the `WebAppRuntimeActivitySlotN` subclasses (see [WebAppLauncher] and
 * [com.dockeredly.app.browser.core.BrowserProcessSlots] for why).
 */
open class WebAppRuntimeActivity : ComponentActivity() {

    private var engine: BrowserEngine? = null
    private var fullscreenContainer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val webAppName = intent.getStringExtra(WebAppLauncher.EXTRA_NAME).orEmpty()
        val webAppUrl = intent.getStringExtra(WebAppLauncher.EXTRA_URL).orEmpty()
        val profileId = intent.getStringExtra(WebAppLauncher.EXTRA_PROFILE_ID).orEmpty()
        val engineType = intent.readEngineExtra()

        setContent {
            DockeredlyTheme {
                RuntimeScreen(
                    engineType = engineType,
                    profileId = profileId,
                    url = webAppUrl,
                    title = webAppName,
                    onCreateEngine = { createdEngine -> engine = createdEngine },
                    onRequestFullscreen = { view -> showFullscreenOverlay(view) },
                    onExit = { finish() },
                )
            }
        }
    }

    private fun showFullscreenOverlay(view: android.view.View?) {
        if (view == null) {
            fullscreenContainer?.let { (window.decorView as ViewGroup).removeView(it) }
            fullscreenContainer = null
            return
        }
        val container = FrameLayout(this).apply {
            addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        (window.decorView as ViewGroup).addView(
            container,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        fullscreenContainer = container
    }

    override fun onPause() {
        super.onPause()
        engine?.onPause()
    }

    override fun onResume() {
        super.onResume()
        engine?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.destroy()
    }
}

@Composable
private fun RuntimeScreen(
    engineType: RenderEngine,
    profileId: String,
    url: String,
    title: String,
    onCreateEngine: (BrowserEngine) -> Unit,
    onRequestFullscreen: (android.view.View?) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val availability = remember(engineType) { BrowserEngineFactory.checkAvailability(context, engineType) }

    if (availability is EngineAvailability.Unavailable) {
        EngineUnavailableScreen(engineType = engineType, reason = availability.reason, onExit = onExit)
        return
    }

    val engine = remember(profileId, engineType) {
        BrowserEngineFactory.create(context, engineType, profileId).also(onCreateEngine)
    }
    val state by engine.state.collectAsStateWithLifecycle()

    var pendingPermissionRequest by remember { mutableStateOf<BrowserPermissionRequest?>(null) }
    var pendingFileChooserRequest by remember { mutableStateOf<FileChooserRequest?>(null) }
    val onExitState = rememberUpdatedState(onExit)

    val osPermissionLauncher = rememberLauncherForMultiplePermissions { granted ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (request != null) {
            if (granted.values.all { it }) request.grant() else request.deny()
        }
    }

    val fileChooserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        val request = pendingFileChooserRequest
        pendingFileChooserRequest = null
        if (uris.isNotEmpty()) request?.onPicked(uris) else request?.onCancelled()
    }

    DisposableEffect(engine) {
        engine.callbacks = object : BrowserEngineCallbacks {
            override fun onPermissionRequested(request: BrowserPermissionRequest) {
                val osPermissions = request.kinds.mapNotNull { it.toAndroidPermission() }
                if (osPermissions.isEmpty()) {
                    request.deny()
                    return
                }
                pendingPermissionRequest = request
                osPermissionLauncher.launch(osPermissions.toTypedArray())
            }

            override fun onFileChooserRequested(request: FileChooserRequest) {
                pendingFileChooserRequest = request
                fileChooserLauncher.launch(request.mimeTypes.toTypedArray().ifEmpty { arrayOf("*/*") })
            }

            override fun onDownloadRequested(request: DownloadRequest) {
                DownloadHandler.enqueue(context, request)
            }

            override fun onExternalLinkRequested(url: String) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }

            override fun onFullscreenViewRequested(view: android.view.View?) {
                onRequestFullscreen(view)
            }
        }
        engine.loadUrl(url)
        onDispose { engine.callbacks = null }
    }

    BackHandler(enabled = true) {
        if (!engine.goBack()) onExitState.value()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { engine.getView(it) },
            modifier = Modifier.fillMaxSize(),
        )

        if (state.isLoading) {
            LinearProgressIndicator(
                progress = { state.progress / 100f },
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            )
        }

        if (state.isLoading && state.url.isBlank()) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.runtime_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }

        state.error?.let { error ->
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = if (error.isConnectivity) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = if (error.isConnectivity) {
                            stringResource(R.string.runtime_error_no_connection)
                        } else {
                            stringResource(R.string.runtime_error_title)
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Button(onClick = { engine.reload() }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineUnavailableScreen(engineType: RenderEngine, reason: String, onExit: () -> Unit) {
    val titleRes = if (engineType == RenderEngine.CHROMIUM) {
        R.string.engine_unavailable_chromium_title
    } else {
        R.string.engine_unavailable_gecko_title
    }
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleLarge)
            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onExit) { Text(stringResource(R.string.action_close)) }
        }
    }
}

@Composable
private fun rememberLauncherForMultiplePermissions(
    onResult: (Map<String, Boolean>) -> Unit,
) = androidx.activity.compose.rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
    onResult = onResult,
)

private fun BrowserPermissionKind.toAndroidPermission(): String? = when (this) {
    BrowserPermissionKind.CAMERA -> android.Manifest.permission.CAMERA
    BrowserPermissionKind.MICROPHONE -> android.Manifest.permission.RECORD_AUDIO
    BrowserPermissionKind.LOCATION -> android.Manifest.permission.ACCESS_FINE_LOCATION
}
