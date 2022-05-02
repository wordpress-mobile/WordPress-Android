package org.wordpress.android.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito
import org.wordpress.android.analytics.Tracker

@TestInstallIn(components = [SingletonComponent::class], replaces = [TrackerModule::class])
@Module
class TrackerTestModule {
    @Provides
    fun provideTracker(): Tracker {
        return Mockito.mock(Tracker::class.java)
    }
}
