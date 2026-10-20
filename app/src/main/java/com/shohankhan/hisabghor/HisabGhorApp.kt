package com.shohankhan.hisabghor

import android.app.Application
import androidx.work.Configuration
import com.shohankhan.hisabghor.data.SeedData
import com.shohankhan.hisabghor.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Holds the manual dependency container and provides
 * an on-demand WorkManager configuration (no network, no analytics).
 */
class HisabGhorApp : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.appLockManager.start()
        container.appScope.launch {
            SeedData.seedIfEmpty(container.database)
            container.planningRepository.materializeDueRecurringRules()
        }
        ReminderScheduler.scheduleDaily(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
