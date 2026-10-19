package com.floatscript.app.data

/** Content shown to first-time users so the app is never empty. */
object SampleScripts {
    const val SAMPLE_TITLE = "Welcome to FloatScript"

    val SAMPLE_BODY = """
        Welcome to FloatScript — a transparent teleprompter that floats above
        your camera so you can read your script while you record.

        To get started:
        1. Tap "Start teleprompter" on the home screen.
        2. Grant the "display over other apps" permission if prompted.
        3. Open your camera app — the script stays on screen.
        4. Drag the handle to move it, pinch the corner to resize.
        5. Use the floating controls to play, pause and change speed.

        You can edit this script, create new ones, or import a .txt file.
        Everything is saved on your device.
    """.trimIndent()
}
