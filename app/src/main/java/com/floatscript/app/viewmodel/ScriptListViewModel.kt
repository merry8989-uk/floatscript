package com.floatscript.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatscript.app.data.ScriptRepository
import com.floatscript.app.data.SettingsRepository
import com.floatscript.app.model.Script
import com.floatscript.app.overlay.OverlayService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScriptListViewModel(
    private val scripts: ScriptRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    val scriptList: StateFlow<List<Script>> = scripts.scripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Makes [script] the active script and launches (or updates) the overlay.
     * Returns false when the system refused to start the foreground service.
     */
    fun startOverlay(context: Context, script: Script): Boolean {
        viewModelScope.launch { settings.setLastScriptId(script.id) }
        return if (OverlayService.isRunning) {
            OverlayService.updateScript(context, script.id)
            true
        } else {
            OverlayService.start(context)
        }
    }

    fun stopOverlay(context: Context) = OverlayService.stop(context)

    fun createScript(title: String, body: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = scripts.upsert(Script(title = title, body = body))
            onCreated(id)
        }
    }

    fun duplicate(script: Script) = viewModelScope.launch { scripts.duplicate(script.id) }

    fun rename(script: Script, newTitle: String) =
        viewModelScope.launch { scripts.rename(script.id, newTitle) }

    fun delete(script: Script) = viewModelScope.launch { scripts.delete(script) }

    // ---- Local backup / restore ---------------------------------------------

    /** Produces a JSON backup of every script and hands it back on the main thread. */
    fun buildBackup(onReady: (String) -> Unit) {
        viewModelScope.launch { onReady(scripts.exportAll()) }
    }

    /** Restores scripts from a JSON backup; [onResult] gets the count, or -1 on failure. */
    fun restoreBackup(json: String, onResult: (Int) -> Unit) {
        viewModelScope.launch { onResult(scripts.importAll(json)) }
    }
}
