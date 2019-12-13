package org.wordpress.android.ui.reader

import org.wordpress.android.ui.reader.utils.ReaderTrackersProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderTracker @Inject constructor(trackersProvider: ReaderTrackersProvider) {
    // we did not protect the concurrent access to the trackers
    // intended usage is to call the ReaderTracker fun from the UI thread
    // in events like onResume/onPause
    // TODO: evaluate during IA extensions to use something like Dispatchers.Main.Immediate in the fun(s)
    // to make the usage of this class more robust to its users
    private val trackers = trackersProvider.getTrackers()

    fun initTrackers() {
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
        return trackers.associateBy( {it.key}, { it.accumulatedTime })
    }
}
