package com.afrouzi.fakecall.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.prefs.AppLanguage
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.data.prefs.CallTheme
import com.afrouzi.fakecall.data.prefs.ThemeMode
import com.afrouzi.fakecall.scheduling.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())

    var permissionStatus by remember {
        mutableStateOf(PermissionHelper.getEssentialPermissionsStatus(context))
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1500L)
            permissionStatus = PermissionHelper.getEssentialPermissionsStatus(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 80.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp, start = 4.dp),
        )

        // 1. Permissions Health Status Card
        PermissionsStatusCard(
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Call Screen Theme Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_call_theme_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ThemeOptionRow(
                    title = stringResource(R.string.settings_call_theme_one_ui),
                    selected = settings.callTheme == CallTheme.ONE_UI,
                    onClick = {
                        scope.launch { app.settingsRepository.setCallTheme(CallTheme.ONE_UI) }
                    }
                )

                ThemeOptionRow(
                    title = stringResource(R.string.settings_call_theme_hyper_os),
                    selected = settings.callTheme == CallTheme.HYPER_OS,
                    onClick = {
                        scope.launch { app.settingsRepository.setCallTheme(CallTheme.HYPER_OS) }
                    }
                )

                ThemeOptionRow(
                    title = stringResource(R.string.settings_call_theme_pixel),
                    selected = settings.callTheme == CallTheme.PIXEL,
                    onClick = {
                        scope.launch { app.settingsRepository.setCallTheme(CallTheme.PIXEL) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Theme Mode Section (Light / Dark)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_theme_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ThemeOptionRow(
                    title = stringResource(R.string.settings_theme_system),
                    selected = settings.themeMode == ThemeMode.SYSTEM,
                    onClick = {
                        scope.launch { app.settingsRepository.setThemeMode(ThemeMode.SYSTEM) }
                    }
                )

                ThemeOptionRow(
                    title = stringResource(R.string.settings_theme_light),
                    selected = settings.themeMode == ThemeMode.LIGHT,
                    onClick = {
                        scope.launch { app.settingsRepository.setThemeMode(ThemeMode.LIGHT) }
                    }
                )

                ThemeOptionRow(
                    title = stringResource(R.string.settings_theme_dark),
                    selected = settings.themeMode == ThemeMode.DARK,
                    onClick = {
                        scope.launch { app.settingsRepository.setThemeMode(ThemeMode.DARK) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Language Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_language_section),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LanguageOptionRow(
                    title = stringResource(R.string.settings_language_system),
                    selected = settings.language == AppLanguage.SYSTEM,
                    onClick = {
                        scope.launch {
                            app.settingsRepository.setLanguage(AppLanguage.SYSTEM)
                            app.applyLanguage(AppLanguage.SYSTEM)
                        }
                    }
                )

                LanguageOptionRow(
                    title = stringResource(R.string.settings_language_persian),
                    selected = settings.language == AppLanguage.PERSIAN,
                    onClick = {
                        scope.launch {
                            app.settingsRepository.setLanguage(AppLanguage.PERSIAN)
                            app.applyLanguage(AppLanguage.PERSIAN)
                        }
                    }
                )

                LanguageOptionRow(
                    title = stringResource(R.string.settings_language_english),
                    selected = settings.language == AppLanguage.ENGLISH,
                    onClick = {
                        scope.launch {
                            app.settingsRepository.setLanguage(AppLanguage.ENGLISH)
                            app.applyLanguage(AppLanguage.ENGLISH)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Adhoc Caller Save Prompt Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            app.settingsRepository.setAskSaveProfileOnAdhoc(!settings.askSaveProfileOnAdhoc)
                        }
                    }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BookmarkBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.settings_ask_save_adhoc),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Switch(
                    checked = settings.askSaveProfileOnAdhoc,
                    onCheckedChange = { checked ->
                        scope.launch {
                            app.settingsRepository.setAskSaveProfileOnAdhoc(checked)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionsStatusCard(
    status: PermissionHelper.EssentialPermissionsStatus,
    onOpenNotifications: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenFullScreen: () -> Unit,
) {
    val allActive = status.allGranted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (allActive) Color(0xFF22C55E).copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (allActive) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (allActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (allActive) {
                        stringResource(R.string.permissions_all_granted)
                    } else {
                        stringResource(R.string.permissions_status_section)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (allActive) {
                    stringResource(R.string.permissions_status_active_desc)
                } else {
                    stringResource(R.string.permissions_banner_desc)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!allActive) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!status.hasNotifications) {
                        OutlinedButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.permissions_grant_notifications), fontSize = 12.sp)
                        }
                    }
                    if (!status.hasExactAlarm) {
                        OutlinedButton(
                            onClick = onOpenExactAlarm,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.permissions_grant_exact_alarm), fontSize = 12.sp)
                        }
                    }
                    if (!status.hasFullScreenIntent) {
                        OutlinedButton(
                            onClick = onOpenFullScreen,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(stringResource(R.string.permissions_grant_full_screen), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )

        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
        )

        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}
