package com.jegly.pages.core.tts.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.jegly.pages.core.tts.BUNDLED_VOICES
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SherpaOnnxTtsEngine"
private const val ASSETS_BASE = "tts_voices"

@Singleton
class SherpaOnnxTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TtsEngine {

    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    private var currentVoiceId: String? = null
    @Volatile private var shouldStop = false
    @Volatile private var paused = false

    override suspend fun initialize(): Boolean = true

    // Root of extracted voice data in internal storage.
    private fun extractedBase(): File = File(context.filesDir, "tts_voices")

    // Copies one asset file to disk.
    private fun copyAssetFile(assetPath: String, dest: File) {
        context.assets.open(assetPath).use { src ->
            dest.outputStream().use { src.copyTo(it, bufferSize = 65_536) }
        }
    }

    // Recursively copies an asset directory to disk.
    private fun copyAssetDir(assetPath: String, destDir: File) {
        val entries = context.assets.list(assetPath) ?: return
        for (entry in entries) {
            val childAsset = "$assetPath/$entry"
            val childDest = File(destDir, entry)
            if (!context.assets.list(childAsset).isNullOrEmpty()) {
                childDest.mkdirs()
                copyAssetDir(childAsset, childDest)
            } else {
                copyAssetFile(childAsset, childDest)
            }
        }
    }

    // Extracts voice + shared espeak-ng-data to internal storage on first use.
    // Returns the base directory path, or null on failure.
    private fun extractIfNeeded(voiceId: String, onnxFileName: String): File? {
        val base = extractedBase()
        val voiceDir = File(base, voiceId)
        val espeakDir = File(base, "espeak-ng-data")

        return try {
            if (!File(voiceDir, ".ok").exists()) {
                Log.d(TAG, "extracting voice $voiceId (~61 MB, first use only)")
                voiceDir.mkdirs()
                copyAssetFile("$ASSETS_BASE/$voiceId/$onnxFileName", File(voiceDir, onnxFileName))
                copyAssetFile("$ASSETS_BASE/$voiceId/tokens.txt", File(voiceDir, "tokens.txt"))
                File(voiceDir, ".ok").createNewFile()
                Log.d(TAG, "voice extraction done")
            }
            if (!File(espeakDir, ".ok").exists()) {
                Log.d(TAG, "extracting espeak-ng-data (~19 MB, once)")
                espeakDir.mkdirs()
                copyAssetDir("$ASSETS_BASE/espeak-ng-data", espeakDir)
                File(espeakDir, ".ok").createNewFile()
                Log.d(TAG, "espeak extraction done")
            }
            base
        } catch (e: Exception) {
            Log.e(TAG, "extraction failed", e)
            null
        }
    }

    suspend fun loadVoice(voiceId: String): Boolean = withContext(Dispatchers.IO) {
        if (voiceId == currentVoiceId && tts != null) return@withContext true
        tts?.let { runCatching { it.release() } }
        tts = null
        currentVoiceId = null

        val voice = BUNDLED_VOICES.find { it.id == voiceId } ?: return@withContext false

        val base = extractIfNeeded(voiceId, voice.onnxFileName) ?: return@withContext false

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "${base.absolutePath}/$voiceId/${voice.onnxFileName}",
                    tokens = "${base.absolutePath}/$voiceId/tokens.txt",
                    dataDir = "${base.absolutePath}/espeak-ng-data",
                    lexicon = "",
                    dictDir = "",
                ),
                numThreads = 2,
                debug = false,
                provider = "cpu",
            ),
            maxNumSentences = 1,
        )

        Log.d(TAG, "creating OfflineTts from extracted paths")
        val result = runCatching {
            tts = OfflineTts(config = config)  // no assetManager — file paths only
            currentVoiceId = voiceId
            true
        }
        Log.d(TAG, "OfflineTts result: success=${result.getOrNull()}, err=${result.exceptionOrNull()}")
        result.getOrElse { false }
    }

    override fun synthesizeAndPlay(
        sentence: String,
        speed: Float,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val engine = tts ?: run { onError(IllegalStateException("No voice loaded")); return }
        shouldStop = false
        paused = false
        try {
            onStart()
            val audio = engine.generate(text = sentence, sid = 0, speed = speed)
            val samples = audio.samples
            val sampleRate = audio.sampleRate
            if (shouldStop) return

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 4)
                .build()

            audioTrack?.release()
            audioTrack = track
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            if (!shouldStop) {
                track.play()
                val durationMs = (samples.size * 1000L / sampleRate)
                var waited = 0L
                val step = 50L
                while (!shouldStop && waited < durationMs + 500) {
                    if (!paused) waited += step
                    Thread.sleep(step)
                }
            }
            track.stop()
            track.release()
            audioTrack = null
            if (!shouldStop) onDone()
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun stop() {
        shouldStop = true
        audioTrack?.stop()
    }

    override fun pause() {
        paused = true
        audioTrack?.pause()
    }

    override fun resume() {
        paused = false
        audioTrack?.play()
    }

    override fun shutdown() {
        stop()
        audioTrack?.release()
        audioTrack = null
        runCatching { tts?.release() }
        tts = null
        currentVoiceId = null
    }
}
