package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTrackerNosara
import org.wordpress.android.analytics.Tracker

@Module
class TrackerModule {
    @Provides
    fun provideTracker(appContext: Context): Tracker {
        val prefix = if (BuildConfig.IS_JETPACK_APP) "jpandroid" else "wpandroid"
        return AnalyticsTrackerNosara(appContext, prefix)
    }
}
