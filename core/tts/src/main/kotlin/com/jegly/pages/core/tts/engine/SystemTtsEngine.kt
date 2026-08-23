package com.jegly.pages.core.tts.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SystemTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var ready = false

    override suspend fun initialize(): Boolean = suspendCancellableCoroutine { cont ->
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (cont.isActive) cont.resume(ready)
        }
    }

    fun setSpeedAndPitch(speed: Float, pitch: Float) {
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
    }

    override fun synthesizeAndPlay(
        sentence: String,
        speed: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = tts ?: run { onError(IllegalStateException("TTS not initialized")); return }
        engine.setSpeechRate(speed)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = onStart()
            override fun onDone(utteranceId: String?) = onDone()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = onError(RuntimeException("TTS error"))
        })
        engine.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "utt")
    }

    override fun stop() { tts?.stop() }
    override fun pause() { tts?.stop() }  // System TTS has no pause; stop is closest
    override fun resume() {}  // must call synthesizeAndPlay again from current sentence
    override fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null; ready = false }
}
