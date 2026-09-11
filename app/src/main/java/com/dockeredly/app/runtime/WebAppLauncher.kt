package com.dockeredly.app.runtime

import android.content.Context
import android.content.Intent
import com.dockeredly.app.browser.core.BrowserProcessSlots
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.WebApp

object WebAppLauncher {
    const val EXTRA_WEB_APP_ID = "web_app_id"
    const val EXTRA_NAME = "name"
    const val EXTRA_URL = "url"
    const val EXTRA_ENGINE = "engine"
    const val EXTRA_PROFILE_ID = "profile_id"

    private val slotActivityClasses = listOf(
        WebAppRuntimeActivitySlot0::class.java,
        WebAppRuntimeActivitySlot1::class.java,
        WebAppRuntimeActivitySlot2::class.java,
        WebAppRuntimeActivitySlot3::class.java,
        WebAppRuntimeActivitySlot4::class.java,
        WebAppRuntimeActivitySlot5::class.java,
        WebAppRuntimeActivitySlot6::class.java,
        WebAppRuntimeActivitySlot7::class.java,
    )

    fun launch(context: Context, webApp: WebApp) {
        val slot = BrowserProcessSlots.slotForProfile(webApp.profileId)
        val activityClass = slotActivityClasses[slot]
        val intent = Intent(context, activityClass).apply {
            putExtra(EXTRA_WEB_APP_ID, webApp.id)
            putExtra(EXTRA_NAME, webApp.name)
            putExtra(EXTRA_URL, webApp.url)
            putExtra(EXTRA_ENGINE, webApp.engine.name)
            putExtra(EXTRA_PROFILE_ID, webApp.profileId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK.takeIf { context !is android.app.Activity } ?: 0)
        }
        context.startActivity(intent)
    }
}

fun Intent.readEngineExtra(): RenderEngine =
    getStringExtra(WebAppLauncher.EXTRA_ENGINE)?.let { runCatching { RenderEngine.valueOf(it) }.getOrNull() }
        ?: RenderEngine.default()
