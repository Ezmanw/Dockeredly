package com.dockeredly.app.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.keyboard.ImeAction
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dockeredly.app.R
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.domain.model.WebApp
import com.dockeredly.app.ui.components.WebAppCard
import com.dockeredly.app.ui.components.EmptyState
import com.dockeredly.app.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onCreateWebApp: () -> Unit,
    onOpenWebApp: (WebApp) -> Unit,
    onOpenDetails: (WebApp) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModel.factory(container.webAppRepository, container.appContext),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isSearching by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text(stringResource(R.string.library_search_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            ),
                        )
                    } else {
                        Text(stringResource(R.string.library_title), fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_close_search))
                        }
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, stringResource(R.string.cd_search))
                        }
                        if (uiState.allWebApps.isNotEmpty()) {
                            TextButton(onClick = { viewModel.setReordering(!uiState.isReordering) }) {
                                Text(
                                    if (uiState.isReordering) {
                                        stringResource(R.string.library_done_reordering)
                                    } else {
                                        stringResource(R.string.library_reorder)
                                    },
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, stringResource(R.string.cd_more_options))
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings_title)) },
                                    leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        onOpenSettings()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.isReordering) {
                ExtendedFloatingActionButton(
                    onClick = onCreateWebApp,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.action_create_web_app)) },
                )
            }
        },
    ) { padding ->
        when {
            uiState.isEmpty -> EmptyState(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                actionLabel = stringResource(R.string.library_empty_action),
                onAction = onCreateWebApp,
                modifier = Modifier.padding(padding),
            )
            uiState.isReordering -> ReorderableWebAppList(
                webApps = uiState.allWebApps,
                onReorder = viewModel::reorder,
                contentPadding = padding,
            )
            uiState.filteredWebApps.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.library_search_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                items(uiState.filteredWebApps, key = { it.id }) { webApp ->
                    WebAppCard(
                        webApp = webApp,
                        onClick = {
                            viewModel.markLaunched(webApp)
                            onOpenWebApp(webApp)
                        },
                        onLongClick = { onOpenDetails(webApp) },
                    )
                }
            }
        }
    }

    uiState.pendingDeleteWebApp?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = { Text(stringResource(R.string.dialog_delete_body, pending.name)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun ReorderableWebAppList(
    webApps: List<WebApp>,
    onReorder: (List<String>) -> Unit,
    contentPadding: PaddingValues,
) {
    var orderedApps by remember(webApps) { mutableStateOf(webApps) }

    fun move(from: Int, to: Int) {
        if (to < 0 || to >= orderedApps.size) return
        orderedApps = orderedApps.toMutableList().apply { add(to, removeAt(from)) }
        onReorder(orderedApps.map { it.id })
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp, contentPadding.calculateTopPadding() + 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(orderedApps, key = { _, item -> item.id }) { index, webApp ->
            ReorderableRow(
                webApp = webApp,
                canMoveUp = index > 0,
                canMoveDown = index < orderedApps.size - 1,
                onMoveUp = { move(index, index - 1) },
                onMoveDown = { move(index, index + 1) },
            )
        }
    }
}

@Composable
private fun ReorderableRow(
    webApp: WebApp,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Box {
        WebAppCard(webApp = webApp, onClick = {}, onLongClick = {}, showDragHandle = false)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
        ) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp, contentDescription = null)
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = null)
            }
        }
    }
}
