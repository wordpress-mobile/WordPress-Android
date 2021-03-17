package org.wordpress.android.ui.reader.tracker

import android.net.Uri
import androidx.annotation.MainThread
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_A8C_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_CUSTOM_TAB_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_DISCOVER_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_FOLLOWING_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_LIKED_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_P2_SHOWN
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SAVED_LIST_SHOWN
import org.wordpress.android.datasets.ReaderPostTable
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType.TAG_FOLLOWED
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
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
class ReaderTracker @Inject constructor(
    private val dateProvider: DateProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
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

    /* BLOG */

    fun trackBlog(stat: Stat, blogId: Long) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId
        )
        track(stat, properties)
    }

    fun trackBlog(stat: Stat, blogId: Long, isFollowed: Boolean?) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                FOLLOW_KEY to (isFollowed ?: UNKNOWN_VALUE)
        )
        track(stat, properties)
    }

    fun trackBlog(stat: Stat, blogId: Long, isFollowed: Boolean?, source: String) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                FOLLOW_KEY to (isFollowed ?: UNKNOWN_VALUE),
                SOURCE_KEY to source,
        )
        track(stat, properties)
    }

    /* TAG */

    fun trackTag(stat: Stat, tag: String) {
        val properties = mutableMapOf<String, Any>(
                TAG_KEY to tag
        )
        track(stat, properties)
    }

    /* POST */

    fun trackBlogPost(stat: Stat, blogId: Long, postId: Long) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                POST_ID_KEY to postId
        )
        track(stat, properties)
    }

    fun trackBlogPost(stat: Stat, blogId: String, postId: String, commentId: Int) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                POST_ID_KEY to postId,
                COMMENT_ID_KEY to commentId
        )
        track(stat, properties)
    }

    fun trackFeedPost(stat: Stat, feedId: Long, feedItemId: Long) {
        val properties = mutableMapOf<String, Any>(
                FEED_ID_KEY to feedId,
                FEED_ITEM_ID_KEY to feedItemId
        )
        track(stat, properties)
    }

    fun trackPost(stat: Stat, blogId: Long, postId: Long) {
        trackPost(stat, ReaderPostTable.getBlogPost(blogId, postId, true))
    }

    fun trackPost(stat: Stat, post: ReaderPost?) {
        trackPost(stat, post, mutableMapOf<String, Any>())
    }

    private fun trackPost(
        stat: Stat,
        post: ReaderPost?,
        properties: MutableMap<String, *>
    ) {
        analyticsUtilsWrapper.trackWithReaderPostDetails(
                stat,
                post,
                properties
        )
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

    fun trackDeepLink(stat: Stat, action: String, host: String, uri: Uri?) {
        analyticsUtilsWrapper.trackWithDeepLinkData(stat, action, host, uri)
    }

    /* HELPER */

    @JvmOverloads
    fun getSource(
        postListType: ReaderPostListType,
        readerTab: ReaderTab? = null
    ): String = if (postListType == TAG_FOLLOWED) {
        readerTab?.source ?: UNKNOWN_VALUE
    } else {
        postListType.source
    }

    companion object {
        private const val BLOG_ID_KEY = "blog_id"
        private const val POST_ID_KEY = "post_id"
        private const val COMMENT_ID_KEY = "comment_id"
        private const val FEED_ID_KEY = "feed_id"
        private const val FEED_ITEM_ID_KEY = "feed_item_id"
        private const val FOLLOW_KEY = "follow"
        private const val TAG_KEY = "tag"
        private const val INTERCEPTED_URI = "intercepted_uri"

        private const val SOURCE_KEY = "source"
        const val SOURCE_FOLLOWING = "following"
        const val SOURCE_DISCOVER = "discover"
        const val SOURCE_LIKED = "liked"
        const val SOURCE_SAVED = "saved"
        const val SOURCE_CUSTOM = "custom"
        const val SOURCE_A8C = "a8c"
        const val SOURCE_P2 = "p2"
        const val SOURCE_SETTINGS = "subscriptions"
        const val SOURCE_SEARCH = "search"
        const val SOURCE_SITE_PREVIEW = "site_preview"
        const val SOURCE_TAG_PREVIEW = "tag_preview"
        const val SOURCE_POST_DETAIL = "post_detail"
        const val SOURCE_COMMENT = "comment"
        const val SOURCE_USER = "user"
        const val SOURCE_STATS = "stats"
        const val SOURCE_NOTIFICATION = "notification"
        const val SOURCE_ACTIVITY_LOG_DETAIL = "activity_log_detail"

        private const val UNKNOWN_VALUE = "unknown"
    }
}

enum class ReaderTab(
    val id: Int,
    val source: String
) {
    FOLLOWING(1, ReaderTracker.SOURCE_FOLLOWING),
    DISCOVER(2, ReaderTracker.SOURCE_DISCOVER),
    LIKED(3, ReaderTracker.SOURCE_LIKED),
    SAVED(4, ReaderTracker.SOURCE_SAVED),
    CUSTOM(5, ReaderTracker.SOURCE_CUSTOM),
    A8C(6, ReaderTracker.SOURCE_A8C),
    P2(7, ReaderTracker.SOURCE_P2);

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

        @JvmStatic
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
