package com.afrouzi.fakecall.call

import android.app.KeyguardManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.CallHistoryItem
import com.afrouzi.fakecall.data.model.CallOutcome
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.scheduling.CallAlarmReceiver
import com.afrouzi.fakecall.ui.theme.FakeCallTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class IncomingCallActivity : ComponentActivity() {

    private var ringtonePlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    private var callerName: String = ""
    private var callerNumber: String = ""
    private var photoPath: String? = null
    private var voicePath: String? = null
    private var ringtoneUriString: String? = null
    private var scheduledFor: Long = 0L

    private var isCallFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: getString(R.string.app_name)
        callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: ""
        photoPath = intent.getStringExtra(EXTRA_PHOTO_PATH)
        voicePath = intent.getStringExtra(EXTRA_VOICE_PATH)
        ringtoneUriString = intent.getStringExtra(EXTRA_RINGTONE_URI)
        scheduledFor = intent.getLongExtra(EXTRA_SCHEDULED_FOR, System.currentTimeMillis())

        val app = FakeCallApplication.from(this)

        startRingtoneAndVibration()

        setContent {
            val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())

            FakeCallTheme(settings = settings) {
                IncomingCallScreen(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    photoPath = photoPath,
                    onAnswer = { answerCall() },
                    onDecline = { declineCall() },
                    onEndCall = { duration -> endCall(duration) },
                    onSpeakerToggle = { on -> toggleSpeaker(on) },
                )
            }
        }
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun startRingtoneAndVibration() {
        // Dismiss the incoming call notification now that the activity is in foreground
        runCatching {
            NotificationManagerCompat.from(this).cancel(CallAlarmReceiver.NOTIFICATION_ID)
        }

        // Start ringtone
        val ringtoneUri = ringtoneUriString?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        runCatching {
            ringtonePlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@IncomingCallActivity, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        }.onFailure {
            // Fallback to default ringtone if custom URI failed
            runCatching {
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ringtonePlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(this@IncomingCallActivity, fallbackUri)
                    isLooping = true
                    prepare()
                    start()
                }
            }
        }

        // Start vibration
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 1)
            }
        }
    }

    private fun stopRingtoneAndVibration() {
        runCatching {
            ringtonePlayer?.stop()
            ringtonePlayer?.release()
            ringtonePlayer = null
        }
        runCatching {
            vibrator?.cancel()
        }
    }

    private fun answerCall() {
        stopRingtoneAndVibration()

        // Start voice playback if file exists
        val path = voicePath
        if (!path.isNullOrBlank()) {
            runCatching {
                val file = File(path)
                if (file.exists()) {
                    voicePlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        setDataSource(file.absolutePath)
                        isLooping = true
                        prepare()
                        start()
                    }
                }
            }
        }
    }

    private fun declineCall() {
        if (isCallFinished) return
        isCallFinished = true
        stopRingtoneAndVibration()
        recordHistory(CallOutcome.DECLINED, 0)
        finish()
    }

    fun onRingingTimeout() {
        if (isCallFinished) return
        isCallFinished = true
        stopRingtoneAndVibration()
        recordHistory(CallOutcome.MISSED, 0)
        finish()
    }

    private fun endCall(durationSec: Int) {
        if (isCallFinished) return
        isCallFinished = true
        stopVoicePlayer()
        recordHistory(CallOutcome.ANSWERED, durationSec)
        finish()
    }

    private fun toggleSpeaker(enable: Boolean) {
        runCatching {
            audioManager?.isSpeakerphoneOn = enable
        }
    }

    private fun stopVoicePlayer() {
        runCatching {
            voicePlayer?.stop()
            voicePlayer?.release()
            voicePlayer = null
        }
        runCatching {
            audioManager?.isSpeakerphoneOn = false
        }
    }

    private fun recordHistory(outcome: CallOutcome, durationSec: Int) {
        val app = FakeCallApplication.from(this)
        app.applicationScope.launch(Dispatchers.IO) {
            app.historyRepository.addHistory(
                CallHistoryItem(
                    callerName = callerName,
                    callerNumber = callerNumber,
                    scheduledFor = scheduledFor,
                    firedAt = System.currentTimeMillis(),
                    outcome = outcome,
                    durationSec = durationSec
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
        stopVoicePlayer()
        runCatching {
            NotificationManagerCompat.from(this).cancel(CallAlarmReceiver.NOTIFICATION_ID)
        }
    }

    companion object {
        const val ACTION_INCOMING_CALL = "com.afrouzi.fakecall.ACTION_INCOMING_CALL"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALLER_NAME = "extra_caller_name"
        const val EXTRA_CALLER_NUMBER = "extra_caller_number"
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        const val EXTRA_VOICE_PATH = "extra_voice_path"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
        const val EXTRA_SCHEDULED_FOR = "extra_scheduled_for"
    }
}

private enum class CallState {
    RINGING,
    ANSWERED
}

@Composable
private fun IncomingCallScreen(
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onEndCall: (Int) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
) {
    var callState by remember { mutableStateOf(CallState.RINGING) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // Intercept back button
    BackHandler {
        when (callState) {
            CallState.RINGING -> onDecline()
            CallState.ANSWERED -> onEndCall(callDurationSeconds)
        }
    }

    // Ringing 45-second timeout
    LaunchedEffect(callState) {
        if (callState == CallState.RINGING) {
            delay(45_000L)
            onDecline()
        }
    }

    // Call duration timer while answered
    LaunchedEffect(callState) {
        if (callState == CallState.ANSWERED) {
            while (isActive) {
                delay(1000L)
                callDurationSeconds++
            }
        }
    }

    // Gradient phone call background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 48.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header: Status and caller information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = when (callState) {
                        CallState.RINGING -> stringResource(R.string.incoming_call_title)
                        CallState.ANSWERED -> formatCallDuration(callDurationSeconds)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (callState) {
                        CallState.RINGING -> Color(0xFF94A3B8)
                        CallState.ANSWERED -> Color(0xFF38BDF8)
                    },
                    fontWeight = FontWeight.Medium,
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Avatar / Photo
                CallerAvatar(
                    name = callerName,
                    photoPath = photoPath,
                    isRinging = callState == CallState.RINGING,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callerName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )

                if (callerNumber.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = callerNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFCBD5E1),
                    )
                }
            }

            // Controls section based on CallState
            when (callState) {
                CallState.RINGING -> {
                    // Ringing actions: Decline (Red) and Answer (Green)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CallActionButton(
                            icon = Icons.Filled.CallEnd,
                            label = stringResource(R.string.incoming_call_decline),
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            size = 72,
                            onClick = onDecline,
                        )

                        CallActionButton(
                            icon = Icons.Filled.Call,
                            label = stringResource(R.string.incoming_call_answer),
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            size = 72,
                            onClick = {
                                callState = CallState.ANSWERED
                                onAnswer()
                            },
                        )
                    }
                }

                CallState.ANSWERED -> {
                    // Answered controls: Mute, Speaker, and End Call
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CallControlButton(
                                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                label = stringResource(R.string.call_mute),
                                isActive = isMuted,
                                onClick = { isMuted = !isMuted },
                            )

                            CallControlButton(
                                icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                                label = stringResource(R.string.call_speaker),
                                isActive = isSpeakerOn,
                                onClick = {
                                    val next = !isSpeakerOn
                                    isSpeakerOn = next
                                    onSpeakerToggle(next)
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        CallActionButton(
                            icon = Icons.Filled.CallEnd,
                            label = stringResource(R.string.call_ended),
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White,
                            size = 76,
                            onClick = { onEndCall(callDurationSeconds) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CallerAvatar(
    name: String,
    photoPath: String?,
    isRinging: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRinging) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarScale"
    )

    val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()

    Box(
        modifier = Modifier
            .size(130.dp)
            .scale(pulseScale),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = File(photoPath!!),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(3.dp, Color.White.copy(alpha = 0.2f), CircleShape),
            )
        } else {
            val initials = remember(name) {
                name.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .joinToString("")
                    .uppercase()
                    .ifEmpty { "?" }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7))
                    .border(3.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CallActionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    size: Int,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .clickable { onClick() },
            shape = CircleShape,
            color = backgroundColor,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size((size * 0.48).dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .clickable { onClick() },
            shape = CircleShape,
            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF1E293B),
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) Color(0xFF082F49) else Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d", m, s)
}
