package com.dockeredly.app

import android.app.Application
import android.webkit.WebView
import com.dockeredly.app.browser.core.BrowserProcessSlots
import com.dockeredly.app.di.AppContainer
import com.dockeredly.app.util.ProcessNameCompat

class DockeredlyApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        // Must run before any WebView is created in this process (including one created
        // by a runtime activity slot below) - see BrowserProcessSlots for why.
        configureWebViewDataDirectoryForProcess()
        super.onCreate()
    }

    private fun configureWebViewDataDirectoryForProcess() {
        val processName = ProcessNameCompat.currentProcessName(this)
        val suffix = BrowserProcessSlots.suffixFromProcessName(processName, packageName) ?: return
        runCatching { WebView.setDataDirectorySuffix(suffix) }
    }
}
