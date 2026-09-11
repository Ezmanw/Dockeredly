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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val allWebApps: List<WebApp> = emptyList(),
    val searchQuery: String = "",
    val isReordering: Boolean = false,
    val pendingDeleteWebApp: WebApp? = null,
) {
    val filteredWebApps: List<WebApp>
        get() = if (searchQuery.isBlank()) {
            allWebApps
        } else {
            allWebApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.url.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
            }
        }

    val isEmpty: Boolean get() = allWebApps.isEmpty()
}

class LibraryViewModel(
    private val repository: WebAppRepository,
    private val appContext: Context,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val isReordering = MutableStateFlow(false)
    private val pendingDeleteWebApp = MutableStateFlow<WebApp?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeWebApps(),
        searchQuery,
        isReordering,
        pendingDeleteWebApp,
    ) { apps, query, reordering, pendingDelete ->
        LibraryUiState(apps, query, reordering, pendingDelete)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun setReordering(reordering: Boolean) {
        isReordering.value = reordering
    }

    fun reorder(orderedIds: List<String>) {
        viewModelScope.launch { repository.reorderWebApps(orderedIds) }
    }

    fun requestDelete(webApp: WebApp) {
        pendingDeleteWebApp.value = webApp
    }

    fun cancelDelete() {
        pendingDeleteWebApp.value = null
    }

    fun confirmDelete() {
        val webApp = pendingDeleteWebApp.value ?: return
        viewModelScope.launch {
            repository.deleteWebApp(webApp.id)
            BrowserEngineFactory.destroyProfile(appContext, webApp.engine, webApp.profileId)
            IconProcessor.deleteIcon(webApp.id, appContext)
        }
        pendingDeleteWebApp.value = null
    }

    fun markLaunched(webApp: WebApp) {
        viewModelScope.launch { repository.markLaunched(webApp.id) }
    }

    companion object {
        fun factory(repository: WebAppRepository, appContext: Context) = viewModelFactory {
            initializer { LibraryViewModel(repository, appContext) }
        }
    }
}
