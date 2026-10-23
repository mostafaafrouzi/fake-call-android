package com.afrouzi.fakecall.call

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import coil.compose.AsyncImage
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.call.theme.PixelIncomingCallUi
import com.afrouzi.fakecall.call.theme.SamsungIncomingCallUi
import com.afrouzi.fakecall.call.theme.XiaomiIncomingCallUi
import com.afrouzi.fakecall.data.model.CallHistoryItem
import com.afrouzi.fakecall.data.model.CallOutcome
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.data.prefs.CallTheme
import com.afrouzi.fakecall.scheduling.CallAlarmReceiver
import com.afrouzi.fakecall.ui.MainActivity
import com.afrouzi.fakecall.ui.theme.FakeCallTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class IncomingCallActivity : AppCompatActivity() {

    private var ringtonePlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioManager: AudioManager? = null

    private var proximityWakeLock: PowerManager.WakeLock? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var proximityListener: SensorEventListener? = null
    private var isNearEarState = mutableStateOf(false)

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
            val isNearEar by isNearEarState

            FakeCallTheme(settings = settings) {
                Box(modifier = Modifier.fillMaxSize()) {
                    IncomingCallScreen(
                        settings = settings,
                        callerName = callerName,
                        callerNumber = callerNumber,
                        photoPath = photoPath,
                        onAnswer = { answerCall() },
                        onDecline = { declineCall() },
                        onTimeout = { onRingingTimeout() },
                        onEndCall = { duration -> endCall(duration) },
                        onSpeakerToggle = { on -> toggleSpeaker(on) },
                    )

                    // Blackout overlay when phone is held to ear
                    if (isNearEar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    }
                }
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
        runCatching {
            NotificationManagerCompat.from(this).cancel(CallAlarmReceiver.NOTIFICATION_ID)
        }

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

        // 1. Audio Routing: Set mode to IN_CALL and route to Earpiece by default
        runCatching {
            audioManager?.mode = AudioManager.MODE_IN_CALL
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
        }

        // 2. Proximity sensor: Turn screen off when held to ear
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
                proximityWakeLock = pm.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "fakecall:proximity"
                ).apply {
                    acquire(60 * 60 * 1000L)
                }
            }
        }

        // Software proximity fallback listener
        runCatching {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            if (proximitySensor != null) {
                proximityListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val distance = event?.values?.getOrNull(0) ?: return
                        val max = proximitySensor?.maximumRange ?: 5f
                        isNearEarState.value = (distance < max && distance < 4f)
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager?.registerListener(proximityListener, proximitySensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        // 3. Play voice recording
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
        showMissedCallNotification()
        finish()
    }

    fun onRingingTimeout() {
        if (isCallFinished) return
        isCallFinished = true
        stopRingtoneAndVibration()
        recordHistory(CallOutcome.MISSED, 0)
        showMissedCallNotification()
        finish()
    }

    private fun endCall(durationSec: Int) {
        if (isCallFinished) return
        isCallFinished = true
        cleanupCallHardware()
        recordHistory(CallOutcome.ANSWERED, durationSec)
        finish()
    }

    private fun toggleSpeaker(enable: Boolean) {
        runCatching {
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = enable
        }
    }

    private fun cleanupCallHardware() {
        runCatching {
            voicePlayer?.stop()
            voicePlayer?.release()
            voicePlayer = null
        }
        runCatching {
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        }
        runCatching {
            proximityWakeLock?.let {
                if (it.isHeld) it.release()
            }
            proximityWakeLock = null
        }
        runCatching {
            proximityListener?.let { sensorManager?.unregisterListener(it) }
            proximityListener = null
        }
        isNearEarState.value = false
    }

    private fun showMissedCallNotification() {
        val channelId = "missed_calls"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_missed_call_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_missed_call_channel_desc)
            }
            nm?.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "history")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_missed_call)
            .setContentTitle(getString(R.string.notification_missed_call_title))
            .setContentText(getString(R.string.notification_missed_call_text, callerName))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
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
        if (!isCallFinished) {
            isCallFinished = true
            recordHistory(CallOutcome.MISSED, 0)
            showMissedCallNotification()
        }
        stopRingtoneAndVibration()
        cleanupCallHardware()
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
    settings: AppSettings,
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onTimeout: () -> Unit,
    onEndCall: (Int) -> Unit,
    onSpeakerToggle: (Boolean) -> Unit,
) {
    var callState by remember { mutableStateOf(CallState.RINGING) }
    var callDurationSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var showDialpad by remember { mutableStateOf(false) }

    BackHandler {
        when (callState) {
            CallState.RINGING -> onDecline()
            CallState.ANSWERED -> onEndCall(callDurationSeconds)
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.RINGING) {
            delay(45_000L)
            onTimeout()
        }
    }

    LaunchedEffect(callState) {
        if (callState == CallState.ANSWERED) {
            while (isActive) {
                delay(1000L)
                callDurationSeconds++
            }
        }
    }

    if (showDialpad) {
        DialpadDialog(onDismiss = { showDialpad = false })
    }

    when (callState) {
        CallState.RINGING -> {
            when (settings.callTheme) {
                CallTheme.ONE_UI -> {
                    SamsungIncomingCallUi(
                        callerName = callerName,
                        callerNumber = callerNumber,
                        photoPath = photoPath,
                        onAnswer = {
                            callState = CallState.ANSWERED
                            onAnswer()
                        },
                        onDecline = onDecline
                    )
                }
                CallTheme.HYPER_OS -> {
                    XiaomiIncomingCallUi(
                        callerName = callerName,
                        callerNumber = callerNumber,
                        photoPath = photoPath,
                        onAnswer = {
                            callState = CallState.ANSWERED
                            onAnswer()
                        },
                        onDecline = onDecline
                    )
                }
                CallTheme.PIXEL -> {
                    PixelIncomingCallUi(
                        callerName = callerName,
                        callerNumber = callerNumber,
                        photoPath = photoPath,
                        onAnswer = {
                            callState = CallState.ANSWERED
                            onAnswer()
                        },
                        onDecline = onDecline
                    )
                }
            }
        }

        CallState.ANSWERED -> {
            ActiveCallUi(
                callerName = callerName,
                callerNumber = callerNumber,
                photoPath = photoPath,
                durationSeconds = callDurationSeconds,
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                onMuteToggle = { isMuted = !isMuted },
                onKeypadClick = { showDialpad = true },
                onSpeakerToggle = {
                    val next = !isSpeakerOn
                    isSpeakerOn = next
                    onSpeakerToggle(next)
                },
                onEndCall = { onEndCall(callDurationSeconds) }
            )
        }
    }
}

@Composable
private fun ActiveCallUi(
    callerName: String,
    callerNumber: String,
    photoPath: String?,
    durationSeconds: Int,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onMuteToggle: () -> Unit,
    onKeypadClick: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onEndCall: () -> Unit,
) {
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
            // Header Info & Duration
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = formatCallDuration(durationSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(32.dp))

                CallerAvatar(
                    name = callerName,
                    photoPath = photoPath,
                    isRinging = false,
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

            // Interactive Controls Row & End Call
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CallControlButton(
                        icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        label = stringResource(R.string.call_mute),
                        isActive = isMuted,
                        onClick = onMuteToggle,
                    )

                    CallControlButton(
                        icon = Icons.Filled.Dialpad,
                        label = stringResource(R.string.call_keypad),
                        isActive = false,
                        onClick = onKeypadClick,
                    )

                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                        label = stringResource(R.string.call_speaker),
                        isActive = isSpeakerOn,
                        onClick = onSpeakerToggle,
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // End Call Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .clickable(onClick = onEndCall),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = stringResource(R.string.call_ended),
                        tint = Color.White,
                        modifier = Modifier.size(36.dp),
                    )
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
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()

    Box(
        modifier = Modifier
            .size(130.dp)
            .scale(if (isRinging) pulseScale else 1f),
        contentAlignment = Alignment.Center,
    ) {
        if (hasPhoto) {
            AsyncImage(
                model = File(photoPath!!),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.Center,
            ) {
                val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
                Text(
                    text = initial,
                    color = Color.White,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.15f),
            contentColor = if (isActive) Color(0xFF0F172A) else Color.White,
            modifier = Modifier.size(60.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatCallDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
