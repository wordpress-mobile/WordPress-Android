package org.wordpress.android.ui.reader.tracker

import android.net.Uri
import androidx.annotation.MainThread
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
@Suppress("ForbiddenComment")
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
                AppLog.d(AppLog.T.MAIN, "ReaderTracker: started $type")
                trackers[type] = it.copy(startDate = dateProvider.getCurrentDate())
            }
        }
    }

    fun stop(type: ReaderTrackerType) {
        trackers[type]?.let { trackerInfo ->
            if (isRunning(type)) {
                AppLog.d(AppLog.T.MAIN, "ReaderTracker: stopped $type")
                trackerInfo.startDate?.let { startDate ->
                    val accumulatedTime = trackerInfo.accumulatedTime +
                            DateTimeUtils.secondsBetween(dateProvider.getCurrentDate(), startDate)
                    // let reset the startDate to null
                    trackers[type] = ReaderTrackerInfo(accumulatedTime = accumulatedTime)
                } ?: AppLog.e(AppLog.T.READER, "ReaderTracker > stop found a null startDate")
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
                ReaderTab.FOLLOWING -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_FOLLOWING_SHOWN)
                ReaderTab.DISCOVER -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_DISCOVER_SHOWN)
                ReaderTab.LIKED -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_LIKED_SHOWN)
                ReaderTab.SAVED -> analyticsTrackerWrapper.track(
                        AnalyticsTracker.Stat.READER_SAVED_LIST_SHOWN,
                        mapOf("source" to "reader_filter")
                )
                ReaderTab.A8C -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_A8C_SHOWN)
                ReaderTab.P2 -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_P2_SHOWN)
                ReaderTab.CUSTOM -> analyticsTrackerWrapper.track(AnalyticsTracker.Stat.READER_CUSTOM_TAB_SHOWN)
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

    fun track(
        stat: AnalyticsTracker.Stat
    ) {
        analyticsTrackerWrapper.track(stat)
    }

    fun track(
        stat: AnalyticsTracker.Stat,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
                SOURCE_KEY to source
        )
        track(stat, properties)
    }

    fun track(
        stat: AnalyticsTracker.Stat,
        properties: MutableMap<String, *>
    ) {
        analyticsTrackerWrapper.track(stat, properties)
    }

    /* BLOG */

    fun trackBlog(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        feedId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId(blogId, feedId),
                FEED_ID_KEY to feedId
        )
        track(stat, properties)
    }

    fun trackBlog(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        feedId: Long,
        isFollowed: Boolean?
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId(blogId, feedId),
                FEED_ID_KEY to feedId,
                FOLLOW_KEY to (isFollowed ?: UNKNOWN_VALUE)
        )
        track(stat, properties)
    }

    fun trackBlog(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        feedId: Long,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId(blogId, feedId),
                FEED_ID_KEY to feedId,
                SOURCE_KEY to source
        )
        track(stat, properties)
    }

    fun trackBlog(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        feedId: Long,
        isFollowed: Boolean?,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId(blogId, feedId),
                FEED_ID_KEY to feedId,
                FOLLOW_KEY to (isFollowed ?: UNKNOWN_VALUE),
                SOURCE_KEY to source
        )
        track(stat, properties)
    }

    /**
     * The [org.wordpress.android.models.ReaderBlog.fromJson] method within this model class has a logic where it
     * checks whether the blogs 'blogId' is 0, if it is, then it checks whether the 'feedId' is not 0. If both
     * conditions are met then it assigns the 'feedId' to 'blogId'. It does that in order to keep consistency with the
     * '/read/' endpoints.
     *
     * This tracking function captures that and does the opposite in order to make sure that the tracking is done to the
     * absolute. As such, it check whether the 'feedId' is equal to the 'blogId' and if so it assigns 0 to the 'blog_id'
     * tracking property. Else, as should, it assigns the actual 'blogId' to the 'blog_id' tracking property.
     */
    private fun blogId(blogId: Long, feedId: Long) = if (feedId == blogId) 0 else blogId

    /* TAG */

    fun trackTag(
        stat: AnalyticsTracker.Stat,
        tag: String,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
                TAG_KEY to tag,
                SOURCE_KEY to source
        )
        track(stat, properties)
    }

    fun trackTagQuantity(
        stat: AnalyticsTracker.Stat,
        quantity: Int
    ) {
        val properties = mutableMapOf<String, Any>(
                QUANTITY_KEY to quantity
        )
        track(stat, properties)
    }

    /* POST */

    fun trackBlogPost(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        postId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                POST_ID_KEY to postId
        )
        track(stat, properties)
    }

    fun trackBlogPost(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        postId: Long,
        isJetpack: Boolean
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                POST_ID_KEY to postId,
                IS_JETPACK_KEY to isJetpack
        )
        track(stat, properties)
    }

    fun trackBlogPost(
        stat: AnalyticsTracker.Stat,
        blogId: String,
        postId: String,
        commentId: Int
    ) {
        val properties = mutableMapOf<String, Any>(
                BLOG_ID_KEY to blogId,
                POST_ID_KEY to postId,
                COMMENT_ID_KEY to commentId
        )
        track(stat, properties)
    }

    fun trackFeedPost(
        stat: AnalyticsTracker.Stat,
        feedId: Long,
        feedItemId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
                FEED_ID_KEY to feedId,
                FEED_ITEM_ID_KEY to feedItemId
        )
        track(stat, properties)
    }

    fun trackPost(
        stat: AnalyticsTracker.Stat,
        post: ReaderPost?
    ) {
        trackPost(stat, post, mutableMapOf<String, Any>())
    }

    fun trackPost(
        stat: AnalyticsTracker.Stat,
        post: ReaderPost?,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
                SOURCE_KEY to source
        )
        trackPost(stat, post, properties)
    }

    private fun trackPost(
        stat: AnalyticsTracker.Stat,
        post: ReaderPost?,
        properties: MutableMap<String, *>
    ) {
        analyticsUtilsWrapper.trackWithReaderPostDetails(
                stat,
                post,
                properties
        )
    }

    fun trackPostComments(
        stat: AnalyticsTracker.Stat,
        blogId: Long,
        postId: Long,
        post: ReaderPost?,
        properties: MutableMap<String, *>
    ) {
        analyticsUtilsWrapper.trackFollowCommentsWithReaderPostDetails(
                stat,
                blogId,
                postId,
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
    fun trackUri(
        stat: AnalyticsTracker.Stat,
        interceptedUri: String
    ) {
        val properties = mutableMapOf<String, Any>(
                INTERCEPTED_URI_KEY to interceptedUri
        )
        track(stat, properties)
    }

    fun trackQuery(
        stat: AnalyticsTracker.Stat,
        query: String
    ) {
        val properties = mutableMapOf<String, Any>(
                QUERY_KEY to query
        )
        track(stat, properties)
    }

    fun trackDeepLink(
        stat: AnalyticsTracker.Stat,
        action: String,
        host: String,
        uri: Uri?
    ) {
        analyticsUtilsWrapper.trackWithDeepLinkData(stat, action, host, uri)
    }

    fun trackRailcar(
        railcarJson: String
    ) {
        analyticsUtilsWrapper.trackRailcarRender(railcarJson)
    }

    /* HELPER */

    @JvmOverloads
    fun getSource(
        postListType: ReaderPostListType,
        readerTab: ReaderTab? = null
    ): String = if (postListType == ReaderPostListType.TAG_FOLLOWED) {
        readerTab?.source ?: UNKNOWN_VALUE
    } else {
        postListType.source
    }

    companion object {
        private const val BLOG_ID_KEY = "blog_id"
        private const val POST_ID_KEY = "post_id"
        private const val IS_JETPACK_KEY = "is_jetpack"
        private const val COMMENT_ID_KEY = "comment_id"
        private const val FEED_ID_KEY = "feed_id"
        private const val FEED_ITEM_ID_KEY = "feed_item_id"
        private const val FOLLOW_KEY = "follow"
        private const val TAG_KEY = "tag"
        private const val QUANTITY_KEY = "quantity"
        private const val INTERCEPTED_URI_KEY = "intercepted_uri"
        private const val QUERY_KEY = "query"

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
        const val SOURCE_POST_DETAIL_COMMENT_SNIPPET = "post_detail_comment_snippet"
        const val SOURCE_COMMENT = "comment"
        const val SOURCE_USER = "user"
        const val SOURCE_STATS = "stats"
        const val SOURCE_NOTIFICATION = "notification"
        const val SOURCE_READER_LIKE_LIST = "reader_like_list"
        const val SOURCE_READER_LIKE_LIST_USER_PROFILE = "reader_like_list_user_profile"
        const val SOURCE_NOTIF_LIKE_LIST_USER_PROFILE = "notif_like_list_user_profile"
        const val SOURCE_USER_PROFILE_UNKNOWN = "user_profile_source_unknown"
        const val SOURCE_ACTIVITY_LOG_DETAIL = "activity_log_detail"

        const val SOURCE_POST_LIST_SAVED_POST_NOTICE = "post_list_saved_post_notice"

        private const val UNKNOWN_VALUE = "unknown"

        @JvmStatic
        fun trackTag(
            stat: AnalyticsTracker.Stat,
            tag: String
        ) {
            val properties = mutableMapOf<String, Any>(
                    TAG_KEY to tag
            )
            AnalyticsTracker.track(stat, properties)
        }

        fun isUserProfileSource(source: String): Boolean {
            return (source == SOURCE_READER_LIKE_LIST_USER_PROFILE ||
                    source == SOURCE_NOTIF_LIKE_LIST_USER_PROFILE ||
                    source == SOURCE_USER_PROFILE_UNKNOWN)
        }
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
