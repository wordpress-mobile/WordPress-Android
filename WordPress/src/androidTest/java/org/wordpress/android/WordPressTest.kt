package org.wordpress.android

import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import org.wordpress.android.modules.AppComponent
import org.wordpress.android.modules.DaggerAppComponentTest

class WordPressTest : WordPress() {
    override fun onCreate() {
        // Setup WorkManager debug logging level
        val config = Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        WorkManager.initialize(this, config)

        super.onCreate()
    }

    override fun initDaggerComponent() {
        mAppComponent = DaggerAppComponentTest.builder()
                .application(this)
                .build()
    }

    fun setAppComponent(appComponent: AppComponent) {
        mAppComponent = appComponent
    }
}
