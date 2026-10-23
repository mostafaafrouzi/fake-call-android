package com.afrouzi.fakecall

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.afrouzi.fakecall.data.db.FakeCallDatabase
import com.afrouzi.fakecall.data.prefs.AppLanguage
import com.afrouzi.fakecall.data.prefs.SettingsRepository
import com.afrouzi.fakecall.data.repository.AudioRecorderHelper
import com.afrouzi.fakecall.data.repository.CallHistoryRepository
import com.afrouzi.fakecall.data.repository.CallerProfileRepository
import com.afrouzi.fakecall.data.repository.ProfileMediaManager
import com.afrouzi.fakecall.data.repository.ScheduledCallRepository
import com.afrouzi.fakecall.scheduling.CallScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FakeCallApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var database: FakeCallDatabase
        private set

    lateinit var profileRepository: CallerProfileRepository
        private set

    lateinit var scheduleRepository: ScheduledCallRepository
        private set

    lateinit var historyRepository: CallHistoryRepository
        private set

    lateinit var callScheduler: CallScheduler
        private set

    lateinit var mediaManager: ProfileMediaManager
        private set

    lateinit var audioRecorderHelper: AudioRecorderHelper
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        database = FakeCallDatabase.getInstance(this)

        mediaManager = ProfileMediaManager(this)
        audioRecorderHelper = AudioRecorderHelper(this)
        profileRepository = CallerProfileRepository(database.callerProfileDao(), mediaManager)
        scheduleRepository = ScheduledCallRepository(database.scheduledCallDao())
        historyRepository = CallHistoryRepository(database.callHistoryDao())
        callScheduler = CallScheduler(this)

        applicationScope.launch {
            val settings = settingsRepository.settings.first()
            applyLanguage(settings.language)
        }
    }

    fun applyLanguage(language: AppLanguage) {
        val localeList = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.PERSIAN -> LocaleListCompat.forLanguageTags("fa")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    companion object {
        fun from(context: Context): FakeCallApplication {
            return context.applicationContext as FakeCallApplication
        }
    }
}
