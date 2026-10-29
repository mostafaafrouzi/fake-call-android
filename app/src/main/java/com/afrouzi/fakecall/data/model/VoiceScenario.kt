package com.afrouzi.fakecall.data.model

import androidx.annotation.StringRes
import com.afrouzi.fakecall.R

data class VoiceScenario(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
    val languageCode: String = "fa",
    val assetFileName: String? = null
)

object VoiceScenarios {
    val all: List<VoiceScenario> = listOf(
        VoiceScenario(
            id = "mom_fa",
            titleRes = R.string.scenario_mom_title,
            textRes = R.string.scenario_mom_text,
            languageCode = "fa",
            assetFileName = null
        ),
        VoiceScenario(
            id = "boss_fa",
            titleRes = R.string.scenario_boss_title,
            textRes = R.string.scenario_boss_text,
            languageCode = "fa",
            assetFileName = null
        ),
        VoiceScenario(
            id = "friend_fa",
            titleRes = R.string.scenario_friend_title,
            textRes = R.string.scenario_friend_text,
            languageCode = "fa",
            assetFileName = null
        ),
        VoiceScenario(
            id = "office_fa",
            titleRes = R.string.scenario_office_title,
            textRes = R.string.scenario_office_text,
            languageCode = "fa",
            assetFileName = null
        ),
        VoiceScenario(
            id = "meeting_en",
            titleRes = R.string.scenario_meeting_en_title,
            textRes = R.string.scenario_meeting_en_text,
            languageCode = "en",
            assetFileName = "voices/meeting_en.wav"
        ),
        VoiceScenario(
            id = "family_en",
            titleRes = R.string.scenario_family_en_title,
            textRes = R.string.scenario_family_en_text,
            languageCode = "en",
            assetFileName = "voices/urgent_en.wav"
        ),
    )
}
