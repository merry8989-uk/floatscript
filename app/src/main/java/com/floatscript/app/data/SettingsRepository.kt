package com.floatscript.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.floatscript.app.model.OverlayGeometry
import com.floatscript.app.model.ScrollMode
import com.floatscript.app.model.ScriptSettings
import com.floatscript.app.model.TextAlign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "floatscript_settings")

/**
 * Persists the whole teleprompter configuration, the floating window geometry
 * and the id of the last script used, all in a single Preferences DataStore.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val TEXT_COLOR = intPreferencesKey("text_color")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val BOLD = booleanPreferencesKey("bold")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val ALIGN = stringPreferencesKey("align")
        val BG_OPACITY = floatPreferencesKey("bg_opacity")
        val BG_COLOR = intPreferencesKey("bg_color")
        val MARGIN = intPreferencesKey("margin")
        val SPEED = floatPreferencesKey("speed")
        val SCROLL_MODE = stringPreferencesKey("scroll_mode")
        val COUNTDOWN = intPreferencesKey("countdown")
        val AUTO_HIDE = booleanPreferencesKey("auto_hide")
        val MIRROR_H = booleanPreferencesKey("mirror_h")
        val FLIP_V = booleanPreferencesKey("flip_v")
        val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
        val SPEECH_FOLLOW = booleanPreferencesKey("speech_follow")
        val MINIMAL = booleanPreferencesKey("minimal")
        val LAST_SCRIPT_ID = longPreferencesKey("last_script_id")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        val GEO_XF = floatPreferencesKey("geo_x_fraction")
        val GEO_YF = floatPreferencesKey("geo_y_fraction")
        val GEO_WF = floatPreferencesKey("geo_w_fraction")
        val GEO_HF = floatPreferencesKey("geo_h_fraction")
    }

    private val prefs: Flow<Preferences> = context.dataStore.data

    val settings: Flow<ScriptSettings> = prefs.map { p ->
        val d = ScriptSettings()
        ScriptSettings(
            textColor = Color(p[Keys.TEXT_COLOR] ?: d.textColor.toArgb()),
            fontSizeSp = p[Keys.FONT_SIZE] ?: d.fontSizeSp,
            bold = p[Keys.BOLD] ?: d.bold,
            lineHeightMultiplier = p[Keys.LINE_HEIGHT] ?: d.lineHeightMultiplier,
            align = p[Keys.ALIGN]?.let { runCatching { TextAlign.valueOf(it) }.getOrNull() } ?: d.align,
            backgroundOpacity = p[Keys.BG_OPACITY] ?: d.backgroundOpacity,
            backgroundColor = Color(p[Keys.BG_COLOR] ?: d.backgroundColor.toArgb()),
            horizontalMarginDp = p[Keys.MARGIN] ?: d.horizontalMarginDp,
            scrollSpeedPxPerSec = p[Keys.SPEED] ?: d.scrollSpeedPxPerSec,
            scrollMode = p[Keys.SCROLL_MODE]?.let { runCatching { ScrollMode.valueOf(it) }.getOrNull() } ?: d.scrollMode,
            countdownSeconds = p[Keys.COUNTDOWN] ?: d.countdownSeconds,
            autoHideControls = p[Keys.AUTO_HIDE] ?: d.autoHideControls,
            mirrorHorizontally = p[Keys.MIRROR_H] ?: d.mirrorHorizontally,
            flipVertically = p[Keys.FLIP_V] ?: d.flipVertically,
            keepScreenAwake = p[Keys.KEEP_AWAKE] ?: d.keepScreenAwake,
            speechFollowEnabled = p[Keys.SPEECH_FOLLOW] ?: d.speechFollowEnabled,
            minimalMode = p[Keys.MINIMAL] ?: d.minimalMode
        )
    }

    val geometry: Flow<OverlayGeometry> = prefs.map { p ->
        val d = OverlayGeometry()
        OverlayGeometry(
            xFraction = p[Keys.GEO_XF] ?: d.xFraction,
            yFraction = p[Keys.GEO_YF] ?: d.yFraction,
            widthFraction = p[Keys.GEO_WF] ?: d.widthFraction,
            heightFraction = p[Keys.GEO_HF] ?: d.heightFraction
        )
    }

    val lastScriptId: Flow<Long> = prefs.map { it[Keys.LAST_SCRIPT_ID] ?: 0L }

    /** Timestamp (ms) of the last automatic update check. */
    val lastUpdateCheck: Flow<Long> = prefs.map { it[Keys.LAST_UPDATE_CHECK] ?: 0L }

    suspend fun saveSettings(s: ScriptSettings) {
        context.dataStore.edit { p ->
            p[Keys.TEXT_COLOR] = s.textColor.toArgb()
            p[Keys.FONT_SIZE] = s.fontSizeSp
            p[Keys.BOLD] = s.bold
            p[Keys.LINE_HEIGHT] = s.lineHeightMultiplier
            p[Keys.ALIGN] = s.align.name
            p[Keys.BG_OPACITY] = s.backgroundOpacity
            p[Keys.BG_COLOR] = s.backgroundColor.toArgb()
            p[Keys.MARGIN] = s.horizontalMarginDp
            p[Keys.SPEED] = s.scrollSpeedPxPerSec
            p[Keys.SCROLL_MODE] = s.scrollMode.name
            p[Keys.COUNTDOWN] = s.countdownSeconds
            p[Keys.AUTO_HIDE] = s.autoHideControls
            p[Keys.MIRROR_H] = s.mirrorHorizontally
            p[Keys.FLIP_V] = s.flipVertically
            p[Keys.KEEP_AWAKE] = s.keepScreenAwake
            p[Keys.SPEECH_FOLLOW] = s.speechFollowEnabled
            p[Keys.MINIMAL] = s.minimalMode
        }
    }

    suspend fun saveGeometry(g: OverlayGeometry) {
        context.dataStore.edit { p ->
            p[Keys.GEO_XF] = g.xFraction
            p[Keys.GEO_YF] = g.yFraction
            p[Keys.GEO_WF] = g.widthFraction
            p[Keys.GEO_HF] = g.heightFraction
        }
    }

    suspend fun setLastScriptId(id: Long) {
        context.dataStore.edit { it[Keys.LAST_SCRIPT_ID] = id }
    }

    suspend fun setLastUpdateCheck(timestampMs: Long) {
        context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK] = timestampMs }
    }
}
