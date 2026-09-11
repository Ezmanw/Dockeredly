package com.dockeredly.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.dockeredly.app.domain.model.ColorSource
import com.dockeredly.app.domain.model.ThemeMode

@Composable
fun DockeredlyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorSource: ColorSource = ColorSource.DYNAMIC,
    customSeedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        colorSource == ColorSource.DYNAMIC && dynamicSupported ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> SeedColorScheme.dark(customSeedColor)
        else -> SeedColorScheme.light(customSeedColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DockeredlyTypography,
        shapes = DockeredlyShapes,
        content = content,
    )
}
