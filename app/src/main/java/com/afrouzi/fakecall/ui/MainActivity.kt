package com.afrouzi.fakecall.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.prefs.AppSettings
import com.afrouzi.fakecall.ui.about.AboutScreen
import com.afrouzi.fakecall.ui.history.HistoryScreen
import com.afrouzi.fakecall.ui.home.HomeScreen
import com.afrouzi.fakecall.ui.profiles.ProfilesScreen
import com.afrouzi.fakecall.ui.settings.SettingsScreen
import com.afrouzi.fakecall.ui.theme.FakeCallTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = FakeCallApplication.from(this)

        setContent {
            val settings by app.settingsRepository.settings.collectAsState(initial = AppSettings())

            FakeCallTheme(settings = settings) {
                FakeCallRoot(
                    onOpenUrl = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        runCatching { startActivity(intent) }
                    },
                )
            }
        }
    }
}

@Composable
private fun FakeCallRoot(onOpenUrl: (String) -> Unit) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Call, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_profiles)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.History, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_history)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_about)) },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> ProfilesScreen()
                2 -> HistoryScreen()
                3 -> SettingsScreen()
                else -> AboutScreen(onOpenUrl = onOpenUrl)
            }
        }
    }
}
