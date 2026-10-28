package com.floatscript.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatscript.app.BuildConfig
import com.floatscript.app.data.SettingsRepository
import com.floatscript.app.update.AppVersion
import com.floatscript.app.update.UpdateChecker
import com.floatscript.app.update.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Downloading(val info: UpdateInfo, val downloaded: Long, val total: Long) : UpdateUiState
    data class Downloaded(val info: UpdateInfo, val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

/**
 * Drives the update flow: check the latest release, download the APK with
 * progress, then hand it to the system installer.
 */
class UpdateViewModel(private val settings: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /** Runs at most once per [intervalMs]; used on app start. */
    fun autoCheckIfDue(intervalMs: Long = DEFAULT_INTERVAL_MS) {
        if (_state.value !is UpdateUiState.Idle) return
        viewModelScope.launch {
            val last = settings.lastUpdateCheck.first()
            if (System.currentTimeMillis() - last >= intervalMs) check()
        }
    }

    fun check() {
        if (_state.value is UpdateUiState.Checking) return
        viewModelScope.launch { settings.setLastUpdateCheck(System.currentTimeMillis()) }
        _state.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = UpdateChecker().latest()
            _state.value = result.fold(
                onSuccess = { info ->
                    when {
                        info == null -> UpdateUiState.Error("No downloadable release found")
                        AppVersion.isNewer(info.version, BuildConfig.VERSION_NAME) ->
                            UpdateUiState.Available(info)
                        else -> UpdateUiState.UpToDate
                    }
                },
                onFailure = { UpdateUiState.Error(it.message ?: "Could not reach the update server") }
            )
        }
    }

    fun download(context: Context, info: UpdateInfo) {
        downloadJob?.cancel()
        val dir = File(context.cacheDir, "updates")
        val fileName = "FloatScript-${info.version}.apk"
        downloadJob = viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(info, 0L, info.sizeBytes)
            try {
                val file = fetch(info.apkUrl, dir, fileName) { done, total ->
                    _state.value = UpdateUiState.Downloading(info, done, total)
                }
                _state.value = UpdateUiState.Downloaded(info, file)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                _state.value = UpdateUiState.Error(t.message ?: "Download failed")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.value = UpdateUiState.Available(
            (_state.value as? UpdateUiState.Downloading)?.info ?: return
        )
    }

    fun reset() { _state.value = UpdateUiState.Idle }

    private companion object {
        const val DEFAULT_INTERVAL_MS = 24L * 60L * 60L * 1000L
    }

    private suspend fun fetch(
        url: String,
        dir: File,
        name: String,
        onProgress: (Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "FloatScript")
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: 0L
            dir.mkdirs()
            val out = File(dir, name)
            connection.inputStream.use { input ->
                FileOutputStream(out).use { fos ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        if (!coroutineContext.isActive) {
                            out.delete()
                            error("Cancelled")
                        }
                        val read = input.read(buffer)
                        if (read <= 0) break
                        fos.write(buffer, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
            }
            out
        } finally {
            runCatching { connection.disconnect() }
        }
    }
}
