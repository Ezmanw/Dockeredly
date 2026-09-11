package com.dockeredly.app.domain.repository

import com.dockeredly.app.domain.model.IconSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.WebApp
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for web app records. Implementations persist to Room and manage the
 * on-disk icon cache; they do not touch per-app browser profile data (see
 * [com.dockeredly.app.browser.core.BrowserEngine] for that).
 */
interface WebAppRepository {
    fun observeWebApps(): Flow<List<WebApp>>
    fun observeWebApp(id: String): Flow<WebApp?>
    suspend fun getWebApp(id: String): WebApp?

    suspend fun createWebApp(
        id: String,
        name: String,
        url: String,
        description: String,
        engine: RenderEngine,
        iconSource: IconSource,
    ): WebApp

    suspend fun updateWebApp(
        id: String,
        name: String,
        url: String,
        description: String,
        engine: RenderEngine,
        iconSource: IconSource,
    )

    suspend fun deleteWebApp(id: String)
    suspend fun deleteAllWebApps()
    suspend fun reorderWebApps(orderedIds: List<String>)
    suspend fun markLaunched(id: String)
}
