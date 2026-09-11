package com.dockeredly.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dockeredly.app.browser.core.BrowserEngineFactory
import com.dockeredly.app.domain.model.WebApp
import com.dockeredly.app.domain.repository.WebAppRepository
import com.dockeredly.app.util.IconProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WebAppDetailsViewModel(
    private val repository: WebAppRepository,
    private val appContext: Context,
    webAppId: String,
) : ViewModel() {

    val webApp: StateFlow<WebApp?> = repository.observeWebApp(webAppId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearCache() {
        val app = webApp.value ?: return
        BrowserEngineFactory.clearCache(appContext, app.engine, app.profileId)
    }

    fun resetBrowserData() {
        val app = webApp.value ?: return
        BrowserEngineFactory.destroyProfile(appContext, app.engine, app.profileId)
    }

    fun delete() = viewModelScope.launch {
        val app = webApp.value ?: return@launch
        repository.deleteWebApp(app.id)
        BrowserEngineFactory.destroyProfile(appContext, app.engine, app.profileId)
        IconProcessor.deleteIcon(app.id, appContext)
    }

    companion object {
        fun factory(repository: WebAppRepository, appContext: Context, webAppId: String) = viewModelFactory {
            initializer { WebAppDetailsViewModel(repository, appContext, webAppId) }
        }
    }
}
