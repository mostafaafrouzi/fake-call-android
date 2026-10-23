package com.afrouzi.fakecall.ui.profiles

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.afrouzi.fakecall.data.repository.ContactPickerHelper
import com.afrouzi.fakecall.scheduling.PermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen() {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()

    val profiles by app.profileRepository.profiles.collectAsState(initial = emptyList())

    var isEditSheetVisible by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<CallerProfile?>(null) }
    var profileToDelete by remember { mutableStateOf<CallerProfile?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
            if (profiles.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.profiles_empty_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.profiles_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.profiles_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                        )
                    }

                    items(profiles, key = { it.id }) { profile ->
                        ProfileItemCard(
                            profile = profile,
                            onEdit = {
                                editingProfile = profile
                                isEditSheetVisible = true
                            },
                            onDelete = {
                                profileToDelete = profile
                            }
                        )
                    }
                }
            }

            // Edit / Add ModalBottomSheet
            if (isEditSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { isEditSheetVisible = false },
                    sheetState = bottomSheetState,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    ProfileEditSheetContent(
                        initialProfile = editingProfile,
                        onSave = { updatedProfile ->
                            scope.launch {
                                app.profileRepository.saveProfile(updatedProfile)
                                isEditSheetVisible = false
                            }
                        },
                        onCancel = {
                            isEditSheetVisible = false
                        }
                    )
                }
            }

            // Delete Confirmation Dialog
            profileToDelete?.let { prof ->
                AlertDialog(
                    onDismissRequest = { profileToDelete = null },
                    title = { Text(stringResource(R.string.profiles_delete_confirm_title)) },
                    text = { Text(stringResource(R.string.profiles_delete_confirm_desc)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    app.profileRepository.deleteProfile(prof)
                                    profileToDelete = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.profiles_delete_action))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { profileToDelete = null }) {
                            Text(stringResource(R.string.dialog_action_cancel))
                        }
                    }
                )
            }

            // FAB anchored at bottom corner right above navigation bar
            FloatingActionButton(
                onClick = {
                    editingProfile = null
                    isEditSheetVisible = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier
                    .align(if (isRtl) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(20.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.profiles_add_title))
            }
        }
    }

@Composable
private fun ProfileItemCard(
    profile: CallerProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hasPhoto = !profile.photoPath.isNullOrBlank() && File(profile.photoPath).exists()
            if (hasPhoto) {
                AsyncImage(
                    model = File(profile.photoPath!!),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape),
                )
            } else {
                val initials = profile.name.trim().take(1).uppercase()
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = profile.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!profile.voicePath.isNullOrBlank() || !profile.ringtoneUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!profile.voicePath.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.Audiotrack, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.profiles_voice_section), fontSize = 10.sp)
                            }
                        }

                        if (!profile.ringtoneUri.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.profiles_ringtone_section), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.profiles_edit_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.profiles_delete_action),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ProfileEditSheetContent(
    initialProfile: CallerProfile?,
    onSave: (CallerProfile) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var number by remember { mutableStateOf(initialProfile?.number ?: "") }
    var photoPath by remember { mutableStateOf(initialProfile?.photoPath) }
    var voicePath by remember { mutableStateOf(initialProfile?.voicePath) }
    var ringtoneUriString by remember { mutableStateOf(initialProfile?.ringtoneUri) }

    // Recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            previewPlayer?.release()
            previewPlayer = null
            if (isRecording) {
                app.audioRecorderHelper.cancelRecording()
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isActive && isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    // Permission launcher for Recording
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val targetFile = app.mediaManager.createNewAudioRecordFile()
            if (app.audioRecorderHelper.startRecording(targetFile)) {
                isRecording = true
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.profiles_recording_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val phoneUri = result.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                val contact = ContactPickerHelper.extractContact(context, app.mediaManager, phoneUri)
                if (!contact.name.isNullOrBlank()) name = contact.name
                if (!contact.number.isNullOrBlank()) number = contact.number
                if (contact.photoPath != null) photoPath = contact.photoPath
            }
        }
    }

    // Contact Permission Launcher
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        contactPickerLauncher.launch(intent)
    }

    // Photo Picker Launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = app.profileRepository.copyPhotoUri(uri)
                if (savedPath != null) {
                    photoPath = savedPath
                }
            }
        }
    }

    // Voice Picker Launcher (File import)
    val voicePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = app.profileRepository.copyVoiceUri(uri)
                if (savedPath != null) {
                    voicePath = savedPath
                }
            }
        }
    }

    // Ringtone Picker Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            ringtoneUriString = pickedUri?.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (initialProfile == null) R.string.profiles_add_title else R.string.profiles_edit_title
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Contact Picker Button
            OutlinedButton(
                onClick = {
                    if (PermissionHelper.canReadContacts(context)) {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    } else {
                        contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(Icons.Filled.ContactPhone, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.profiles_pick_contact), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Photo preview and selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath!!).exists()
            if (hasPhoto) {
                AsyncImage(
                    model = File(photoPath!!),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        stringResource(
                            if (hasPhoto) R.string.profiles_change_photo else R.string.profiles_pick_photo
                        )
                    )
                }

                if (hasPhoto) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                app.profileRepository.deleteOldFile(photoPath)
                                photoPath = null
                            }
                        }
                    ) {
                        Text(
                            stringResource(R.string.profiles_remove_photo),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name and Number fields
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.profiles_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = number,
            onValueChange = { number = it },
            label = { Text(stringResource(R.string.profiles_number_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Voice Section
        Text(
            text = stringResource(R.string.profiles_voice_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(10.dp))

        val hasVoice = !voicePath.isNullOrBlank() && File(voicePath!!).exists()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Recording in progress banner
                if (isRecording) {
                    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .scale(pulseScale)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.profiles_recording_in_progress),
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Button(
                            onClick = {
                                val recordedFile = app.audioRecorderHelper.stopRecording()
                                isRecording = false
                                if (recordedFile != null && recordedFile.exists()) {
                                    voicePath = recordedFile.absolutePath
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.profiles_recording_stop))
                        }
                    }
                } else {
                    // Normal state: choose record or pick file
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Audiotrack,
                                contentDescription = null,
                                tint = if (hasVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    if (hasVoice) R.string.profiles_voice_attached else R.string.profiles_no_voice
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (hasVoice) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (hasVoice) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Preview play button
                                IconButton(
                                    onClick = {
                                        if (isPreviewPlaying) {
                                            previewPlayer?.stop()
                                            previewPlayer?.release()
                                            previewPlayer = null
                                            isPreviewPlaying = false
                                        } else {
                                            runCatching {
                                                previewPlayer?.release()
                                                previewPlayer = MediaPlayer().apply {
                                                    setDataSource(voicePath!!)
                                                    prepare()
                                                    setOnCompletionListener { isPreviewPlaying = false }
                                                    start()
                                                }
                                                isPreviewPlaying = true
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.profiles_recording_preview),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Delete button
                                IconButton(
                                    onClick = {
                                        previewPlayer?.stop()
                                        previewPlayer?.release()
                                        previewPlayer = null
                                        isPreviewPlaying = false
                                        scope.launch {
                                            app.profileRepository.deleteOldFile(voicePath)
                                            voicePath = null
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // In-app voice record button
                        OutlinedButton(
                            onClick = {
                                if (PermissionHelper.canRecordAudio(context)) {
                                    val targetFile = app.mediaManager.createNewAudioRecordFile()
                                    if (app.audioRecorderHelper.startRecording(targetFile)) {
                                        isRecording = true
                                    }
                                } else {
                                    recordPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.profiles_record_voice), fontSize = 12.sp)
                        }

                        // Pick file button
                        OutlinedButton(
                            onClick = { voicePickerLauncher.launch("audio/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(
                                stringResource(
                                    if (hasVoice) R.string.profiles_change_voice else R.string.profiles_pick_voice
                                ),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ringtone Section
        Text(
            text = stringResource(R.string.profiles_ringtone_section),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val hasCustomRingtone = !ringtoneUriString.isNullOrBlank()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = if (hasCustomRingtone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (hasCustomRingtone) R.string.profiles_custom_ringtone else R.string.profiles_default_ringtone
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (hasCustomRingtone) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtoneUriString))
                            }
                        }
                        ringtonePickerLauncher.launch(intent)
                    },
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(stringResource(R.string.profiles_pick_ringtone), fontSize = 12.sp)
                }

                if (hasCustomRingtone) {
                    IconButton(onClick = { ringtoneUriString = null }) {
                        Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Save & Cancel actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.dialog_action_cancel))
            }

            Button(
                onClick = {
                    val trimmedName = name.trim()
                    val trimmedNumber = number.trim()
                    if (!ProfileValidator.isValid(trimmedName, trimmedNumber)) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.error_fill_required_fields),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val profile = CallerProfile(
                        id = initialProfile?.id ?: 0L,
                        name = trimmedName,
                        number = trimmedNumber,
                        photoPath = photoPath,
                        voicePath = voicePath,
                        ringtoneUri = ringtoneUriString,
                        createdAt = initialProfile?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(profile)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.profiles_button_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}
