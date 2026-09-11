package com.dockeredly.app.ui.screens.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dockeredly.app.R
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.domain.model.ColorSource
import com.dockeredly.app.ui.viewmodel.SettingsViewModel

/** Evenly spaced hues at a fixed saturation/lightness, so swatches read as one coherent family. */
private val presetSeedColors: List<Color> = (0 until 12).map { i ->
    Color.hsv(hue = i * 30f, saturation = 0.55f, value = 0.55f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizerScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.factory(container.settingsRepository, container.webAppRepository, container.appContext),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_customizer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Aa", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        stringResource(R.string.settings_custom_color),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Text(
                stringResource(R.string.settings_custom_color),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp),
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(presetSeedColors) { color ->
                    val isSelected = settings.colorSource == ColorSource.CUSTOM && settings.customSeedColor == color.toArgb()
                    ColorSwatch(
                        color = color,
                        isSelected = isSelected,
                        onClick = {
                            viewModel.setCustomSeedColor(color.toArgb())
                            viewModel.setColorSource(ColorSource.CUSTOM)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}
