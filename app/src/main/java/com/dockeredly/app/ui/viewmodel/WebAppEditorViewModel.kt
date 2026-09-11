package com.dockeredly.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dockeredly.app.browser.core.BrowserEngineFactory
import com.dockeredly.app.data.preferences.AppSettingsRepository
import com.dockeredly.app.domain.model.IconSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.WebApp
import com.dockeredly.app.domain.repository.WebAppRepository
import com.dockeredly.app.util.FaviconFetcher
import com.dockeredly.app.util.IconProcessor
import com.dockeredly.app.util.UrlValidationResult
import com.dockeredly.app.util.UrlValidator
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class WebAppEditorUiState(
    val isEditing: Boolean = false,
    val editingId: String = UUID.randomUUID().toString(),
    val url: String = "",
    val urlError: String? = null,
    val name: String = "",
    val description: String = "",
    val engine: RenderEngine = RenderEngine.default(),
    val iconSource: IconSource = IconSource.Fallback,
    val isFetchingIcon: Boolean = false,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val pendingEngineChange: RenderEngine? = null,
) {
    val previewName: String get() = name.ifBlank { UrlValidator.deriveNameFromUrl(url) }
}

class WebAppEditorViewModel(
    private val repository: WebAppRepository,
    private val settingsRepository: AppSettingsRepository,
    private val appContext: Context,
    private val existingWebAppId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WebAppEditorUiState(isEditing = existingWebAppId != null))
    val uiState: StateFlow<WebAppEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (existingWebAppId != null) {
                val existing = repository.getWebApp(existingWebAppId)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        editingId = existing.id,
                        url = existing.url,
                        name = existing.name,
                        description = existing.description,
                        engine = existing.engine,
                        iconSource = existing.iconSource,
                        isLoaded = true,
                    )
                }
            } else {
                val defaultEngine = settingsRepository.settings.first().defaultEngine
                _uiState.value = _uiState.value.copy(engine = defaultEngine, isLoaded = true)
            }
        }
    }

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(url = url, urlError = null)
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onDescriptionChange(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun requestEngineChange(engine: RenderEngine) {
        val state = _uiState.value
        if (state.isEditing && state.engine != engine) {
            _uiState.value = state.copy(pendingEngineChange = engine)
        } else {
            _uiState.value = state.copy(engine = engine)
        }
    }

    fun confirmEngineChange() {
        val pending = _uiState.value.pendingEngineChange ?: return
        _uiState.value = _uiState.value.copy(engine = pending, pendingEngineChange = null)
    }

    fun cancelEngineChange() {
        _uiState.value = _uiState.value.copy(pendingEngineChange = null)
    }

    fun fetchWebsiteIcon() {
        val validation = UrlValidator.validate(_uiState.value.url)
        val normalizedUrl = (validation as? UrlValidationResult.Valid)?.normalizedUrl ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingIcon = true)
            val result = FaviconFetcher.fetch(appContext, normalizedUrl, _uiState.value.editingId)
            _uiState.value = if (result != null) {
                _uiState.value.copy(
                    iconSource = IconSource.WebsiteIcon(result.sourceUrl, result.localPath),
                    isFetchingIcon = false,
                )
            } else {
                _uiState.value.copy(isFetchingIcon = false)
            }
        }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingIcon = true)
            val localPath = IconProcessor.processUri(appContext, uri, _uiState.value.editingId)
            _uiState.value = if (localPath != null) {
                _uiState.value.copy(iconSource = IconSource.UserImage(localPath), isFetchingIcon = false)
            } else {
                _uiState.value.copy(isFetchingIcon = false)
            }
        }
    }

    fun useFallbackIcon() {
        _uiState.value = _uiState.value.copy(iconSource = IconSource.Fallback)
    }

    fun save() {
        val state = _uiState.value
        val validation = UrlValidator.validate(state.url)
        val normalizedUrl = when (validation) {
            is UrlValidationResult.Valid -> validation.normalizedUrl
            UrlValidationResult.Empty -> {
                _uiState.value = state.copy(urlError = "empty")
                return
            }
            UrlValidationResult.Invalid -> {
                _uiState.value = state.copy(urlError = "invalid")
                return
            }
            UrlValidationResult.UnsupportedScheme -> {
                _uiState.value = state.copy(urlError = "scheme")
                return
            }
        }

        val finalName = state.name.ifBlank { UrlValidator.deriveNameFromUrl(normalizedUrl) }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            if (state.isEditing) {
                repository.updateWebApp(
                    id = state.editingId,
                    name = finalName,
                    url = normalizedUrl,
                    description = state.description,
                    engine = state.engine,
                    iconSource = state.iconSource,
                )
            } else {
                repository.createWebApp(
                    id = state.editingId,
                    name = finalName,
                    url = normalizedUrl,
                    description = state.description,
                    engine = state.engine,
                    iconSource = state.iconSource,
                )
            }
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }

    fun delete() {
        val state = _uiState.value
        if (!state.isEditing) return
        viewModelScope.launch {
            repository.deleteWebApp(state.editingId)
            BrowserEngineFactory.destroyProfile(appContext, state.engine, state.editingId)
            IconProcessor.deleteIcon(state.editingId, appContext)
            _uiState.value = _uiState.value.copy(deleted = true)
        }
    }

    companion object {
        fun factory(
            repository: WebAppRepository,
            settingsRepository: AppSettingsRepository,
            appContext: Context,
            existingWebAppId: String?,
        ) = viewModelFactory {
            initializer { WebAppEditorViewModel(repository, settingsRepository, appContext, existingWebAppId) }
        }
    }
}
