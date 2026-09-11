package com.dockeredly.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dockeredly.app.domain.model.AppSettings
import com.dockeredly.app.domain.model.ColorSource
import com.dockeredly.app.domain.model.RenderEngine
import com.dockeredly.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "dockeredly_prefs")

/** Global app preferences: theme, dynamic/custom color, default new-web-app engine, link handling. */
class AppSettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val CUSTOM_SEED_COLOR = intPreferencesKey("custom_seed_color")
        val DEFAULT_ENGINE = stringPreferencesKey("default_engine")
        val OPEN_EXTERNAL_LINKS = booleanPreferencesKey("open_external_links")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            colorSource = prefs[Keys.COLOR_SOURCE]?.let { runCatching { ColorSource.valueOf(it) }.getOrNull() }
                ?: ColorSource.DYNAMIC,
            customSeedColor = prefs[Keys.CUSTOM_SEED_COLOR] ?: 0xFF6750A4.toInt(),
            defaultEngine = prefs[Keys.DEFAULT_ENGINE]?.let { runCatching { RenderEngine.valueOf(it) }.getOrNull() }
                ?: RenderEngine.default(),
            openUnsupportedLinksExternally = prefs[Keys.OPEN_EXTERNAL_LINKS] ?: true,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setColorSource(source: ColorSource) {
        context.dataStore.edit { it[Keys.COLOR_SOURCE] = source.name }
    }

    suspend fun setCustomSeedColor(color: Int) {
        context.dataStore.edit { it[Keys.CUSTOM_SEED_COLOR] = color }
    }

    suspend fun setDefaultEngine(engine: RenderEngine) {
        context.dataStore.edit { it[Keys.DEFAULT_ENGINE] = engine.name }
    }

    suspend fun setOpenExternalLinks(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OPEN_EXTERNAL_LINKS] = enabled }
    }
}
