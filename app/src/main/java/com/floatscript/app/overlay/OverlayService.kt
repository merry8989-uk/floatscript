package com.floatscript.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.floatscript.app.FloatScriptApp
import com.floatscript.app.MainActivity
import com.floatscript.app.R
import com.floatscript.app.model.OverlayGeometry
import com.floatscript.app.model.ScriptSettings
import com.floatscript.app.ui.theme.FloatScriptTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the floating teleprompter window. It hosts a
 * ComposeView directly in the WindowManager (no Activity) and keeps the script
 * visible above other apps — including the system camera app when that app
 * permits overlays.
 */
class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var state: TeleprompterState
    private lateinit var lifecycleOwner: OverlayLifecycleOwner

    private var rootView: ComposeView? = null
    private var params: WindowManager.LayoutParams? = null

    private var density = 1f
    private var screenWidth = 0
    private var screenHeight = 0

    private var saveGeometryJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        val app = application as FloatScriptApp
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        density = resources.displayMetrics.density
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        createNotificationChannel()
        startInForeground()

        // Build the window once the persisted configuration and last script
        // have been read. All of this runs on the main dispatcher.
        lifecycleScope.launch {
            val settings = app.settingsRepository.settings.first()
            val geometry = app.settingsRepository.geometry.first()
            val scriptId = app.settingsRepository.lastScriptId.first()

            state = TeleprompterState(settings)

            if (scriptId != 0L) {
                app.scriptRepository.get(scriptId)?.let { state.setScript(it.id, it.title, it.body) }
            }

            buildOverlay(geometry)
            observeState(app)
        }
    }

    /**
     * Keeps the overlay and the persisted settings in sync in both directions:
     *  - changes made in the Settings screen reach a running overlay
     *  - changes made with the floating controls are written back to DataStore
     * The two flows converge because equal values are dropped by StateFlow.
     */
    private fun observeState(app: FloatScriptApp) {
        // DataStore -> overlay
        lifecycleScope.launch {
            app.settingsRepository.settings.collect { state.applySettings(it) }
        }
        // overlay -> DataStore (+ live keep-awake flag)
        lifecycleScope.launch {
            state.settings.drop(1).distinctUntilChanged().collect { s ->
                app.settingsRepository.saveSettings(s)
                applyKeepAwakeFlag(s)
            }
        }
        // Reflect play/pause in the notification action label.
        lifecycleScope.launch {
            state.isPlaying.distinctUntilChanged().collect { playing ->
                NotificationManagerCompat.from(this@OverlayService)
                    .notify(OverlayConstants.NOTIFICATION_ID, buildNotification(playing))
            }
        }
        // Close the window when the user taps the close button.
        lifecycleScope.launch {
            state.closeRequested.collect { requested -> if (requested) stopSelf() }
        }
    }

    private fun buildOverlay(geometry: OverlayGeometry) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val width = if (geometry.hasPosition && geometry.width > 0) geometry.width else screenWidth
        val height = if (geometry.height > 0) {
            geometry.height
        } else {
            (screenHeight * OverlayConstants.DEFAULT_HEIGHT_FRACTION).toInt()
        }

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (state.settings.value.keepScreenAwake) {
            flags = flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        }

        val lp = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (geometry.hasPosition) geometry.x else 0
            y = geometry.y
        }
        params = lp

        lifecycleOwner = OverlayLifecycleOwner().also { it.onCreate() }

        val view = ComposeView(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            lifecycleOwner.attachTo(this)
        }
        rootView = view

        // Attach the view to the window first, then move the owner to RESUMED,
        // and only then start the composition.
        windowManager.addView(view, lp)
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        view.setContent {
            FloatScriptTheme(darkTheme = true) {
                TeleprompterView(
                    state = state,
                    onDrag = ::onDrag,
                    onResize = ::onResize
                )
            }
        }
    }

    // ---- Window interactions -------------------------------------------------

    private fun onDrag(dx: Float, dy: Float) {
        val lp = params ?: return
        lp.x = (lp.x + dx.toInt()).coerceIn(0, (screenWidth - lp.width).coerceAtLeast(0))
        lp.y = (lp.y + dy.toInt()).coerceIn(0, (screenHeight - lp.height).coerceAtLeast(0))
        applyLayoutUpdate(lp)
    }

    private fun onResize(dx: Float, dy: Float) {
        val lp = params ?: return
        val minW = (OverlayConstants.MIN_WINDOW_WIDTH_DP * density).toInt()
        val minH = (OverlayConstants.MIN_WINDOW_HEIGHT_DP * density).toInt()
        lp.width = (lp.width + dx.toInt()).coerceIn(minW, screenWidth)
        lp.height = (lp.height + dy.toInt()).coerceIn(minH, screenHeight)
        applyLayoutUpdate(lp)
    }

    private fun applyLayoutUpdate(lp: WindowManager.LayoutParams) {
        rootView?.let { runCatching { windowManager.updateViewLayout(it, lp) } }
        scheduleGeometrySave(lp)
    }

    private fun scheduleGeometrySave(lp: WindowManager.LayoutParams) {
        saveGeometryJob?.cancel()
        saveGeometryJob = lifecycleScope.launch {
            delay(500)
            val app = application as FloatScriptApp
            app.settingsRepository.saveGeometry(
                OverlayGeometry(x = lp.x, y = lp.y, width = lp.width, height = lp.height)
            )
        }
    }

    private fun applyKeepAwakeFlag(settings: ScriptSettings) {
        val lp = params ?: return
        val bit = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        val has = (lp.flags and bit) != 0
        if (settings.keepScreenAwake && !has) {
            lp.flags = lp.flags or bit
            rootView?.let { runCatching { windowManager.updateViewLayout(it, lp) } }
        } else if (!settings.keepScreenAwake && has) {
            lp.flags = lp.flags and bit.inv()
            rootView?.let { runCatching { windowManager.updateViewLayout(it, lp) } }
        }
    }

    /**
     * Re-clamps the floating window when the device is rotated, so a window
     * saved in portrait doesn't end up off-screen in landscape (and vice versa).
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        density = resources.displayMetrics.density
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        val lp = params ?: return
        lp.width = lp.width.coerceIn(
            (OverlayConstants.MIN_WINDOW_WIDTH_DP * density).toInt(),
            screenWidth
        )
        lp.height = lp.height.coerceIn(
            (OverlayConstants.MIN_WINDOW_HEIGHT_DP * density).toInt(),
            screenHeight
        )
        lp.x = lp.x.coerceIn(0, (screenWidth - lp.width).coerceAtLeast(0))
        lp.y = lp.y.coerceIn(0, (screenHeight - lp.height).coerceAtLeast(0))
        applyLayoutUpdate(lp)
    }

    // ---- Intents -------------------------------------------------------------

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            OverlayConstants.ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            OverlayConstants.ACTION_TOGGLE_PLAY -> if (::state.isInitialized) state.togglePlay()
            OverlayConstants.ACTION_RESTART -> if (::state.isInitialized) state.restart()
            OverlayConstants.ACTION_SET_SCRIPT -> {
                val id = intent.getLongExtra(OverlayConstants.EXTRA_SCRIPT_ID, 0L)
                if (id != 0L) loadScript(id, onlyIfCurrent = false)
            }
            OverlayConstants.ACTION_SET_SCRIPT_IF_CURRENT -> {
                val id = intent.getLongExtra(OverlayConstants.EXTRA_SCRIPT_ID, 0L)
                if (id != 0L) loadScript(id, onlyIfCurrent = true)
            }
        }
        return START_NOT_STICKY
    }

    private fun loadScript(id: Long, onlyIfCurrent: Boolean) {
        val app = application as FloatScriptApp
        lifecycleScope.launch {
            if (onlyIfCurrent && (!::state.isInitialized || state.currentScriptId != id)) return@launch
            val script = app.scriptRepository.get(id) ?: return@launch
            if (::state.isInitialized) state.setScript(script.id, script.title, script.body)
            app.settingsRepository.setLastScriptId(id)
        }
    }

    override fun onDestroy() {
        isRunning = false
        saveGeometryJob?.cancel()
        rootView?.let { runCatching { windowManager.removeView(it) } }
        if (::lifecycleOwner.isInitialized) lifecycleOwner.onDestroy()
        rootView = null
        params = null
        super.onDestroy()
    }

    // ---- Foreground notification --------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OverlayConstants.CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_desc)
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startInForeground() {
        ServiceCompat.startForeground(
            this,
            OverlayConstants.NOTIFICATION_ID,
            buildNotification(playing = false),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )
    }

    private fun buildNotification(playing: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        fun serviceAction(requestCode: Int, action: String): PendingIntent =
            PendingIntent.getService(
                this,
                requestCode,
                Intent(this, OverlayService::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

        return NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                0,
                getString(if (playing) R.string.cd_pause else R.string.cd_play),
                serviceAction(1, OverlayConstants.ACTION_TOGGLE_PLAY)
            )
            .addAction(
                0,
                getString(R.string.cd_restart),
                serviceAction(2, OverlayConstants.ACTION_RESTART)
            )
            .addAction(
                0,
                getString(R.string.overlay_action_stop),
                serviceAction(3, OverlayConstants.ACTION_STOP)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(OverlayConstants.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayService::class.java).setAction(OverlayConstants.ACTION_STOP)
            )
        }

        /** Makes [scriptId] the script shown by the overlay (starting it if needed). */
        fun updateScript(context: Context, scriptId: Long) {
            if (!isRunning) return
            context.startService(
                Intent(context, OverlayService::class.java)
                    .setAction(OverlayConstants.ACTION_SET_SCRIPT)
                    .putExtra(OverlayConstants.EXTRA_SCRIPT_ID, scriptId)
            )
        }

        /** Reloads [scriptId] only if it is the script currently on screen. */
        fun updateIfCurrent(context: Context, scriptId: Long) {
            if (!isRunning) return
            context.startService(
                Intent(context, OverlayService::class.java)
                    .setAction(OverlayConstants.ACTION_SET_SCRIPT_IF_CURRENT)
                    .putExtra(OverlayConstants.EXTRA_SCRIPT_ID, scriptId)
            )
        }
    }
}
