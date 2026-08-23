package com.jegly.pages.core.tts

enum class VoiceQuality { Low, Medium, High }
enum class VoiceGender { Female, Male, Neutral }

data class VoiceModel(
    val id: String,
    val displayName: String,
    val language: String,
    val gender: VoiceGender,
    val quality: VoiceQuality,
    val sizeMb: Int,
    val onnxFileName: String,
)
