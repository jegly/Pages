package com.jegly.pages.core.tts

import javax.inject.Inject
import javax.inject.Singleton

// All voices are embedded in assets — no download logic needed.
@Singleton
class ModelManager @Inject constructor() {
    fun getAvailableVoices(): List<VoiceModel> = BUNDLED_VOICES
}
