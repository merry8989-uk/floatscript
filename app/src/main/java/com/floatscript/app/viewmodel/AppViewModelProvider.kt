package com.floatscript.app.viewmodel

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.floatscript.app.FloatScriptApp

/** Wires ViewModels to the app's repositories without a DI framework. */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            val app = this[APPLICATION_KEY] as FloatScriptApp
            ScriptListViewModel(app.scriptRepository, app.settingsRepository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as FloatScriptApp
            ScriptEditorViewModel(app.scriptRepository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as FloatScriptApp
            SettingsViewModel(app.settingsRepository)
        }
        initializer {
            val app = this[APPLICATION_KEY] as FloatScriptApp
            UpdateViewModel(app.settingsRepository)
        }
    }
}
