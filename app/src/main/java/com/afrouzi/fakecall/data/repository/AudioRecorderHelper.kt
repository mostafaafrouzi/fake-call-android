package com.afrouzi.fakecall.data.repository

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    var isRecording: Boolean = false
        private set

    fun startRecording(outputFile: File): Boolean {
        if (isRecording) return false
        return try {
            currentOutputFile = outputFile
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.stop()
            val file = currentOutputFile
            releaseRecorder()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            releaseRecorder()
            null
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        releaseRecorder()
        currentOutputFile?.delete()
        currentOutputFile = null
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
        isRecording = false
    }
}
