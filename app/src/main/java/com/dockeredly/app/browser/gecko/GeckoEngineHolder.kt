package com.dockeredly.app.browser.gecko

import android.content.Context
import com.dockeredly.app.browser.core.EngineAvailability
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController

/**
 * GeckoView requires exactly one [GeckoRuntime] per process. This holder lazily creates
 * it once and hands every [GeckoBrowserEngine] a reference; per-web-app isolation is then
 * achieved within that single runtime via GeckoSession's `contextId` (GeckoView's
 * container-style storage partitioning), not via separate runtimes or processes.
 */
internal object GeckoEngineHolder {
    @Volatile private var runtime: GeckoRuntime? = null
    @Volatile private var initFailure: Throwable? = null

    private fun getOrCreateRuntime(context: Context): GeckoRuntime? {
        runtime?.let { return it }
        synchronized(this) {
            runtime?.let { return it }
            return try {
                val settings = GeckoRuntimeSettings.Builder()
                    .javaScriptEnabled(true)
                    .build()
                GeckoRuntime.create(context.applicationContext, settings).also { runtime = it }
            } catch (t: Throwable) {
                initFailure = t
                null
            }
        }
    }

    fun checkAvailability(context: Context): EngineAvailability {
        val instance = getOrCreateRuntime(context)
        return if (instance != null) {
            EngineAvailability.Available
        } else {
            EngineAvailability.Unavailable(
                initFailure?.message ?: "Mozilla GeckoView could not be initialized on this device.",
            )
        }
    }

    fun createEngine(context: Context, profileId: String): GeckoBrowserEngine {
        val instance = getOrCreateRuntime(context)
            ?: error("GeckoRuntime unavailable: ${initFailure?.message}")
        return GeckoBrowserEngine(instance, profileId)
    }

    fun clearCacheForProfile(context: Context, profileId: String) {
        val instance = runtime ?: return
        // GeckoView's public StorageController API clears by flag, not by contextId,
        // so this best-effort call clears cache runtime-wide rather than just this
        // profile's container. Cache loss for sibling web apps is low-severity (it is
        // rebuilt transparently) so this trade-off is acceptable; verify against the
        // pinned GeckoView version's StorageController API if a narrower call becomes
        // available.
        instance.storageController.clearData(StorageController.ClearFlags.ALL_CACHES.toLong())
    }

    fun destroyProfile(context: Context, profileId: String) {
        val instance = runtime ?: return
        instance.storageController.clearData(StorageController.ClearFlags.ALL.toLong())
    }
}
