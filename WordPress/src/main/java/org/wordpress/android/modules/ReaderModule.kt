package org.wordpress.android.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerImpl
import org.wordpress.android.util.NetworkAvailability
import org.wordpress.android.util.NetworkUtilsWrapper

@InstallIn(SingletonComponent::class)
@Module
interface ReaderModule {
    @Binds
    fun bindReaderTracker(impl: ReaderTrackerImpl): ReaderTracker

    @Binds
    fun bindNetworkAvailability(
        impl: NetworkUtilsWrapper
    ): NetworkAvailability
}
