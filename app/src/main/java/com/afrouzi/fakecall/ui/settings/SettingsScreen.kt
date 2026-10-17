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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.prefs.AppLanguage
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.data.prefs.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())

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

        // Theme Mode Section
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

        // Language Section
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
