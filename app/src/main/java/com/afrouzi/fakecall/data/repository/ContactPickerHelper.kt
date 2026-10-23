package com.afrouzi.fakecall.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File

data class PickedContactResult(
    val name: String? = null,
    val number: String? = null,
    val photoPath: String? = null
)

object ContactPickerHelper {
    private const val TAG = "ContactPickerHelper"

    suspend fun extractContact(
        context: Context,
        mediaManager: ProfileMediaManager,
        pickedUri: Uri
    ): PickedContactResult = withContext(Dispatchers.IO) {
        val cr = context.contentResolver
        var pickedName: String? = null
        var pickedNumber: String? = null
        var photoUriStr: String? = null
        var photoThumbUriStr: String? = null
        var contactId: Long? = null
        var photoId: Long? = null

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
            ContactsContract.CommonDataKinds.Phone.PHOTO_ID,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
        )

        runCatching {
            cr.query(pickedUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val thumbIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                    val photoIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_ID)
                    val contactIdIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                    if (nameIdx >= 0) pickedName = cursor.getString(nameIdx)
                    if (numIdx >= 0) pickedNumber = cursor.getString(numIdx)
                    if (photoIdx >= 0) photoUriStr = cursor.getString(photoIdx)
                    if (thumbIdx >= 0) photoThumbUriStr = cursor.getString(thumbIdx)

                    if (photoIdIdx >= 0 && !cursor.isNull(photoIdIdx)) {
                        photoId = cursor.getLong(photoIdIdx)
                    }
                    if (contactIdIdx >= 0 && !cursor.isNull(contactIdIdx)) {
                        contactId = cursor.getLong(contactIdIdx)
                    }
                }
            }
        }.onFailure { Log.e(TAG, "Error querying picked contact URI: $pickedUri", it) }

        // Fallback: If contactId wasn't found directly, try parsing from URI if it's a contact URI
        if (contactId == null) {
            runCatching {
                val lastSeg = pickedUri.lastPathSegment?.toLongOrNull()
                if (lastSeg != null && pickedUri.toString().contains("contacts")) {
                    contactId = lastSeg
                }
            }
        }

        // Fallback: If number is still null and we have contactId, query phone number
        if (pickedNumber.isNullOrBlank() && contactId != null && contactId > 0) {
            runCatching {
                cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        pickedNumber = phoneCursor.getString(0)
                    }
                }
            }.onFailure { Log.w(TAG, "Fallback phone query failed for contact $contactId", it) }
        }

        // Clean number
        val cleanNumber = pickedNumber
            ?.replace(" ", "")
            ?.replace("-", "")
            ?.replace("(", "")
            ?.replace(")", "")
            ?.replace("\u00A0", "")

        // Photo extraction with multiple tiers of fallback
        var savedPhotoPath: String? = null

        // Tier 1: Try PHOTO_URI (Full resolution contact photo URI)
        if (!photoUriStr.isNullOrBlank()) {
            runCatching {
                cr.openInputStream(Uri.parse(photoUriStr))?.use { stream ->
                    savedPhotoPath = mediaManager.copyStreamToInternalStorage(stream, "contact_photo", "jpg")
                }
            }.onFailure { Log.w(TAG, "Tier 1: Failed to open photoUri: $photoUriStr", it) }
        }

        // Tier 2: Try Contacts.openContactPhotoInputStream (High-res display photo)
        if (savedPhotoPath == null && contactId != null && contactId > 0) {
            runCatching {
                val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri, true)?.use { stream ->
                    savedPhotoPath = mediaManager.copyStreamToInternalStorage(stream, "contact_photo", "jpg")
                }
            }.onFailure { Log.w(TAG, "Tier 2 (high-res): Failed openContactPhotoInputStream for contact $contactId", it) }
        }

        // Tier 3: Try Contacts.openContactPhotoInputStream (Standard/thumbnail resolution)
        if (savedPhotoPath == null && contactId != null && contactId > 0) {
            runCatching {
                val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
                ContactsContract.Contacts.openContactPhotoInputStream(cr, contactUri, false)?.use { stream ->
                    savedPhotoPath = mediaManager.copyStreamToInternalStorage(stream, "contact_photo", "jpg")
                }
            }.onFailure { Log.w(TAG, "Tier 3 (standard): Failed openContactPhotoInputStream for contact $contactId", it) }
        }

        // Tier 4: Try PHOTO_THUMBNAIL_URI
        if (savedPhotoPath == null && !photoThumbUriStr.isNullOrBlank()) {
            runCatching {
                cr.openInputStream(Uri.parse(photoThumbUriStr))?.use { stream ->
                    savedPhotoPath = mediaManager.copyStreamToInternalStorage(stream, "contact_photo", "jpg")
                }
            }.onFailure { Log.w(TAG, "Tier 4: Failed to open photoThumbUri: $photoThumbUriStr", it) }
        }

        // Tier 5: Query raw Photo BLOB directly from ContactsContract.Data
        if (savedPhotoPath == null && photoId != null && photoId > 0) {
            runCatching {
                cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Photo.PHOTO),
                    "${ContactsContract.Data._ID} = ?",
                    arrayOf(photoId.toString()),
                    null
                )?.use { photoCursor ->
                    if (photoCursor.moveToFirst()) {
                        val blobIdx = photoCursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO)
                        if (blobIdx >= 0 && !photoCursor.isNull(blobIdx)) {
                            val blob = photoCursor.getBlob(blobIdx)
                            if (blob != null && blob.isNotEmpty()) {
                                savedPhotoPath = mediaManager.copyStreamToInternalStorage(
                                    ByteArrayInputStream(blob),
                                    "contact_photo",
                                    "jpg"
                                )
                            }
                        }
                    }
                }
            }.onFailure { Log.w(TAG, "Tier 5: Failed to read photo blob for photoId $photoId", it) }
        }

        // Validate that the saved photo file actually exists and is not 0 bytes
        val validPhotoPath = savedPhotoPath?.takeIf { path ->
            val file = File(path)
            file.exists() && file.length() > 0
        }

        Log.d(TAG, "Extracted contact: name=$pickedName, number=$cleanNumber, photoPath=$validPhotoPath")

        PickedContactResult(
            name = pickedName,
            number = cleanNumber,
            photoPath = validPhotoPath
        )
    }
}
