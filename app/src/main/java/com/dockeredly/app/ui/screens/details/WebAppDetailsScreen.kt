package com.dockeredly.app.ui.screens.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dockeredly.app.R
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.domain.model.WebApp
import com.dockeredly.app.ui.components.WebAppIcon
import com.dockeredly.app.ui.components.engineLabel
import com.dockeredly.app.ui.viewmodel.WebAppDetailsViewModel
import java.text.DateFormat
import java.util.Date

private enum class PendingAction { CLEAR_CACHE, RESET_DATA, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppDetailsScreen(
    webAppId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenWebApp: (WebApp) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: WebAppDetailsViewModel = viewModel(
        factory = WebAppDetailsViewModel.factory(container.webAppRepository, container.appContext, webAppId),
    )
    val webApp by viewModel.webApp.collectAsStateWithLifecycle()
    var pendingAction by remember { mutableStateOf<PendingAction?>(null) }
    var deleted by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(webAppId) }) {
                        Icon(Icons.Filled.Edit, stringResource(R.string.action_edit_web_app))
                    }
                },
            )
        },
    ) { padding ->
        val app = webApp
        if (app == null) {
            return@Scaffold
        }
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WebAppIcon(iconSource = app.iconSource, size = 56.dp)
                Column {
                    Text(app.name, style = MaterialTheme.typography.titleLarge)
                    Text(app.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = { onOpenWebApp(app) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.PlayArrow, null)
                Text(" " + stringResource(R.string.action_open))
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(stringResource(R.string.engine_section_title), engineLabel(app.engine))
                    InfoRow(stringResource(R.string.details_created), formatDate(app.createdAt))
                    InfoRow(
                        stringResource(R.string.details_last_used),
                        app.lastUsedAt?.let { formatDate(it) } ?: stringResource(R.string.details_never_used),
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.details_storage_title), style = MaterialTheme.typography.titleSmall)
                    InfoRow(stringResource(R.string.details_storage_session), stringResource(R.string.details_storage_session_value))
                    InfoRow(stringResource(R.string.details_storage_cache), stringResource(R.string.details_storage_cache_value))
                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { pendingAction = PendingAction.CLEAR_CACHE }) {
                            Text(stringResource(R.string.details_clear_cache))
                        }
                        OutlinedButton(onClick = { pendingAction = PendingAction.RESET_DATA }) {
                            Text(stringResource(R.string.details_reset_data))
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { pendingAction = PendingAction.DELETE },
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Filled.Delete, null)
                Text(" " + stringResource(R.string.action_delete))
            }
        }

        pendingAction?.let { action ->
            val (title, body) = when (action) {
                PendingAction.CLEAR_CACHE -> stringResource(R.string.dialog_clear_cache_title) to
                    stringResource(R.string.dialog_clear_cache_body, app.name)
                PendingAction.RESET_DATA -> stringResource(R.string.dialog_reset_title) to
                    stringResource(R.string.dialog_reset_body, app.name)
                PendingAction.DELETE -> stringResource(R.string.dialog_delete_title) to
                    stringResource(R.string.dialog_delete_body, app.name)
            }
            AlertDialog(
                onDismissRequest = { pendingAction = null },
                title = { Text(title) },
                text = { Text(body) },
                confirmButton = {
                    TextButton(onClick = {
                        when (action) {
                            PendingAction.CLEAR_CACHE -> viewModel.clearCache()
                            PendingAction.RESET_DATA -> viewModel.resetBrowserData()
                            PendingAction.DELETE -> {
                                viewModel.delete()
                                deleted = true
                            }
                        }
                        pendingAction = null
                    }) { Text(stringResource(R.string.action_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingAction = null }) { Text(stringResource(R.string.action_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))
