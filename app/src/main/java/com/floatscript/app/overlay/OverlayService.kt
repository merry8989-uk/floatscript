package com.floatscript.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
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
                app.scriptRepository.get(scriptId)?.let { state.setScript(it.title, it.body) }
            }

            buildOverlay(geometry)

            // Close the window when the user taps the close button.
            lifecycleScope.launch {
                state.closeRequested.collect { requested ->
                    if (requested) stopSelf()
                }
            }
        }
    }

    private fun buildOverlay(geometry: OverlayGeometry) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val width = if (geometry.hasPosition && geometry.width > 0) geometry.width else screenWidth
        val height = if (geometry.height > 0) geometry.height else (screenHeight * 0.6f).toInt()

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
                    onResize = ::onResize,
                    onSettingsPersist = { persistSettings(it) }
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

    private fun persistSettings(settings: ScriptSettings) {
        val app = application as FloatScriptApp
        lifecycleScope.launch { app.settingsRepository.saveSettings(settings) }
        // Reflect the keep-awake flag on the live window.
        params?.let { lp ->
            val keepAwakeBit = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            val has = (lp.flags and keepAwakeBit) != 0
            if (settings.keepScreenAwake && !has) {
                lp.flags = lp.flags or keepAwakeBit
                applyLayoutUpdate(lp)
            } else if (!settings.keepScreenAwake && has) {
                lp.flags = lp.flags and keepAwakeBit.inv()
                applyLayoutUpdate(lp)
            }
        }
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
                if (id != 0L) {
                    val app = application as FloatScriptApp
                    lifecycleScope.launch {
                        val script = app.scriptRepository.get(id) ?: return@launch
                        if (::state.isInitialized) state.setScript(script.title, script.body)
                        app.settingsRepository.setLastScriptId(id)
                    }
                }
            }
        }
        return START_NOT_STICKY
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
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else 0
        )
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(OverlayConstants.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.overlay_action_stop), stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(OverlayConstants.ACTION_START)
            ContextCompatCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, OverlayService::class.java).setAction(OverlayConstants.ACTION_STOP)
            )
        }

        fun updateScript(context: Context, scriptId: Long) {
            if (!isRunning) return
            context.startService(
                Intent(context, OverlayService::class.java)
                    .setAction(OverlayConstants.ACTION_SET_SCRIPT)
                    .putExtra(OverlayConstants.EXTRA_SCRIPT_ID, scriptId)
            )
        }
    }
}

/** Small indirection so the version check lives in one place. */
private object ContextCompatCompat {
    fun startForegroundService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
