package com.afrouzi.fakecall.data.repository

import android.net.Uri
import com.afrouzi.fakecall.data.db.CallerProfileDao
import com.afrouzi.fakecall.data.model.CallerProfile
import kotlinx.coroutines.flow.Flow

class CallerProfileRepository(
    private val dao: CallerProfileDao,
    private val mediaManager: ProfileMediaManager
) {
    val profiles: Flow<List<CallerProfile>> = dao.getAllProfiles()

    suspend fun getProfile(id: Long): CallerProfile? = dao.getProfileById(id)

    suspend fun saveProfile(profile: CallerProfile): Long {
        return if (profile.id == 0L) {
            dao.insertProfile(profile)
        } else {
            dao.updateProfile(profile)
            profile.id
        }
    }

    suspend fun deleteProfile(profile: CallerProfile) {
        mediaManager.deleteFile(profile.photoPath)
        mediaManager.deleteFile(profile.voicePath)
        dao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(id: Long) {
        val existing = dao.getProfileById(id)
        if (existing != null) {
            deleteProfile(existing)
        } else {
            dao.deleteProfileById(id)
        }
    }

    suspend fun copyPhotoUri(uri: Uri): String? {
        return mediaManager.copyUriToInternalStorage(uri, prefix = "photo", extension = "jpg")
    }

    suspend fun copyVoiceUri(uri: Uri): String? {
        return mediaManager.copyUriToInternalStorage(uri, prefix = "voice", extension = "m4a")
    }

    suspend fun deleteOldFile(path: String?) {
        mediaManager.deleteFile(path)
    }
}
