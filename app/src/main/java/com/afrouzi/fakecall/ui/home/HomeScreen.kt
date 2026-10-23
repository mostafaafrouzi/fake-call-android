package com.afrouzi.fakecall.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.data.repository.ContactPickerHelper
import com.afrouzi.fakecall.scheduling.PermissionHelper
import com.afrouzi.fakecall.scheduling.SchedulingUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())
    val profiles by app.profileRepository.profiles.collectAsState(initial = emptyList())
    val nextCall by app.scheduleRepository.getNextUpcomingCall().collectAsState(initial = null)

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var permissionStatus by remember {
        mutableStateOf(PermissionHelper.getEssentialPermissionsStatus(context))
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1000L)
            now = System.currentTimeMillis()
            permissionStatus = PermissionHelper.getEssentialPermissionsStatus(context)
        }
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    var selectedProfileId by remember { mutableStateOf<Long?>(null) }
    var adhocName by remember { mutableStateOf("") }
    var adhocNumber by remember { mutableStateOf("") }
    var adhocPhotoPath by remember { mutableStateOf<String?>(null) }

    var selectedDelaySeconds by remember { mutableLongStateOf(10L) }
    var isCustomDelay by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("1") }
    var customSecondsText by remember { mutableStateOf("0") }

    var permissionDialog by remember { mutableStateOf<PermissionDialogType?>(null) }
    var showProfilePickerDialog by remember { mutableStateOf(false) }
    var showSaveAdhocDialog by remember { mutableStateOf(false) }
    var dontAskAgainChecked by remember { mutableStateOf(false) }

    val selectedProfile: CallerProfile? = profiles.find { it.id == selectedProfileId } ?: profiles.firstOrNull()

    // Contact Picker Launcher for Ad-hoc on Home Screen
    val homeContactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val phoneUri = result.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                val contact = ContactPickerHelper.extractContact(context, app.mediaManager, phoneUri)
                if (!contact.name.isNullOrBlank()) adhocName = contact.name
                if (!contact.number.isNullOrBlank()) adhocNumber = contact.number
                if (contact.photoPath != null) adhocPhotoPath = contact.photoPath
            }
        }
    }

    // Contact Permission Launcher for Ad-hoc on Home Screen
    val homeContactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        homeContactPickerLauncher.launch(intent)
    }

    // Helper function to trigger scheduling
    val scheduleCallAction: (saveAsProfile: Boolean) -> Unit = { saveAsProfile ->
        val delaySec = if (isCustomDelay) {
            val mins = customMinutesText.toIntOrNull() ?: 0
            val secs = customSecondsText.toIntOrNull() ?: 0
            SchedulingUtils.computeCustomDelaySeconds(mins, secs)
        } else {
            selectedDelaySeconds
        }

        val triggerTime = SchedulingUtils.computeTriggerTime(System.currentTimeMillis(), delaySec)

        scope.launch {
            var profileId: Long? = null
            var photoPath: String? = null
            var voicePath: String? = null
            var ringtoneUri: String? = null
            val name: String
            val number: String

            if (pagerState.currentPage == 1) {
                val prof = selectedProfile ?: profiles.firstOrNull()
                if (prof == null) {
                    Toast.makeText(context, context.getString(R.string.home_no_profiles_hint), Toast.LENGTH_SHORT).show()
                    return@launch
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
                photoPath = adhocPhotoPath
                if (saveAsProfile) {
                    val newProfile = CallerProfile(
                        name = name,
                        number = number,
                        photoPath = photoPath,
                        createdAt = System.currentTimeMillis()
                    )
                    profileId = app.profileRepository.saveProfile(newProfile)
                }
            }

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

            val id = app.scheduleRepository.scheduleCall(scheduledCall)
            app.callScheduler.schedule(scheduledCall.copy(id = id))
            Toast.makeText(
                context,
                context.getString(R.string.home_call_scheduled_toast),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_quick_schedule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Essential Permissions Banner (Auto-hides when all granted)
            EssentialPermissionsBanner(
                status = permissionStatus,
                onOpenNotifications = {
                    PermissionHelper.openNotificationSettings(context)
                },
                onOpenExactAlarm = {
                    PermissionHelper.openExactAlarmSettings(context)
                },
                onOpenFullScreen = {
                    PermissionHelper.openFullScreenIntentSettings(context)
                }
            )

            // Active Pending Call Card (if scheduled and in future)
            val activeCall = nextCall
            if (activeCall != null && activeCall.triggerAtMillis > now) {
                val remainingSec = maxOf(0L, (activeCall.triggerAtMillis - now) / 1000L)

                Card(
                    modifier = Modifier.fillMaxWidth(),
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

            // Card 1: Caller Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.home_caller_source),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        if (pagerState.currentPage == 0) {
                            OutlinedButton(
                                onClick = {
                                    if (PermissionHelper.canReadContacts(context)) {
                                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        homeContactPickerLauncher.launch(intent)
                                    } else {
                                        homeContactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.ContactPhone, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.profiles_pick_contact), fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Source Tabs: Ad-hoc vs Saved Profile with swipeable HorizontalPager
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Tab(
                            selected = pagerState.currentPage == 0,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.home_source_adhoc),
                                    fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = pagerState.currentPage == 1,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                                if (selectedProfileId == null && profiles.isNotEmpty()) {
                                    selectedProfileId = profiles.first().id
                                }
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.home_source_profile),
                                    fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        if (page == 0) {
                            // Ad-hoc input fields
                            Column(modifier = Modifier.fillMaxWidth()) {
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

                                if (!adhocPhotoPath.isNullOrBlank() && File(adhocPhotoPath!!).exists()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        AsyncImage(
                                            model = File(adhocPhotoPath!!),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.profiles_photo_section),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { adhocPhotoPath = null },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Saved profiles selector
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (profiles.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.home_no_profiles_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
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
                            }
                        }
                    }
                }
            }

            // Card 2: Delay & Timing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.home_delay_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Delay Presets with smooth horizontal scrolling to avoid text squishing in RTL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                label = { Text(label, fontSize = 12.sp, maxLines = 1) },
                                shape = RoundedCornerShape(12.dp),
                            )
                        }

                        FilterChip(
                            selected = isCustomDelay,
                            onClick = { isCustomDelay = true },
                            label = { Text(stringResource(R.string.home_delay_custom), fontSize = 12.sp, maxLines = 1) },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }

                    if (isCustomDelay) {
                        Spacer(modifier = Modifier.height(14.dp))
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Pinned Bottom Action Bar above navigation bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage == 0) {
                            val name = adhocName.trim()
                            val number = adhocNumber.trim()
                            if (!ProfileValidator.isValid(name, number)) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.error_fill_required_fields),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }
                        } else {
                            if (selectedProfile == null && profiles.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.home_no_profiles_hint),
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

                        // Check if adhoc save confirmation is needed
                        if (pagerState.currentPage == 0 && settings.askSaveProfileOnAdhoc) {
                            dontAskAgainChecked = false
                            showSaveAdhocDialog = true
                        } else {
                            scheduleCallAction(false)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 4.dp),
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.home_button_schedule),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    // Save Ad-hoc Profile Dialog
    if (showSaveAdhocDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAdhocDialog = false },
            title = { Text(stringResource(R.string.dialog_save_profile_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.dialog_save_profile_message),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dontAskAgainChecked = !dontAskAgainChecked }
                    ) {
                        Checkbox(
                            checked = dontAskAgainChecked,
                            onCheckedChange = { dontAskAgainChecked = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.dialog_dont_ask_again),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSaveAdhocDialog = false
                    if (dontAskAgainChecked) {
                        scope.launch { app.settingsRepository.setAskSaveProfileOnAdhoc(false) }
                    }
                    scheduleCallAction(true)
                }) {
                    Text(stringResource(R.string.dialog_save_and_start))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showSaveAdhocDialog = false
                    if (dontAskAgainChecked) {
                        scope.launch { app.settingsRepository.setAskSaveProfileOnAdhoc(false) }
                    }
                    scheduleCallAction(false)
                }) {
                    Text(stringResource(R.string.dialog_start_only))
                }
            }
        )
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
                        PermissionHelper.openNotificationSettings(context)
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
                        PermissionHelper.openExactAlarmSettings(context)
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
                        PermissionHelper.openFullScreenIntentSettings(context)
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

@Composable
private fun EssentialPermissionsBanner(
    status: PermissionHelper.EssentialPermissionsStatus,
    onOpenNotifications: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (status.allGranted) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.permissions_banner_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.permissions_banner_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!status.hasNotifications) {
                    Button(
                        onClick = onOpenNotifications,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.permissions_grant_notifications), fontSize = 12.sp)
                    }
                }

                if (!status.hasExactAlarm) {
                    Button(
                        onClick = onOpenExactAlarm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.permissions_grant_exact_alarm), fontSize = 12.sp)
                    }
                }

                if (!status.hasFullScreenIntent) {
                    Button(
                        onClick = onOpenFullScreen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.permissions_grant_full_screen), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
