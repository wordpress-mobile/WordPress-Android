package org.wordpress.android.ui.reader.tracker

import androidx.annotation.MainThread
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_A8C_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_CUSTOM_TAB_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_DISCOVER_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_FOLLOWING_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_LIKED_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_P2_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_LIST_SHOWN
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTab.A8C
import org.wordpress.android.ui.reader.tracker.ReaderTab.CUSTOM
import org.wordpress.android.ui.reader.tracker.ReaderTab.DISCOVER
import org.wordpress.android.ui.reader.tracker.ReaderTab.FOLLOWING
import org.wordpress.android.ui.reader.tracker.ReaderTab.LIKED
import org.wordpress.android.ui.reader.tracker.ReaderTab.P2
import org.wordpress.android.ui.reader.tracker.ReaderTab.SAVED
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.AppLog.T.MAIN
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
class ReaderTracker @Inject constructor(
    private val dateProvider: DateProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
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
            if (!isRunning(type)) {
                AppLog.d(MAIN, "ReaderTracker: started $type")
                trackers[type] = it.copy(startDate = dateProvider.getCurrentDate())
            }
        }
    }

    fun stop(type: ReaderTrackerType) {
        trackers[type]?.let { trackerInfo ->
            if (isRunning(type)) {
                AppLog.d(MAIN, "ReaderTracker: stopped $type")
                trackerInfo.startDate?.let { startDate ->
                    val accumulatedTime = trackerInfo.accumulatedTime +
                            DateTimeUtils.secondsBetween(dateProvider.getCurrentDate(), startDate)
                    // let reset the startDate to null
                    trackers[type] = ReaderTrackerInfo(accumulatedTime = accumulatedTime)
                } ?: AppLog.e(T.READER, "ReaderTracker > stop found a null startDate")
            }
        }
    }

    fun isRunning(type: ReaderTrackerType): Boolean {
        return trackers[type]?.startDate != null
    }

    fun getAnalyticsData(): Map<String, Any> {
        return trackers.entries.associate { it.key.propertyName to it.value.accumulatedTime }
    }

    fun trackReaderTabIfNecessary(readerTab: ReaderTab) {
        if (readerTab != appPrefsWrapper.getReaderActiveTab()) {
            when (readerTab) {
                FOLLOWING -> analyticsTrackerWrapper.track(READER_FOLLOWING_SHOWN)
                DISCOVER -> analyticsTrackerWrapper.track(READER_DISCOVER_SHOWN)
                LIKED -> analyticsTrackerWrapper.track(READER_LIKED_SHOWN)
                SAVED -> analyticsTrackerWrapper.track(READER_SAVED_LIST_SHOWN, mapOf("source" to "reader_filter"))
                A8C -> analyticsTrackerWrapper.track(READER_A8C_SHOWN)
                P2 -> analyticsTrackerWrapper.track(READER_P2_SHOWN)
                CUSTOM -> analyticsTrackerWrapper.track(READER_CUSTOM_TAB_SHOWN)
            }
            appPrefsWrapper.setReaderActiveTab(readerTab)
        }
    }

    fun onAppGoesToBackground() {
        appPrefsWrapper.setReaderActiveTab(null)
    }

    fun onBottomNavigationTabChanged() {
        appPrefsWrapper.setReaderActiveTab(null)
    }

    /* TRACK */

    fun track(stat: Stat) {
        analyticsTrackerWrapper.track(stat)
    }

    fun track(stat: Stat, properties: Map<String, *>) {
        analyticsTrackerWrapper.track(stat, properties)
    }

    /* TAG */

    fun trackTag(stat: Stat, tag: String) {
        val properties = mutableMapOf<String, Any>(
                TAG_KEY to tag
        )
        track(stat, properties)
    }

    /* OTHER */

    /**
     * Track when app launched via deep-linking but then fell back to external browser
     *
     * @param stat The Stat to bump
     * @param interceptedUri The fallback URI the app was started with
     */
    fun trackUri(stat: Stat, interceptedUri: String) {
        val properties = mutableMapOf<String, Any>(
                INTERCEPTED_URI to interceptedUri
        )
        track(stat, properties)
    }

    companion object {
        private const val TAG_KEY = "tag"
        private const val INTERCEPTED_URI = "intercepted_uri"
    }
}

enum class ReaderTab(val id: Int) {
    FOLLOWING(1), DISCOVER(2), LIKED(3), SAVED(4), CUSTOM(5), A8C(6), P2(7);

    companion object {
        fun fromId(id: Int): ReaderTab {
            return when (id) {
                FOLLOWING.id -> FOLLOWING
                DISCOVER.id -> DISCOVER
                LIKED.id -> LIKED
                SAVED.id -> SAVED
                A8C.id -> A8C
                P2.id -> P2
                CUSTOM.id -> CUSTOM
                else -> throw RuntimeException("Unexpected ReaderTab id")
            }
        }

        fun transformTagToTab(readerTag: ReaderTag): ReaderTab {
            return when {
                readerTag.isFollowedSites -> FOLLOWING
                readerTag.isPostsILike -> LIKED
                readerTag.isBookmarked -> SAVED
                readerTag.isDiscover -> DISCOVER
                readerTag.isA8C -> A8C
                readerTag.isP2 -> P2
                else -> CUSTOM
            }
        }
    }
}
