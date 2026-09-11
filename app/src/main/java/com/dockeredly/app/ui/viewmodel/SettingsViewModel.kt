package com.dockeredly.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dockeredly.app.browser.core.BrowserEngineFactory
import com.dockeredly.app.data.preferences.AppSettingsRepository
import com.dockeredly.app.domain.model.AppSettings
import com.dockeredly.app.domain.model.ColorSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.ThemeMode
import com.dockeredly.app.domain.repository.WebAppRepository
import com.dockeredly.app.util.IconProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: AppSettingsRepository,
    private val webAppRepository: WebAppRepository,
    private val appContext: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }

    fun setColorSource(source: ColorSource) = viewModelScope.launch { settingsRepository.setColorSource(source) }

    fun setCustomSeedColor(color: Int) = viewModelScope.launch { settingsRepository.setCustomSeedColor(color) }

    fun setDefaultEngine(engine: RenderEngine) = viewModelScope.launch { settingsRepository.setDefaultEngine(engine) }

    fun setOpenExternalLinks(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setOpenExternalLinks(enabled) }

    fun deleteAllWebApps() = viewModelScope.launch {
        val apps = webAppRepository.observeWebApps().first()
        apps.forEach { app ->
            BrowserEngineFactory.destroyProfile(appContext, app.engine, app.profileId)
            IconProcessor.deleteIcon(app.id, appContext)
        }
        webAppRepository.deleteAllWebApps()
    }

    companion object {
        fun factory(
            settingsRepository: AppSettingsRepository,
            webAppRepository: WebAppRepository,
            appContext: Context,
        ) = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository, webAppRepository, appContext) }
        }
    }
}
