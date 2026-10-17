package com.afrouzi.fakecall

import com.afrouzi.fakecall.data.model.CallerProfile
import com.afrouzi.fakecall.data.model.toScheduledCall
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ScheduledCallSnapshotTest {

    @Test
    fun toScheduledCall_createsAccurateSnapshot() {
        val originalProfile = CallerProfile(
            id = 42L,
            name = "Manager",
            number = "+989123456789",
            photoPath = "/data/user/0/com.afrouzi.fakecall/files/profiles/photo_123.jpg",
            voicePath = "/data/user/0/com.afrouzi.fakecall/files/profiles/voice_456.m4a",
            ringtoneUri = "content://media/internal/audio/media/7",
            createdAt = 1000L
        )

        val triggerTime = 250_000L
        val snapshot = originalProfile.toScheduledCall(triggerAtMillis = triggerTime)

        assertEquals(42L, snapshot.profileId)
        assertEquals("Manager", snapshot.callerName)
        assertEquals("+989123456789", snapshot.callerNumber)
        assertEquals("/data/user/0/com.afrouzi.fakecall/files/profiles/photo_123.jpg", snapshot.photoPath)
        assertEquals("/data/user/0/com.afrouzi.fakecall/files/profiles/voice_456.m4a", snapshot.voicePath)
        assertEquals("content://media/internal/audio/media/7", snapshot.ringtoneUri)
        assertEquals(250_000L, snapshot.triggerAtMillis)
    }

    @Test
    fun toScheduledCall_profileModificationsDoNotAffectSnapshot() {
        var profile = CallerProfile(
            id = 10L,
            name = "Alice",
            number = "09120000000",
            photoPath = "/path/to/alice.jpg"
        )

        val snapshot = profile.toScheduledCall(triggerAtMillis = 500_000L)

        // Simulate subsequent profile edit
        profile = profile.copy(
            name = "Bob",
            number = "09199999999",
            photoPath = "/path/to/bob.jpg"
        )

        // Snapshot must still retain original values
        assertEquals("Alice", snapshot.callerName)
        assertEquals("09120000000", snapshot.callerNumber)
        assertEquals("/path/to/alice.jpg", snapshot.photoPath)
        assertNotEquals(profile.name, snapshot.callerName)
        assertNotEquals(profile.number, snapshot.callerNumber)
    }
}
