package com.afrouzi.fakecall.ui.home

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.CallerProfile
import com.afrouzi.fakecall.data.model.ProfileValidator
import com.afrouzi.fakecall.data.model.ScheduledCall
import com.afrouzi.fakecall.scheduling.PermissionHelper
import com.afrouzi.fakecall.scheduling.SchedulingUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

private enum class PermissionDialogType {
    NOTIFICATION,
    EXACT_ALARM,
    FULL_SCREEN_INTENT
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val profiles by app.profileRepository.profiles.collectAsState(initial = emptyList())
    val nextCall by app.scheduleRepository.getNextUpcomingCall().collectAsState(initial = null)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            now = System.currentTimeMillis()
        }
    }

    var useSavedProfile by remember { mutableStateOf(false) }
    var selectedProfileId by remember { mutableStateOf<Long?>(null) }
    var adhocName by remember { mutableStateOf("") }
    var adhocNumber by remember { mutableStateOf("") }

    var selectedDelaySeconds by remember { mutableLongStateOf(10L) }
    var isCustomDelay by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("1") }
    var customSecondsText by remember { mutableStateOf("0") }

    var permissionDialog by remember { mutableStateOf<PermissionDialogType?>(null) }
    var showProfilePickerDialog by remember { mutableStateOf(false) }

    val selectedProfile: CallerProfile? = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp),
    ) {
        // Active Pending Call Card (if scheduled and in future)
        val activeCall = nextCall
        if (activeCall != null && activeCall.triggerAtMillis > now) {
            val remainingSec = maxOf(0L, (activeCall.triggerAtMillis - now) / 1000L)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                ),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_pending_call_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${activeCall.callerName} (${activeCall.callerNumber})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.home_ringing_in, remainingSec),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                app.callScheduler.cancel(activeCall.id)
                                app.scheduleRepository.deleteScheduledCall(activeCall.id)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.home_call_cancelled_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.home_button_cancel))
                    }
                }
            }
        }

        // Quick Schedule Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.home_quick_schedule),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Source selector: Saved Profile vs Ad-hoc
                Text(
                    text = stringResource(R.string.home_caller_source),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !useSavedProfile,
                        onClick = { useSavedProfile = false },
                        label = { Text(stringResource(R.string.home_source_adhoc)) },
                        shape = RoundedCornerShape(12.dp),
                    )

                    FilterChip(
                        selected = useSavedProfile,
                        onClick = {
                            useSavedProfile = true
                            if (selectedProfileId == null && profiles.isNotEmpty()) {
                                selectedProfileId = profiles.first().id
                            }
                        },
                        label = { Text(stringResource(R.string.home_source_profile)) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (useSavedProfile) {
                    if (profiles.isEmpty()) {
                        Text(
                            text = stringResource(R.string.home_no_profiles_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val prof = selectedProfile ?: profiles.first()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showProfilePickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = CardDefaults.outlinedCardBorder(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val hasPhoto = !prof.photoPath.isNullOrBlank() && File(prof.photoPath).exists()
                                    if (hasPhoto) {
                                        AsyncImage(
                                            model = File(prof.photoPath!!),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = prof.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = prof.number,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                TextButton(onClick = { showProfilePickerDialog = true }) {
                                    Text(stringResource(R.string.home_select_profile), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = adhocName,
                        onValueChange = { adhocName = it },
                        label = { Text(stringResource(R.string.home_caller_name_label)) },
                        placeholder = { Text(stringResource(R.string.home_caller_name_placeholder)) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = adhocNumber,
                        onValueChange = { adhocNumber = it },
                        label = { Text(stringResource(R.string.home_caller_number_label)) },
                        placeholder = { Text(stringResource(R.string.home_caller_number_placeholder)) },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Delay Presets
                Text(
                    text = stringResource(R.string.home_delay_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        10L to stringResource(R.string.home_delay_10s),
                        30L to stringResource(R.string.home_delay_30s),
                        60L to stringResource(R.string.home_delay_1m),
                        300L to stringResource(R.string.home_delay_5m),
                    )

                    presets.forEach { (sec, label) ->
                        FilterChip(
                            selected = !isCustomDelay && selectedDelaySeconds == sec,
                            onClick = {
                                isCustomDelay = false
                                selectedDelaySeconds = sec
                            },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }

                    FilterChip(
                        selected = isCustomDelay,
                        onClick = { isCustomDelay = true },
                        label = { Text(stringResource(R.string.home_delay_custom), fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
                    )
                }

                if (isCustomDelay) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customMinutesText,
                            onValueChange = { customMinutesText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.home_custom_minutes)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )

                        OutlinedTextField(
                            value = customSecondsText,
                            onValueChange = { customSecondsText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(R.string.home_custom_seconds)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Schedule Button
                Button(
                    onClick = {
                        val name: String
                        val number: String
                        val photoPath: String?
                        val voicePath: String?
                        val ringtoneUri: String?
                        val profileId: Long?

                        if (useSavedProfile) {
                            val prof = selectedProfile
                            if (prof == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.home_no_profiles_hint),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                            name = prof.name
                            number = prof.number
                            photoPath = prof.photoPath
                            voicePath = prof.voicePath
                            ringtoneUri = prof.ringtoneUri
                            profileId = prof.id
                        } else {
                            name = adhocName.trim()
                            number = adhocNumber.trim()
                            photoPath = null
                            voicePath = null
                            ringtoneUri = null
                            profileId = null

                            if (!ProfileValidator.isValid(name, number)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_fill_required_fields),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                        }

                        // Permission checks with guidance dialogs
                        if (!PermissionHelper.canPostNotifications(context)) {
                            permissionDialog = PermissionDialogType.NOTIFICATION
                            return@Button
                        }

                        if (!PermissionHelper.canScheduleExactAlarms(context)) {
                            permissionDialog = PermissionDialogType.EXACT_ALARM
                            return@Button
                        }

                        if (!PermissionHelper.canUseFullScreenIntent(context)) {
                            permissionDialog = PermissionDialogType.FULL_SCREEN_INTENT
                            return@Button
                        }

                        // All valid, perform scheduling
                        val delaySec = if (isCustomDelay) {
                            val mins = customMinutesText.toIntOrNull() ?: 0
                            val secs = customSecondsText.toIntOrNull() ?: 0
                            SchedulingUtils.computeCustomDelaySeconds(mins, secs)
                        } else {
                            selectedDelaySeconds
                        }

                        val triggerTime = SchedulingUtils.computeTriggerTime(System.currentTimeMillis(), delaySec)

                        val scheduledCall = ScheduledCall(
                            profileId = profileId,
                            callerName = name,
                            callerNumber = number,
                            photoPath = photoPath,
                            voicePath = voicePath,
                            ringtoneUri = ringtoneUri,
                            triggerAtMillis = triggerTime,
                            createdAt = System.currentTimeMillis()
                        )

                        scope.launch {
                            val id = app.scheduleRepository.scheduleCall(scheduledCall)
                            app.callScheduler.schedule(scheduledCall.copy(id = id))
                            Toast.makeText(
                                context,
                                context.getString(R.string.home_call_scheduled_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.home_button_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    // Saved Profile Selection Dialog
    if (showProfilePickerDialog && profiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showProfilePickerDialog = false },
            title = { Text(stringResource(R.string.home_select_profile)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profiles.forEach { prof ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProfileId = prof.id
                                    showProfilePickerDialog = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (prof.id == selectedProfile?.id) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val hasPhoto = !prof.photoPath.isNullOrBlank() && File(prof.photoPath).exists()
                                if (hasPhoto) {
                                    AsyncImage(
                                        model = File(prof.photoPath!!),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape),
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = prof.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = prof.number,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProfilePickerDialog = false }) {
                    Text(stringResource(R.string.dialog_action_cancel))
                }
            }
        )
    }

    // Permission Guidance Dialogs
    when (permissionDialog) {
        PermissionDialogType.NOTIFICATION -> {
            AlertDialog(
                onDismissRequest = { permissionDialog = null },
                title = { Text(stringResource(R.string.permission_notification_title)) },
                text = { Text(stringResource(R.string.permission_notification_desc)) },
                confirmButton = {
                    Button(onClick = {
                        permissionDialog = null
                        runCatching {
                            context.startActivity(PermissionHelper.getNotificationSettingsIntent(context))
                        }
                    }) {
                        Text(stringResource(R.string.dialog_action_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { permissionDialog = null }) {
                        Text(stringResource(R.string.dialog_action_cancel))
                    }
                }
            )
        }
        PermissionDialogType.EXACT_ALARM -> {
            AlertDialog(
                onDismissRequest = { permissionDialog = null },
                title = { Text(stringResource(R.string.permission_exact_alarm_title)) },
                text = { Text(stringResource(R.string.permission_exact_alarm_desc)) },
                confirmButton = {
                    Button(onClick = {
                        permissionDialog = null
                        val intent = PermissionHelper.getExactAlarmSettingsIntent(context)
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                        }
                    }) {
                        Text(stringResource(R.string.dialog_action_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { permissionDialog = null }) {
                        Text(stringResource(R.string.dialog_action_cancel))
                    }
                }
            )
        }
        PermissionDialogType.FULL_SCREEN_INTENT -> {
            AlertDialog(
                onDismissRequest = { permissionDialog = null },
                title = { Text(stringResource(R.string.permission_fullscreen_title)) },
                text = { Text(stringResource(R.string.permission_fullscreen_desc)) },
                confirmButton = {
                    Button(onClick = {
                        permissionDialog = null
                        val intent = PermissionHelper.getFullScreenIntentSettingsIntent(context)
                        if (intent != null) {
                            runCatching { context.startActivity(intent) }
                        }
                    }) {
                        Text(stringResource(R.string.dialog_action_settings))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { permissionDialog = null }) {
                        Text(stringResource(R.string.dialog_action_cancel))
                    }
                }
            )
        }
        null -> {}
    }
}
