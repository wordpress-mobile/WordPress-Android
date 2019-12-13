package org.wordpress.android.ui.reader.utils

import org.wordpress.android.ui.reader.ReaderTrackerInfo
import javax.inject.Inject

/**
 * This simple class was created as a simple approach to make ReaderTracker testable
 */
class ReaderTrackersProvider @Inject constructor(private val dateProvider: DateProvider) {
    fun getTrackers() = listOf(
            ReaderTrackerInfo.ReaderTopLevelList(dateProvider),
            ReaderTrackerInfo.ReaderFilteredList(dateProvider),
            ReaderTrackerInfo.ReaderPagedPosts(dateProvider)
    )
}
