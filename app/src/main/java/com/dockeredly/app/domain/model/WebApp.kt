package com.dockeredly.app.domain.model

/**
 * A single user-created web app: a website bound to a name, icon, and rendering engine,
 * launched into its own fullscreen, chrome-free runtime with an isolated, persistent
 * browser profile (see [com.dockeredly.app.browser.core.BrowserEngine]).
 */
data class WebApp(
    val id: String,
    val name: String,
    val url: String,
    val description: String = "",
    val engine: RenderEngine,
    val iconSource: IconSource,
    val sortOrder: Int,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    /** Stable per-web-app identifier used to namespace its browser profile on disk. */
    val profileId: String = id,
)
