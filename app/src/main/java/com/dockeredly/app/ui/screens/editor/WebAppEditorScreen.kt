package com.dockeredly.app.ui.screens.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dockeredly.app.R
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.ui.components.EngineSelector
import com.dockeredly.app.ui.components.WebAppIcon
import com.dockeredly.app.ui.viewmodel.WebAppEditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebAppEditorScreen(
    existingWebAppId: String?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: WebAppEditorViewModel = viewModel(
        factory = WebAppEditorViewModel.factory(
            container.webAppRepository,
            container.settingsRepository,
            container.appContext,
            existingWebAppId,
        ),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saved, uiState.deleted) {
        if (uiState.saved || uiState.deleted) onDone()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::onImagePicked) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.action_edit_web_app else R.string.action_create_web_app,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !uiState.isSaving) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.Check, null)
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = uiState.url,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.field_url_label)) },
                placeholder = { Text(stringResource(R.string.field_url_placeholder)) },
                singleLine = true,
                isError = uiState.urlError != null,
                supportingText = uiState.urlError?.let { { Text(urlErrorMessage(it)) } },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.field_name_label)) },
                placeholder = { Text(uiState.previewName.ifBlank { stringResource(R.string.field_name_placeholder) }) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text(stringResource(R.string.field_description_label)) },
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.icon_section_title), style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WebAppIcon(iconSource = uiState.iconSource, size = 56.dp)
                    if (uiState.isFetchingIcon) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = viewModel::fetchWebsiteIcon,
                        label = { Text(stringResource(R.string.icon_action_favicon)) },
                    )
                    AssistChip(
                        onClick = {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly,
                                ),
                            )
                        },
                        label = { Text(stringResource(R.string.icon_action_gallery)) },
                    )
                    AssistChip(
                        onClick = viewModel::useFallbackIcon,
                        label = { Text(stringResource(R.string.icon_action_fallback)) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.engine_section_title), style = MaterialTheme.typography.titleMedium)
                EngineSelector(selected = uiState.engine, onSelect = viewModel::requestEngineChange)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.preview_section_title), style = MaterialTheme.typography.titleMedium)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WebAppIcon(iconSource = uiState.iconSource, size = 48.dp)
                        Column {
                            Text(uiState.previewName, style = MaterialTheme.typography.titleMedium)
                            Text(
                                engineDisplayName(uiState.engine),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (uiState.isEditing) {
                TextButton(onClick = viewModel::delete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    uiState.pendingEngineChange?.let {
        AlertDialog(
            onDismissRequest = viewModel::cancelEngineChange,
            title = { Text(stringResource(R.string.dialog_reset_title)) },
            text = { Text(stringResource(R.string.dialog_reset_body, uiState.previewName)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEngineChange) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEngineChange) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun urlErrorMessage(tag: String): String = when (tag) {
    "empty" -> stringResource(R.string.field_url_error_empty)
    "scheme" -> stringResource(R.string.field_url_error_scheme)
    else -> stringResource(R.string.field_url_error_invalid)
}

@Composable
private fun engineDisplayName(engine: com.dockeredly.app.domain.model.RenderEngine): String = when (engine) {
    com.dockeredly.app.domain.model.RenderEngine.CHROMIUM -> stringResource(R.string.engine_chromium_title)
    com.dockeredly.app.domain.model.RenderEngine.GECKO -> stringResource(R.string.engine_firefox_title)
}
