package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTrackerNosara
import org.wordpress.android.analytics.Tracker

@InstallIn(SingletonComponent::class)
@Module
class TrackerModule {
    @Provides
    fun provideTracker(@ApplicationContext appContext: Context): Tracker {
        return AnalyticsTrackerNosara(appContext, BuildConfig.TRACKS_EVENT_PREFIX)
    }
}
