package org.wordpress.android.ui.reader.tracker

import androidx.annotation.MainThread
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
class ReaderTracker @Inject constructor(private val dateProvider: DateProvider) {
    // TODO: evaluate to use something like Dispatchers.Main.Immediate in the fun(s)
    // to sync the access to trackers; so to remove the @MainThread and make the
    // usage of this class more transparent to its users
    private val trackers = mutableMapOf<ReaderTrackerType, ReaderTrackerInfo>()

    init {
        for (trackerType in ReaderTrackerType.values()) {
            trackers[trackerType] = ReaderTrackerInfo()
        }
    }

    fun setupTrackers() {
        for (key in trackers.keys) {
            trackers[key] = ReaderTrackerInfo()
        }
    }

    fun start(type: ReaderTrackerType) {
        trackers[type]?.let {
            trackers[type] = it.copy(startDate = dateProvider.getCurrentDate())
        }
    }

    fun stop(type: ReaderTrackerType) {
        trackers[type]?.let { trackerInfo ->
            trackerInfo.startDate?.let { startDate ->
                val accumulatedTime = trackerInfo.accumulatedTime +
                        DateTimeUtils.secondsBetween(dateProvider.getCurrentDate(), startDate)
                // let reset the startDate to null
                trackers[type] = ReaderTrackerInfo(accumulatedTime = accumulatedTime)
            } ?: AppLog.e(T.READER, "ReaderTracker > stop found a null startDate")
        }
    }

    fun isRunning(type: ReaderTrackerType): Boolean {
        return trackers[type]?.startDate != null
    }

    fun getAnalyticsData(): Map<String, Any> {
        return trackers.entries.associate { it.key.propertyName to it.value.accumulatedTime }
    }
}
