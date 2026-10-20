package com.shohankhan.hisabghor

import android.app.Application
import androidx.work.Configuration
import com.shohankhan.hisabghor.di.AppContainer

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
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
