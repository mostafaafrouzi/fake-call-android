package com.afrouzi.fakecall.tile

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.ScheduledCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EmergencyCallTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_emergency_call)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        val app = FakeCallApplication.from(this)

        // Haptic feedback
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        }

        app.applicationScope.launch(Dispatchers.IO) {
            val profiles = app.profileRepository.profiles.first()
            val firstProfile = profiles.firstOrNull()

            val callerName = firstProfile?.name ?: getString(R.string.home_default_caller_name)
            val callerNumber = firstProfile?.number ?: getString(R.string.home_default_caller_number)
            val photoPath = firstProfile?.photoPath
            val voicePath = firstProfile?.voicePath
            val ringtoneUri = firstProfile?.ringtoneUri
            val profileId = firstProfile?.id

            val triggerAt = System.currentTimeMillis() + 20_000L

            val call = ScheduledCall(
                profileId = profileId,
                callerName = callerName,
                callerNumber = callerNumber,
                photoPath = photoPath,
                voicePath = voicePath,
                ringtoneUri = ringtoneUri,
                triggerAtMillis = triggerAt,
                createdAt = System.currentTimeMillis(),
            )

            val insertedId = app.scheduleRepository.scheduleCall(call)
            app.callScheduler.schedule(call.copy(id = insertedId))

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@EmergencyCallTileService,
                    getString(R.string.tile_call_scheduled),
                    Toast.LENGTH_LONG
                ).show()

                qsTile?.let {
                    it.state = Tile.STATE_ACTIVE
                    it.updateTile()
                }
            }
        }
    }
}
