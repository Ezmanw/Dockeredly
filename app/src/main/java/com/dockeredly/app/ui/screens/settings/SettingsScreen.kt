package com.dockeredly.app.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dockeredly.app.BuildConfig
import com.dockeredly.app.R
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.domain.model.ColorSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.ThemeMode
import com.dockeredly.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenThemeCustomizer: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsRepository, container.webAppRepository, container.appContext),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
            item {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            item {
                Column {
                    ThemeModeRow(ThemeMode.SYSTEM, settings.themeMode, viewModel::setThemeMode)
                    ThemeModeRow(ThemeMode.LIGHT, settings.themeMode, viewModel::setThemeMode)
                    ThemeModeRow(ThemeMode.DARK, settings.themeMode, viewModel::setThemeMode)
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_color_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.colorSource == ColorSource.DYNAMIC,
                            onCheckedChange = {
                                viewModel.setColorSource(if (it) ColorSource.DYNAMIC else ColorSource.CUSTOM)
                            },
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_custom_color)) },
                    modifier = Modifier.clickableIfEnabled(settings.colorSource == ColorSource.CUSTOM, onOpenThemeCustomizer),
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_webapps)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_engine)) },
                    supportingContent = { Text(stringResource(R.string.settings_default_engine_desc)) },
                )
            }
            item {
                Column {
                    EngineRow(RenderEngine.CHROMIUM, settings.defaultEngine, viewModel::setDefaultEngine)
                    EngineRow(RenderEngine.GECKO, settings.defaultEngine, viewModel::setDefaultEngine)
                }
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_external_links)) },
                    supportingContent = { Text(stringResource(R.string.settings_external_links_desc)) },
                    trailingContent = {
                        Switch(
                            checked = settings.openUnsupportedLinksExternally,
                            onCheckedChange = viewModel::setOpenExternalLinks,
                        )
                    },
                )
            }
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_clear_all_data),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    supportingContent = { Text(stringResource(R.string.settings_clear_all_data_desc)) },
                    modifier = Modifier.clickableIfEnabled(true) { showDeleteAllDialog = true },
                )
            }
            item { HorizontalDivider() }

            item { SectionHeader(stringResource(R.string.settings_section_about)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text("${stringResource(R.string.settings_version)} ${BuildConfig.VERSION_NAME}") },
                )
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.settings_clear_all_data)) },
            text = { Text(stringResource(R.string.settings_clear_all_data_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllWebApps()
                    showDeleteAllDialog = false
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThemeModeRow(mode: ThemeMode, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    ListItem(
        headlineContent = { Text(themeModeLabel(mode)) },
        leadingContent = {
            RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
        },
        modifier = Modifier.clickableIfEnabled(true) { onSelect(mode) },
    )
}

@Composable
private fun EngineRow(engine: RenderEngine, selected: RenderEngine, onSelect: (RenderEngine) -> Unit) {
    ListItem(
        headlineContent = { Text(com.dockeredly.app.ui.components.engineLabel(engine)) },
        leadingContent = {
            RadioButton(selected = engine == selected, onClick = { onSelect(engine) })
        },
        modifier = Modifier.clickableIfEnabled(true) { onSelect(engine) },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

private fun Modifier.clickableIfEnabled(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.clickable(onClick = onClick) else this
