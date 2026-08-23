package com.jegly.pages.core.tts

// All voices are embedded in app assets under assets/tts_voices/.
// espeak-ng-data is shared across all voices at tts_voices/espeak-ng-data/.
// Each voice has its own subdirectory: tts_voices/<id>/<onnxFileName> + tokens.txt.
val BUNDLED_VOICES: List<VoiceModel> = listOf(
    VoiceModel(
        id = "en_US-lessac-low",
        displayName = "Lessac (US English · Female)",
        language = "en-US",
        gender = VoiceGender.Female,
        quality = VoiceQuality.Low,
        sizeMb = 61,
        onnxFileName = "en_US-lessac-low.onnx",
    ),
    VoiceModel(
        id = "en_US-ryan-low",
        displayName = "Ryan (US English · Male)",
        language = "en-US",
        gender = VoiceGender.Male,
        quality = VoiceQuality.Low,
        sizeMb = 61,
        onnxFileName = "en_US-ryan-low.onnx",
    ),
    VoiceModel(
        id = "en_GB-alan-low",
        displayName = "Alan (GB English · Male)",
        language = "en-GB",
        gender = VoiceGender.Male,
        quality = VoiceQuality.Low,
        sizeMb = 61,
        onnxFileName = "en_GB-alan-low.onnx",
    ),
    VoiceModel(
        id = "en_US-amy-low",
        displayName = "Amy (US English · Female)",
        language = "en-US",
        gender = VoiceGender.Female,
        quality = VoiceQuality.Low,
        sizeMb = 61,
        onnxFileName = "en_US-amy-low.onnx",
    ),
)
