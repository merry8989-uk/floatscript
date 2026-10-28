package com.floatscript.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.floatscript.app.ui.diagnostics.DiagnosticsScreen
import com.floatscript.app.ui.editor.ScriptEditorScreen
import com.floatscript.app.ui.home.HomeScreen
import com.floatscript.app.ui.record.RecordScreen
import com.floatscript.app.ui.update.UpdateScreen
import com.floatscript.app.ui.settings.SettingsScreen
import com.floatscript.app.ui.theme.FloatScriptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FloatScriptTheme {
                AppNavHost()
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
    const val UPDATE = "update"
    const val RECORD = "record"

    fun editor(id: Long) = "$EDITOR/$id"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onEditScript = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRecording = { navController.navigate(Routes.RECORD) },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onOpenUpdate = { navController.navigate(Routes.UPDATE) }
            )
        }

        composable(
            route = "${Routes.EDITOR}/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ScriptEditorScreen(
                scriptId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) },
                onOpenUpdate = { navController.navigate(Routes.UPDATE) },
                onOpenMoreApps = { openUrl(context, AppLinks.MORE_APPS_URL) }
            )
        }

        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.UPDATE) {
            UpdateScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RECORD) {
            RecordScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Opens an external link, ignoring the rare case where no browser exists. */
private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
