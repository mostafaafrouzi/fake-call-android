package com.afrouzi.fakecall.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.afrouzi.fakecall.data.model.VoiceScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class VoiceScenarioManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var previewPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        }
    }

    fun playScenarioPreview(
        scenario: VoiceScenario,
        fallbackText: String,
        onFinished: () -> Unit
    ) {
        stopPreview()

        // 1. If scenario has a bundled asset file, play directly via MediaPlayer!
        if (!scenario.assetFileName.isNullOrBlank()) {
            runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    try {
                        val afd = context.assets.openFd(scenario.assetFileName)
                        setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        afd.close()
                    } catch (e: Exception) {
                        // In case asset is compressed, extract to temp file
                        val tempFile = File(context.cacheDir, "preview_${System.currentTimeMillis()}.wav")
                        context.assets.open(scenario.assetFileName).use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        setDataSource(tempFile.absolutePath)
                    }
                    prepare()
                    setOnCompletionListener {
                        stopPreview()
                        mainHandler.post { onFinished() }
                    }
                    start()
                }
                previewPlayer = player
                return
            }.onFailure {
                stopPreview()
                mainHandler.post { onFinished() }
            }
            return
        }

        // 2. Otherwise use TTS
        playTtsPreview(fallbackText, scenario.languageCode, onFinished)
    }

    fun playTtsPreview(
        text: String,
        languageCode: String,
        onFinished: () -> Unit
    ) {
        stopPreview()
        if (!isInitialized || text.isBlank()) {
            onFinished()
            return
        }

        val locale = if (languageCode == "fa") Locale("fa", "IR") else Locale.ENGLISH
        tts?.language = locale

        val utteranceId = "preview_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    mainHandler.post { onFinished() }
                }
            }
            override fun onError(id: String?) {
                if (id == utteranceId) {
                    mainHandler.post { onFinished() }
                }
            }
        })

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            onFinished()
        }
    }

    fun stopPreview() {
        runCatching {
            previewPlayer?.stop()
            previewPlayer?.release()
        }
        previewPlayer = null

        runCatching {
            tts?.stop()
        }
    }

    suspend fun applyScenario(
        mediaManager: ProfileMediaManager,
        scenario: VoiceScenario,
        fallbackText: String
    ): String? = withContext(Dispatchers.IO) {
        // If scenario has a bundled asset file, copy it directly!
        if (!scenario.assetFileName.isNullOrBlank()) {
            val copied = copyAssetToStorage(mediaManager, scenario.assetFileName)
            if (copied != null) return@withContext copied
        }

        // Otherwise synthesize via TTS
        synthesizeSpeechToFile(mediaManager, fallbackText, scenario.languageCode)
    }

    suspend fun copyAssetToStorage(
        mediaManager: ProfileMediaManager,
        assetPath: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.assets.open(assetPath).use { stream ->
                mediaManager.copyStreamToInternalStorage(stream, "scenario_voice", "wav")
            }
        }.getOrNull()
    }

    suspend fun synthesizeSpeechToFile(
        mediaManager: ProfileMediaManager,
        text: String,
        languageCode: String
    ): String? = withContext(Dispatchers.IO) {
        if (!isInitialized || text.isBlank()) return@withContext null

        val locale = if (languageCode == "fa") Locale("fa", "IR") else Locale.ENGLISH
        tts?.language = locale

        val tempFile = File(context.cacheDir, "tts_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.wav")
        val utteranceId = "synth_${System.currentTimeMillis()}"

        val success = suspendCancellableCoroutine<Boolean> { continuation ->
            val listener = object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(true)
                    }
                }
                override fun onError(id: String?) {
                    if (id == utteranceId && continuation.isActive) {
                        continuation.resume(false)
                    }
                }
            }

            tts?.setOnUtteranceProgressListener(listener)
            val params = Bundle()
            val result = tts?.synthesizeToFile(text, params, tempFile, utteranceId)
            if (result != TextToSpeech.SUCCESS && continuation.isActive) {
                continuation.resume(false)
            }
        }

        if (success && tempFile.exists() && tempFile.length() > 0) {
            tempFile.inputStream().use { stream ->
                mediaManager.copyStreamToInternalStorage(stream, "tts_voice", "wav")
            }.also {
                tempFile.delete()
            }
        } else {
            tempFile.delete()
            null
        }
    }

    fun shutdown() {
        stopPreview()
        tts?.shutdown()
        tts = null
    }
}
