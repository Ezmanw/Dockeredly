package com.dockeredly.app.di

import android.content.Context
import com.dockeredly.app.data.database.AppDatabase
import com.dockeredly.app.data.preferences.AppSettingsRepository
import com.dockeredly.app.data.repository.WebAppRepositoryImpl
import com.dockeredly.app.domain.repository.WebAppRepository

/** Minimal hand-rolled composition root; the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database = AppDatabase.getInstance(appContext)
    val webAppRepository: WebAppRepository = WebAppRepositoryImpl(database.webAppDao())
    val settingsRepository = AppSettingsRepository(appContext)
}
