package com.floatscript.app

import android.app.Application
import com.floatscript.app.data.AppDatabase
import com.floatscript.app.data.ScriptRepository
import com.floatscript.app.data.SettingsRepository
import com.floatscript.app.data.SampleScripts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Acts as a tiny manual dependency container so the
 * whole project needs no DI framework — repositories are created once and
 * shared by the UI and the overlay service.
 */
class FloatScriptApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val scriptRepository: ScriptRepository by lazy {
        ScriptRepository(AppDatabase.get(this).scriptDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            scriptRepository.seedIfEmpty(
                sampleTitle = SampleScripts.SAMPLE_TITLE,
                sampleBody = SampleScripts.SAMPLE_BODY
            )
        }
    }
}
