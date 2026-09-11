package com.dockeredly.app.browser.core

import android.content.Context
import com.dockeredly.app.browser.chromium.ChromiumEngine
import com.dockeredly.app.browser.gecko.GeckoEngineHolder
import com.dockeredly.app.domain.model.RenderEngine

sealed interface EngineAvailability {
    data object Available : EngineAvailability
    data class Unavailable(val reason: String) : EngineAvailability
}

/** Creates [BrowserEngine] instances and checks whether an engine can actually initialize. */
object BrowserEngineFactory {

    fun checkAvailability(context: Context, engine: RenderEngine): EngineAvailability = when (engine) {
        RenderEngine.CHROMIUM -> ChromiumEngine.checkAvailability(context)
        RenderEngine.GECKO -> GeckoEngineHolder.checkAvailability(context)
    }

    /** [profileId] namespaces this web app's persistent, isolated browser storage. */
    fun create(context: Context, engine: RenderEngine, profileId: String): BrowserEngine = when (engine) {
        RenderEngine.CHROMIUM -> ChromiumEngine(context.applicationContext, profileId)
        RenderEngine.GECKO -> GeckoEngineHolder.createEngine(context.applicationContext, profileId)
    }

    fun clearCache(context: Context, engine: RenderEngine, profileId: String) = when (engine) {
        RenderEngine.CHROMIUM -> ChromiumEngine.clearCacheForProfile(context, profileId)
        RenderEngine.GECKO -> GeckoEngineHolder.clearCacheForProfile(context, profileId)
    }

    /** Destructive: wipes the on-disk profile for a web app that's being deleted or reset. */
    fun destroyProfile(context: Context, engine: RenderEngine, profileId: String) = when (engine) {
        RenderEngine.CHROMIUM -> ChromiumEngine.destroyProfile(context, profileId)
        RenderEngine.GECKO -> GeckoEngineHolder.destroyProfile(context, profileId)
    }
}
