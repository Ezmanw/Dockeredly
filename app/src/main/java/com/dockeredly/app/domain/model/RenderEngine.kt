package com.dockeredly.app.domain.model

/**
 * The web rendering engine a web app is bound to. Chosen per web app and persisted;
 * changing the global default (see [com.dockeredly.app.domain.model.AppSettings]) never
 * changes an existing web app's engine.
 */
enum class RenderEngine {
    CHROMIUM,
    GECKO;

    companion object {
        fun default(): RenderEngine = CHROMIUM
    }
}
