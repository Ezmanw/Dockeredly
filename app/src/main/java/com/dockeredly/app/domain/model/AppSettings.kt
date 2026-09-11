package com.dockeredly.app.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class ColorSource { DYNAMIC, CUSTOM }

/**
 * Global, app-wide preferences. None of these affect already-created web apps except
 * where explicitly noted (e.g. [defaultEngine] only seeds new web apps).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorSource: ColorSource = ColorSource.DYNAMIC,
    val customSeedColor: Int = 0xFF6750A4.toInt(),
    val defaultEngine: RenderEngine = RenderEngine.default(),
    val openUnsupportedLinksExternally: Boolean = true,
)
