package com.floatscript.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.floatscript.app.ui.editor.ScriptEditorScreen
import com.floatscript.app.ui.home.HomeScreen
import com.floatscript.app.ui.record.RecordScreen
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
    const val RECORD = "record"

    fun editor(id: Long) = "$EDITOR/$id"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onEditScript = { id -> navController.navigate(Routes.editor(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRecording = { navController.navigate(Routes.RECORD) }
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
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.RECORD) {
            RecordScreen(onBack = { navController.popBackStack() })
        }
    }
}
