package com.dockeredly.app.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided - wrap content in CompositionLocalProvider(LocalAppContainer provides ...)")
}
