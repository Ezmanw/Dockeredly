package com.dockeredly.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dockeredly.app.di.LocalAppContainer
import com.dockeredly.app.domain.model.AppSettings
import com.dockeredly.app.navigation.DockeredlyNavGraph
import com.dockeredly.app.ui.theme.DockeredlyTheme
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as DockeredlyApplication).container

        setContent {
            val settings: AppSettings by container.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            CompositionLocalProvider(LocalAppContainer provides container) {
                DockeredlyTheme(
                    themeMode = settings.themeMode,
                    colorSource = settings.colorSource,
                    customSeedColor = Color(settings.customSeedColor),
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        DockeredlyNavGraph(navController = navController)
                    }
                }
            }
        }
    }
}
