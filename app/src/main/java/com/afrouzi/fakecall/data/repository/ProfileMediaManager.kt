package com.afrouzi.fakecall.data.repository

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ProfileMediaManager(private val context: Context) {

    private val profilesDir: File
        get() = File(context.filesDir, "profiles").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    suspend fun copyUriToInternalStorage(uri: Uri, prefix: String, extension: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanExtension = extension.removePrefix(".")
                val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$cleanExtension"
                val targetFile = File(profilesDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                targetFile.absolutePath
            }.getOrNull()
        }

    suspend fun copyStreamToInternalStorage(inputStream: InputStream, prefix: String, extension: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanExtension = extension.removePrefix(".")
                val fileName = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$cleanExtension"
                val targetFile = File(profilesDir, fileName)

                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                targetFile.absolutePath
            }.getOrNull()
        }

    fun createNewAudioRecordFile(): File {
        val fileName = "voice_rec_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.m4a"
        return File(profilesDir, fileName)
    }

    suspend fun deleteFile(path: String?) = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext
        runCatching {
            val file = File(path)
            if (file.exists() && file.parentFile?.canonicalPath == profilesDir.canonicalPath) {
                file.delete()
            }
        }
    }
}
