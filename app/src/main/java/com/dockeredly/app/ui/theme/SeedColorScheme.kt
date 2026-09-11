package com.dockeredly.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/**
 * Derives a full Material 3 [ColorScheme] from a single seed/accent color when the user
 * picks a custom color instead of dynamic (wallpaper-based) color. This uses simple
 * HSL tone/saturation adjustments rather than Google's HCT color space (Material Color
 * Utilities), so hue/chroma perception won't exactly match the reference Material theme
 * builder - but it produces a coherent, accessible-contrast scheme from any seed without
 * an extra color-science dependency.
 */
object SeedColorScheme {

    fun light(seed: Color): ColorScheme {
        val hsl = seed.toHsl()
        return lightColorScheme(
            primary = tone(hsl, lightness = 0.40f, saturationScale = 1f),
            onPrimary = tone(hsl, lightness = 0.99f, saturationScale = 0.1f),
            primaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.5f),
            onPrimaryContainer = tone(hsl, lightness = 0.12f, saturationScale = 0.6f),
            secondary = tone(hsl, lightness = 0.38f, saturationScale = 0.35f),
            onSecondary = tone(hsl, lightness = 0.99f, saturationScale = 0.05f),
            secondaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.2f),
            onSecondaryContainer = tone(hsl, lightness = 0.14f, saturationScale = 0.25f),
            tertiary = tone(hsl, lightness = 0.38f, saturationScale = 0.5f, hueShift = 60f),
            onTertiary = tone(hsl, lightness = 0.99f, saturationScale = 0.05f),
            tertiaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.3f, hueShift = 60f),
            onTertiaryContainer = tone(hsl, lightness = 0.14f, saturationScale = 0.3f, hueShift = 60f),
            background = tone(hsl, lightness = 0.985f, saturationScale = 0.04f),
            onBackground = tone(hsl, lightness = 0.12f, saturationScale = 0.06f),
            surface = tone(hsl, lightness = 0.985f, saturationScale = 0.04f),
            onSurface = tone(hsl, lightness = 0.12f, saturationScale = 0.06f),
            surfaceVariant = tone(hsl, lightness = 0.92f, saturationScale = 0.15f),
            onSurfaceVariant = tone(hsl, lightness = 0.30f, saturationScale = 0.15f),
            outline = tone(hsl, lightness = 0.48f, saturationScale = 0.12f),
            outlineVariant = tone(hsl, lightness = 0.80f, saturationScale = 0.12f),
            error = Color(0xFFBA1A1A),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
        )
    }

    fun dark(seed: Color): ColorScheme {
        val hsl = seed.toHsl()
        return darkColorScheme(
            primary = tone(hsl, lightness = 0.80f, saturationScale = 0.6f),
            onPrimary = tone(hsl, lightness = 0.20f, saturationScale = 0.6f),
            primaryContainer = tone(hsl, lightness = 0.30f, saturationScale = 0.6f),
            onPrimaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.4f),
            secondary = tone(hsl, lightness = 0.80f, saturationScale = 0.2f),
            onSecondary = tone(hsl, lightness = 0.20f, saturationScale = 0.2f),
            secondaryContainer = tone(hsl, lightness = 0.28f, saturationScale = 0.25f),
            onSecondaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.15f),
            tertiary = tone(hsl, lightness = 0.80f, saturationScale = 0.4f, hueShift = 60f),
            onTertiary = tone(hsl, lightness = 0.20f, saturationScale = 0.3f, hueShift = 60f),
            tertiaryContainer = tone(hsl, lightness = 0.30f, saturationScale = 0.35f, hueShift = 60f),
            onTertiaryContainer = tone(hsl, lightness = 0.90f, saturationScale = 0.25f, hueShift = 60f),
            background = tone(hsl, lightness = 0.10f, saturationScale = 0.08f),
            onBackground = tone(hsl, lightness = 0.90f, saturationScale = 0.06f),
            surface = tone(hsl, lightness = 0.10f, saturationScale = 0.08f),
            onSurface = tone(hsl, lightness = 0.90f, saturationScale = 0.06f),
            surfaceVariant = tone(hsl, lightness = 0.30f, saturationScale = 0.15f),
            onSurfaceVariant = tone(hsl, lightness = 0.80f, saturationScale = 0.15f),
            outline = tone(hsl, lightness = 0.60f, saturationScale = 0.12f),
            outlineVariant = tone(hsl, lightness = 0.30f, saturationScale = 0.12f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
        )
    }

    private data class Hsl(val h: Float, val s: Float, val l: Float)

    private fun Color.toHsl(): Hsl {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(this.toArgb(), out)
        return Hsl(out[0], out[1], out[2])
    }

    private fun tone(hsl: Hsl, lightness: Float, saturationScale: Float, hueShift: Float = 0f): Color {
        val hue = (hsl.h + hueShift).mod(360f)
        val saturation = (hsl.s * saturationScale).coerceIn(0f, 1f)
        val argb = ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness.coerceIn(0f, 1f)))
        return Color(argb)
    }
}
