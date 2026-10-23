package com.afrouzi.fakecall.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.CallerProfile
import com.afrouzi.fakecall.ui.theme.FakeCallTheme
import java.io.File

class FakeCallWidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = FakeCallApplication.from(this)

        setContent {
            val settings by app.settingsRepository.settings.collectAsState(initial = com.afrouzi.fakecall.data.prefs.AppSettings())
            val profiles by app.profileRepository.profiles.collectAsState(initial = emptyList())

            FakeCallTheme(settings = settings) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigureScreen(
                        profiles = profiles,
                        onConfirm = { selectedProfile, delaySec ->
                            saveWidgetConfiguration(selectedProfile, delaySec)
                        }
                    )
                }
            }
        }
    }

    private fun saveWidgetConfiguration(selectedProfile: CallerProfile?, delaySec: Long) {
        val prefs = getSharedPreferences(FakeCallWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE).edit()

        val name = selectedProfile?.name ?: getString(R.string.home_default_caller_name)
        val number = selectedProfile?.number ?: getString(R.string.home_default_caller_number)
        val photoPath = selectedProfile?.photoPath
        val voicePath = selectedProfile?.voicePath
        val ringtoneUri = selectedProfile?.ringtoneUri
        val profileId = selectedProfile?.id

        prefs.putString(FakeCallWidgetProvider.KEY_NAME_PREFIX + appWidgetId, name)
        prefs.putString(FakeCallWidgetProvider.KEY_NUMBER_PREFIX + appWidgetId, number)
        prefs.putString(FakeCallWidgetProvider.KEY_PHOTO_PREFIX + appWidgetId, photoPath)
        prefs.putString(FakeCallWidgetProvider.KEY_VOICE_PREFIX + appWidgetId, voicePath)
        prefs.putString(FakeCallWidgetProvider.KEY_RINGTONE_PREFIX + appWidgetId, ringtoneUri)
        prefs.putLong(FakeCallWidgetProvider.KEY_DELAY_PREFIX + appWidgetId, delaySec)
        if (profileId != null) {
            prefs.putLong(FakeCallWidgetProvider.KEY_PROFILE_ID_PREFIX + appWidgetId, profileId)
        } else {
            prefs.remove(FakeCallWidgetProvider.KEY_PROFILE_ID_PREFIX + appWidgetId)
        }
        prefs.apply()

        val appWidgetManager = AppWidgetManager.getInstance(this)
        FakeCallWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}

@Composable
private fun WidgetConfigureScreen(
    profiles: List<CallerProfile>,
    onConfirm: (CallerProfile?, Long) -> Unit,
) {
    var selectedProfile by remember { mutableStateOf<CallerProfile?>(profiles.firstOrNull()) }
    var selectedDelaySec by remember { mutableLongStateOf(10L) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.widget_configure_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.widget_select_profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Default quick caller option
                ProfileSelectOption(
                    title = stringResource(R.string.home_default_caller_name),
                    subtitle = stringResource(R.string.home_default_caller_number),
                    photoPath = null,
                    selected = selectedProfile == null,
                    onClick = { selectedProfile = null }
                )

                profiles.forEach { prof ->
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfileSelectOption(
                        title = prof.name,
                        subtitle = prof.number,
                        photoPath = prof.photoPath,
                        selected = selectedProfile?.id == prof.id,
                        onClick = { selectedProfile = prof }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Delay Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.widget_select_delay),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val presets = listOf(
                    10L to stringResource(R.string.home_delay_10s),
                    30L to stringResource(R.string.home_delay_30s),
                    60L to stringResource(R.string.home_delay_1m),
                    300L to stringResource(R.string.home_delay_5m),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (sec, label) ->
                        FilterChip(
                            selected = selectedDelaySec == sec,
                            onClick = { selectedDelaySec = sec },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onConfirm(selectedProfile, selectedDelaySec) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_save_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProfileSelectOption(
    title: String,
    subtitle: String,
    photoPath: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (selected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
        ) else null
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
                val hasPhoto = !photoPath.isNullOrBlank() && File(photoPath).exists()
                if (hasPhoto) {
                    AsyncImage(
                        model = File(photoPath!!),
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
