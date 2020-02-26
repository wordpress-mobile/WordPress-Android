package org.wordpress.android.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import org.mockito.Mockito
import org.wordpress.android.analytics.Tracker

@Module
class TrackerTestModule {
    @Provides
    fun provideTracker(appContext: Context): Tracker {
        return Mockito.mock(Tracker::class.java)
    }
}
