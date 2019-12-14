package org.wordpress.android.ui.reader

import androidx.annotation.MainThread
import org.wordpress.android.ui.reader.utils.ReaderTrackersProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
class ReaderTracker @Inject constructor(trackersProvider: ReaderTrackersProvider) {
    // TODO: evaluate during IA extensions to use something like Dispatchers.Main.Immediate in the fun(s)
    // to make the usage of this class more robust/transparent to its users
    private val trackers = trackersProvider.getTrackers()

    fun setupTrackers() {
        for (tracker in trackers) {
            tracker.init()
        }
    }

    fun start(type: Class<out ReaderTrackerInfo>) {
        val tracker = trackers.firstOrNull { it.javaClass == type }
        tracker?.start()
    }

    fun stop(type: Class<out ReaderTrackerInfo>) {
        val tracker = trackers.firstOrNull { it.javaClass == type }
        tracker?.stop()
    }

    fun getAnalyticsData(): Map<String, Any> {
        return trackers.associateBy({ it.key }, { it.accumulatedTime })
    }
}
