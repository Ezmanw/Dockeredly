package com.dockeredly.app.navigation

object Routes {
    const val LIBRARY = "library"
    const val CREATE = "editor"
    const val EDIT = "editor/{webAppId}"
    const val DETAILS = "details/{webAppId}"
    const val SETTINGS = "settings"
    const val THEME_CUSTOMIZER = "theme_customizer"

    const val ARG_WEB_APP_ID = "webAppId"

    fun edit(id: String) = "editor/$id"
    fun details(id: String) = "details/$id"
}
