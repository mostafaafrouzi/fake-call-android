package com.afrouzi.fakecall.data.repository

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.afrouzi.fakecall.data.model.VoiceScenario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class VoiceScenarioManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
        }
    }

    fun playPreview(text: String, languageCode: String) {
        if (!isInitialized) return
        val locale = if (languageCode == "fa") Locale("fa", "IR") else Locale.ENGLISH
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview_${System.currentTimeMillis()}")
    }

    fun stopPreview() {
        tts?.stop()
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
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
