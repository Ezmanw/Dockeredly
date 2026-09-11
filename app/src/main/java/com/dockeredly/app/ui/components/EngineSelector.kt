package com.dockeredly.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dockeredly.app.R
import com.dockeredly.app.domain.model.RenderEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EngineSelector(
    selected: RenderEngine,
    onSelect: (RenderEngine) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            RenderEngine.entries.forEachIndexed { index, engine ->
                SegmentedButton(
                    selected = selected == engine,
                    onClick = { onSelect(engine) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = RenderEngine.entries.size),
                    label = { Text(engineTitle(engine)) },
                )
            }
        }
        Text(
            text = engineDescription(selected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun engineTitle(engine: RenderEngine): String = when (engine) {
    RenderEngine.CHROMIUM -> stringResource(R.string.engine_chromium_title)
    RenderEngine.GECKO -> stringResource(R.string.engine_firefox_title)
}

@Composable
private fun engineDescription(engine: RenderEngine): String = when (engine) {
    RenderEngine.CHROMIUM -> stringResource(R.string.engine_chromium_description)
    RenderEngine.GECKO -> stringResource(R.string.engine_firefox_description)
}
