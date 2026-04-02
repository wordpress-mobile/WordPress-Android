package org.wordpress.android.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerImpl

@InstallIn(SingletonComponent::class)
@Module
interface ReaderModule {
    @Binds
    fun bindReaderTracker(impl: ReaderTrackerImpl): ReaderTracker
}
