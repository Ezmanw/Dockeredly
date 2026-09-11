package com.dockeredly.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dockeredly.app.runtime.WebAppLauncher
import com.dockeredly.app.ui.screens.details.WebAppDetailsScreen
import com.dockeredly.app.ui.screens.editor.WebAppEditorScreen
import com.dockeredly.app.ui.screens.library.LibraryScreen
import com.dockeredly.app.ui.screens.settings.SettingsScreen
import com.dockeredly.app.ui.screens.theme.ThemeCustomizerScreen

@Composable
fun DockeredlyNavGraph(navController: NavHostController) {
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onCreateWebApp = { navController.navigate(Routes.CREATE) },
                onOpenWebApp = { webApp -> WebAppLauncher.launch(context, webApp) },
                onOpenDetails = { webApp -> navController.navigate(Routes.details(webApp.id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CREATE) {
            WebAppEditorScreen(existingWebAppId = null, onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument(Routes.ARG_WEB_APP_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Routes.ARG_WEB_APP_ID)
            WebAppEditorScreen(existingWebAppId = id, onDone = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument(Routes.ARG_WEB_APP_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(Routes.ARG_WEB_APP_ID).orEmpty()
            WebAppDetailsScreen(
                webAppId = id,
                onBack = { navController.popBackStack() },
                onEdit = { editId -> navController.navigate(Routes.edit(editId)) },
                onOpenWebApp = { webApp -> WebAppLauncher.launch(context, webApp) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenThemeCustomizer = { navController.navigate(Routes.THEME_CUSTOMIZER) },
            )
        }
        composable(Routes.THEME_CUSTOMIZER) {
            ThemeCustomizerScreen(onBack = { navController.popBackStack() })
        }
    }
}
