package com.floatscript.app.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Thin wrapper around [SpeechRecognizer] used for the optional speech-following
 * mode. It reports recognised words one at a time and automatically restarts
 * listening so the teleprompter can track a continuous read.
 *
 * Callers must hold the RECORD_AUDIO permission before [start].
 *
 * Errors are bounded: a fatal error (missing permission / recogniser not
 * available) stops the follower, and repeated transient errors stop it after
 * [MAX_CONSECUTIVE_ERRORS] so a misconfigured device cannot spin in a
 * restart loop and drain the battery.
 */
class SpeechFollower(
    private val context: Context,
    private val onWords: (List<String>) -> Unit,
    private val onError: (Int) -> Unit
) {

    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var consecutiveErrors = 0

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            onError(error)
            if (isFatal(error)) {
                stop()
                return
            }
            consecutiveErrors++
            if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                stop()
                return
            }
            if (listening) restart()
        }

        override fun onResults(results: Bundle?) {
            consecutiveErrors = 0
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) {
                onWords(text.split(' ', '\n').filter { it.isNotBlank() })
            }
            if (listening) restart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            consecutiveErrors = 0
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) onWords(text.split(' ').filter { it.isNotBlank() })
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private val intent: Intent
        get() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            return
        }
        if (!isAvailable) {
            onError(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        consecutiveErrors = 0
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
        }
        listening = true
        runCatching { recognizer?.startListening(intent) }
    }

    fun stop() {
        listening = false
        runCatching {
            recognizer?.stopListening()
            recognizer?.cancel()
            recognizer?.destroy()
        }
        recognizer = null
    }

    private fun restart() {
        runCatching {
            recognizer?.cancel()
            recognizer?.startListening(intent)
        }
    }

    private fun isFatal(error: Int) = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

    private companion object {
        const val MAX_CONSECUTIVE_ERRORS = 5
    }
}
