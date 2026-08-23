package com.jegly.pages.core.tts.engine

interface TtsEngine {
    suspend fun initialize(): Boolean
    fun synthesizeAndPlay(
        sentence: String,
        speed: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    )
    fun stop()
    fun pause()
    fun resume()
    fun shutdown()
}
