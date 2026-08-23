package com.jegly.pages.core.tts

import com.jegly.pages.core.datastore.PagesPreferencesDataSource
import com.jegly.pages.core.tts.engine.SherpaOnnxTtsEngine
import com.jegly.pages.core.tts.engine.SystemTtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class TtsManager @Inject constructor(
    private val systemEngine: SystemTtsEngine,
    private val sherpaEngine: SherpaOnnxTtsEngine,
    private val modelManager: ModelManager,
    private val dataSource: PagesPreferencesDataSource,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var systemInitialized = false

    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    // Emits one Unit when speak() finishes all sentences naturally (not via stop()).
    private val _pageCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pageCompleted: SharedFlow<Unit> = _pageCompleted.asSharedFlow()

    val availableVoices: List<VoiceModel> get() = modelManager.getAvailableVoices()

    init {
        scope.launch {
            dataSource.userPreferences.collect { prefs ->
                _state.update { it.copy(engineType = prefs.ttsEngine, selectedVoiceId = prefs.ttsVoiceId, speed = prefs.ttsSpeed) }
            }
        }
    }

    fun setEngine(type: String) {
        dataSource.setTtsEngine(type)
        if (type == "SYSTEM") dataSource.setTtsVoiceId("")
    }

    fun setVoice(voiceId: String) {
        dataSource.setTtsVoiceId(voiceId)
    }

    fun setSpeed(speed: Float) {
        dataSource.setTtsSpeed(speed)
    }

    fun speak(text: String, startSentence: Int = 0) {
        scope.launch {
            playJob?.cancelAndJoin()
            val sentences = SentenceChunker.chunk(text)
            if (sentences.isEmpty()) return@launch

            playJob = scope.launch {
                val snapshot = _state.value
                val engineType = snapshot.engineType
                val speed = snapshot.speed

                if (engineType == "NEURAL") {
                    val voiceId = snapshot.selectedVoiceId.ifEmpty { BUNDLED_VOICES.firstOrNull()?.id ?: "" }
                    if (voiceId.isEmpty()) {
                        _state.update { it.copy(errorMessage = "No voice selected.") }
                        return@launch
                    }
                    val loaded = sherpaEngine.loadVoice(voiceId)
                    if (!loaded) {
                        _state.update { it.copy(errorMessage = "Failed to load voice.") }
                        return@launch
                    }
                } else {
                    if (!systemInitialized) {
                        systemInitialized = systemEngine.initialize()
                    }
                }

                _state.update { it.copy(isPlaying = true, isPaused = false, errorMessage = null) }

                for (i in startSentence until sentences.size) {
                    val sentence = sentences[i]
                    _state.update {
                        it.copy(sentenceIndex = i, sentenceStart = sentence.start, sentenceEnd = sentence.end)
                    }
                    val engine = if (engineType == "NEURAL") sherpaEngine else systemEngine
                    suspendCancellableCoroutine<Unit> { cont ->
                        engine.synthesizeAndPlay(
                            sentence = sentence.text,
                            speed = speed,
                            onStart = {},
                            onDone = { if (cont.isActive) cont.resume(Unit) },
                            onError = { if (cont.isActive) cont.resume(Unit) },
                        )
                        cont.invokeOnCancellation { engine.stop() }
                    }
                }
                _state.update { it.copy(isPlaying = false, isPaused = false, sentenceIndex = -1, sentenceStart = -1, sentenceEnd = -1) }
                _pageCompleted.tryEmit(Unit)
            }
        }
    }

    fun pause() {
        val engine = if (_state.value.engineType == "NEURAL") sherpaEngine else systemEngine
        engine.pause()
        _state.update { it.copy(isPaused = true, isPlaying = false) }
    }

    fun resume() {
        val engine = if (_state.value.engineType == "NEURAL") sherpaEngine else systemEngine
        engine.resume()
        _state.update { it.copy(isPaused = false, isPlaying = true) }
    }

    fun stop() {
        playJob?.cancel()
        val engine = if (_state.value.engineType == "NEURAL") sherpaEngine else systemEngine
        engine.stop()
        _state.update { it.copy(isPlaying = false, isPaused = false, sentenceIndex = -1, sentenceStart = -1, sentenceEnd = -1) }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    fun shutdown() {
        stop()
        systemEngine.shutdown()
        sherpaEngine.shutdown()
    }
}
