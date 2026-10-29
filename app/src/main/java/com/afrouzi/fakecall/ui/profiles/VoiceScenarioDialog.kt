package com.afrouzi.fakecall.ui.profiles

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.VoiceScenario
import com.afrouzi.fakecall.data.model.VoiceScenarios
import com.afrouzi.fakecall.data.repository.VoiceScenarioManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScenarioBottomSheet(
    onDismissRequest: () -> Unit,
    onVoiceSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val app = FakeCallApplication.from(context)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scenarioManager = remember { VoiceScenarioManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            scenarioManager.shutdown()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentlyPlayingId by remember { mutableStateOf<String?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var customTtsText by remember { mutableStateOf("") }
    var customLanguage by remember { mutableStateOf("fa") }

    ModalBottomSheet(
        onDismissRequest = {
            scenarioManager.stopPreview()
            onDismissRequest()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.voice_scenarios_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    scenarioManager.stopPreview()
                    onDismissRequest()
                }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {
                        scenarioManager.stopPreview()
                        currentlyPlayingId = null
                        selectedTabIndex = 0
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.voice_scenario_tab_presets),
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {
                        scenarioManager.stopPreview()
                        currentlyPlayingId = null
                        selectedTabIndex = 1
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.voice_scenario_tab_tts),
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // Preset Scenarios List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(VoiceScenarios.all) { scenario ->
                        val isPlaying = currentlyPlayingId == scenario.id
                        val scenarioText = stringResource(scenario.textRes)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = stringResource(scenario.titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = scenarioText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (isPlaying) {
                                                scenarioManager.stopPreview()
                                                currentlyPlayingId = null
                                            } else {
                                                scenarioManager.playPreview(scenarioText, scenario.languageCode)
                                                currentlyPlayingId = scenario.id
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(
                                                if (isPlaying) R.string.voice_scenario_stop else R.string.voice_scenario_preview
                                            ),
                                            fontSize = 12.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            isGenerating = true
                                            scenarioManager.stopPreview()
                                            scope.launch {
                                                val path = scenarioManager.applyScenario(
                                                    app.mediaManager,
                                                    scenario,
                                                    scenarioText
                                                )
                                                isGenerating = false
                                                if (path != null) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.voice_scenario_applied_toast),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onVoiceSelected(path)
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.voice_scenario_tts_error),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isGenerating
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(stringResource(R.string.voice_scenario_use), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Custom TTS Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    Text(
                        text = stringResource(R.string.voice_scenario_tts_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customTtsText,
                        onValueChange = { customTtsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text(stringResource(R.string.voice_scenario_tts_hint)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = customLanguage == "fa",
                            onClick = { customLanguage = "fa" },
                            label = { Text(stringResource(R.string.voice_lang_fa)) }
                        )
                        FilterChip(
                            selected = customLanguage == "en",
                            onClick = { customLanguage = "en" },
                            label = { Text(stringResource(R.string.voice_lang_en)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (customTtsText.isNotBlank()) {
                                    scenarioManager.playPreview(customTtsText, customLanguage)
                                }
                            },
                            enabled = customTtsText.isNotBlank() && !isGenerating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.voice_scenario_preview))
                        }

                        Button(
                            onClick = {
                                if (customTtsText.isNotBlank()) {
                                    isGenerating = true
                                    scenarioManager.stopPreview()
                                    scope.launch {
                                        val path = scenarioManager.synthesizeSpeechToFile(
                                            app.mediaManager,
                                            customTtsText,
                                            customLanguage
                                        )
                                        isGenerating = false
                                        if (path != null) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.voice_scenario_applied_toast),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            onVoiceSelected(path)
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.voice_scenario_tts_error),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            enabled = customTtsText.isNotBlank() && !isGenerating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.voice_scenario_tts_generate))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
