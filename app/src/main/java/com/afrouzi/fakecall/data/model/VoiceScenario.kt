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
        // Persian Scenarios
        VoiceScenario(
            id = "mom_fa",
            titleRes = R.string.scenario_mom_title,
            textRes = R.string.scenario_mom_text,
            languageCode = "fa",
            assetFileName = "voices/mom_fa.wav"
        ),
        VoiceScenario(
            id = "boss_fa",
            titleRes = R.string.scenario_boss_title,
            textRes = R.string.scenario_boss_text,
            languageCode = "fa",
            assetFileName = "voices/boss_fa.wav"
        ),
        VoiceScenario(
            id = "friend_fa",
            titleRes = R.string.scenario_friend_title,
            textRes = R.string.scenario_friend_text,
            languageCode = "fa",
            assetFileName = "voices/friend_fa.wav"
        ),
        VoiceScenario(
            id = "office_fa",
            titleRes = R.string.scenario_office_title,
            textRes = R.string.scenario_office_text,
            languageCode = "fa",
            assetFileName = "voices/office_fa.wav"
        ),
        // English Scenarios
        VoiceScenario(
            id = "mom_en",
            titleRes = R.string.scenario_mom_en_title,
            textRes = R.string.scenario_mom_en_text,
            languageCode = "en",
            assetFileName = "voices/mom_en.wav"
        ),
        VoiceScenario(
            id = "boss_en",
            titleRes = R.string.scenario_boss_en_title,
            textRes = R.string.scenario_boss_en_text,
            languageCode = "en",
            assetFileName = "voices/boss_en.wav"
        ),
        VoiceScenario(
            id = "friend_en",
            titleRes = R.string.scenario_friend_en_title,
            textRes = R.string.scenario_friend_en_text,
            languageCode = "en",
            assetFileName = "voices/friend_en.wav"
        ),
        VoiceScenario(
            id = "office_en",
            titleRes = R.string.scenario_office_en_title,
            textRes = R.string.scenario_office_en_text,
            languageCode = "en",
            assetFileName = "voices/office_en.wav"
        ),
    )
}
