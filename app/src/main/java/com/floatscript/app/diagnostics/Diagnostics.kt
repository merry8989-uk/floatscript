package com.floatscript.app.diagnostics

import android.content.Context
import android.os.Build
import com.floatscript.app.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Local-only diagnostics.
 *
 * Everything lives on the device: a bounded in-memory log buffer, a small
 * rolling log file, and a crash report written when the app dies from an
 * uncaught exception. Nothing is ever sent anywhere — the user can inspect,
 * share or export the report themselves.
 *
 * There is deliberately no network code here and the app declares no INTERNET
 * permission.
 */
object Diagnostics {

    private const val MAX_FILE_BYTES = 256 * 1024
    private const val BUFFER_CAPACITY = 500
    private const val DIR_NAME = "diagnostics"
    private const val CURRENT = "current.log"
    private const val PREVIOUS = "previous.log"
    private const val CRASH_PREFIX = "crash-"

    private val buffer = LogRingBuffer(BUFFER_CAPACITY)
    private val lock = Any()
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile private var dir: File? = null
    @Volatile private var executor: ExecutorService? = null
    @Volatile private var initialised = false

    /** Newest crash report left behind by a previous run, if any. */
    @Volatile
    var lastCrashFile: File? = null
        private set

    fun init(context: Context) {
        if (initialised) return
        initialised = true

        val d = File(context.applicationContext.filesDir, DIR_NAME).apply { mkdirs() }
        dir = d
        executor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "floatscript-diagnostics").apply { isDaemon = true }
        }
        lastCrashFile = d.listFiles { f -> f.isFile && f.name.startsWith(CRASH_PREFIX) }
            ?.maxByOrNull { it.lastModified() }

        installCrashHandler()
        i("App", "Started FloatScript ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) on Android ${Build.VERSION.SDK_INT}")
        lastCrashFile?.let { w("App", "A crash report from a previous run is available: ${it.name}") }
    }

    fun d(tag: String, message: String) = log('D', tag, message)
    fun i(tag: String, message: String) = log('I', tag, message)
    fun w(tag: String, message: String) = log('W', tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) =
        log('E', tag, if (throwable == null) message else "$message: ${throwable.javaClass.simpleName}: ${throwable.message}")

    fun log(level: Char, tag: String, message: String) {
        val entry = synchronized(lock) {
            LogEntry(timeFormat.format(Date()), level, tag, message).also { buffer.add(it) }
        }
        val line = entry.format()
        val ex = executor
        if (ex != null) ex.execute { appendToFile(line) } else appendToFile(line)
    }

    private fun appendToFile(line: String) {
        val d = dir ?: return
        runCatching {
            val current = File(d, CURRENT)
            if (current.length() > MAX_FILE_BYTES) {
                File(d, PREVIOUS).delete()
                current.renameTo(File(d, PREVIOUS))
            }
            File(d, CURRENT).appendText(line + "\n")
        }
    }

    /** Header + the recent in-memory log. */
    fun snapshotText(): String {
        val entries = synchronized(lock) { buffer.snapshot() }
        return buildString {
            append(header())
            append("\n--- recent log ---\n")
            if (entries.isEmpty()) {
                append("(nothing logged yet)\n")
            } else {
                entries.forEach { append(it.format()).append('\n') }
            }
        }
    }

    /** Snapshot plus the crash report from the previous run, if there is one. */
    fun fullReportText(): String {
        val crash = lastCrashFile?.takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }
        return buildString {
            append(snapshotText())
            if (!crash.isNullOrBlank()) {
                append("\n--- crash report (previous run) ---\n")
                append(crash)
            }
        }
    }

    /** Writes a combined report to a file the user can share or export. */
    fun writeReportFile(): File? {
        val d = dir ?: return null
        return runCatching {
            File(d, "report-${stamp()}.txt").apply { writeText(fullReportText()) }
        }.getOrNull()
    }

    fun clear() {
        synchronized(lock) { buffer.clear() }
        dir?.let { d ->
            runCatching {
                File(d, CURRENT).delete()
                File(d, PREVIOUS).delete()
                d.listFiles { f -> f.name.startsWith(CRASH_PREFIX) }?.forEach { it.delete() }
            }
        }
        lastCrashFile = null
        i("App", "Diagnostics cleared")
    }

    private fun header(): String = buildString {
        append("FloatScript diagnostics\n")
        append("Version  : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        append("Android  : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        append("Device   : ${Build.MANUFACTURER} ${Build.MODEL}\n")
        append("Generated: ${stamp()}\n")
    }

    private fun stamp(): String = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrashReport(thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashReport(thread: Thread, throwable: Throwable) {
        val d = dir ?: return
        val text = buildString {
            append(header())
            append("\n--- FATAL EXCEPTION ---\n")
            append("Thread: ${thread.name}\n")
            append(throwable.stackTraceToString())
            append("\n--- recent log ---\n")
            synchronized(lock) { buffer.snapshot() }.forEach { append(it.format()).append('\n') }
        }
        runCatching {
            File(d, "$CRASH_PREFIX${stamp()}.txt").apply { writeText(text) }
        }.onSuccess { lastCrashFile = it }
    }
}
