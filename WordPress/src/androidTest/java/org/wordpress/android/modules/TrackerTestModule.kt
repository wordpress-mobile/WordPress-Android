package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.mockito.Mockito
import org.wordpress.android.analytics.Tracker

@InstallIn(SingletonComponent::class)
@Module
class TrackerTestModule {
    @Provides
    fun provideTracker(): Tracker {
        return Mockito.mock(Tracker::class.java)
    }
}
