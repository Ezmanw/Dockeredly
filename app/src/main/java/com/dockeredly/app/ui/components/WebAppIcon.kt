package com.dockeredly.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dockeredly.app.R
import com.dockeredly.app.domain.model.IconSource

@Composable
fun WebAppIcon(
    iconSource: IconSource,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val shape = RoundedCornerShape(size / 3)
    when (iconSource) {
        is IconSource.WebsiteIcon -> AsyncImage(
            model = iconSource.localPath,
            contentDescription = stringResource(R.string.cd_web_app_icon),
            modifier = modifier.size(size).clip(shape),
        )
        is IconSource.UserImage -> AsyncImage(
            model = iconSource.localPath,
            contentDescription = stringResource(R.string.cd_web_app_icon),
            modifier = modifier.size(size).clip(shape),
        )
        IconSource.Fallback -> Box(
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = stringResource(R.string.cd_web_app_icon),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
