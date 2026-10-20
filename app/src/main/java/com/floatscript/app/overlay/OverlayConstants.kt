package com.floatscript.app.overlay

object OverlayConstants {
    const val CHANNEL_ID = "floatscript_overlay"
    const val NOTIFICATION_ID = 1001

    /** Actions broadcast from the notification / used as service intents. */
    const val ACTION_START = "com.floatscript.app.action.START"
    const val ACTION_STOP = "com.floatscript.app.action.STOP"
    const val ACTION_TOGGLE_PLAY = "com.floatscript.app.action.TOGGLE_PLAY"
    const val ACTION_RESTART = "com.floatscript.app.action.RESTART"

    /** Intents used by the UI to update the running overlay. */
    const val ACTION_SET_SCRIPT = "com.floatscript.app.action.SET_SCRIPT"
    const val ACTION_SET_SCRIPT_IF_CURRENT = "com.floatscript.app.action.SET_SCRIPT_IF_CURRENT"
    const val EXTRA_SCRIPT_ID = "extra_script_id"

    const val MIN_WINDOW_WIDTH_DP = 160
    const val MIN_WINDOW_HEIGHT_DP = 120

    /** Default floating-window height as a fraction of the screen. */
    const val DEFAULT_HEIGHT_FRACTION = 0.40f
}
