package com.jegly.pages.core.tts

data class TtsState(
    val engineType: String = "SYSTEM",
    val selectedVoiceId: String = "",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val speed: Float = 1.0f,
    val sentenceIndex: Int = -1,
    val sentenceStart: Int = -1,
    val sentenceEnd: Int = -1,
    val errorMessage: String? = null,
) {
    val isActive: Boolean get() = isPlaying || isPaused
}
