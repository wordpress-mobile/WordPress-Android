package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import org.wordpress.android.analytics.AnalyticsTrackerNosara
import org.wordpress.android.analytics.Tracker

@Module
class TrackerModule {
    @Provides
    fun provideTracker(appContext: Context): Tracker {
        return AnalyticsTrackerNosara(appContext)
    }
}
