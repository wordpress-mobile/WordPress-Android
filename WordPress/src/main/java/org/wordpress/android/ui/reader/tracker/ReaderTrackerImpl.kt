package org.wordpress.android.ui.reader.tracker

import android.net.Uri
import androidx.annotation.MainThread
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@MainThread
@Suppress("ForbiddenComment", "TooManyFunctions")
class ReaderTrackerImpl @Inject constructor(
    private val dateProvider: DateProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val readingPreferencesTracker: ReaderReadingPreferencesTracker,
) : ReaderTracker {
    // TODO: evaluate to use something like Dispatchers.Main.Immediate
    // in the fun(s) to sync the access to trackers; so to remove the
    // @MainThread and make the usage of this class more transparent
    // to its users
    private val trackers =
        mutableMapOf<ReaderTrackerType, ReaderTrackerInfo>()

    init {
        for (trackerType in ReaderTrackerType.values()) {
            trackers[trackerType] = ReaderTrackerInfo()
        }
    }

    override fun setupTrackers() {
        for (key in trackers.keys) {
            trackers[key] = ReaderTrackerInfo()
        }
    }

    override fun start(type: ReaderTrackerType) {
        trackers[type]?.let {
            if (!isRunning(type)) {
                AppLog.d(
                    AppLog.T.MAIN,
                    "ReaderTracker: started $type"
                )
                trackers[type] =
                    it.copy(startDate = dateProvider.getCurrentDate())
            }
        }
    }

    override fun stop(type: ReaderTrackerType) {
        trackers[type]?.let { trackerInfo ->
            if (isRunning(type)) {
                AppLog.d(
                    AppLog.T.MAIN,
                    "ReaderTracker: stopped $type"
                )
                trackerInfo.startDate?.let { startDate ->
                    val accumulatedTime =
                        trackerInfo.accumulatedTime +
                            DateTimeUtils.secondsBetween(
                                dateProvider.getCurrentDate(),
                                startDate
                            )
                    trackers[type] =
                        ReaderTrackerInfo(
                            accumulatedTime = accumulatedTime
                        )
                } ?: AppLog.e(
                    AppLog.T.READER,
                    "ReaderTracker > stop found a null startDate"
                )
            }
        }
    }

    override fun isRunning(type: ReaderTrackerType): Boolean {
        return trackers[type]?.startDate != null
    }

    override fun getAnalyticsData(): Map<String, Any> {
        return trackers.entries.associate {
            it.key.propertyName to it.value.accumulatedTime
        }
    }

    override fun trackReaderTabIfNecessary(readerTab: ReaderTab) {
        if (readerTab != appPrefsWrapper.getReaderActiveTab()) {
            when (readerTab) {
                ReaderTab.FOLLOWING ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_FOLLOWING_SHOWN
                    )
                ReaderTab.DISCOVER ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_DISCOVER_SHOWN
                    )
                ReaderTab.LIKED ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_LIKED_SHOWN
                    )
                ReaderTab.SAVED ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_SAVED_LIST_SHOWN,
                        mapOf("source" to "reader_filter")
                    )
                ReaderTab.A8C ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_A8C_SHOWN
                    )
                ReaderTab.P2 ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_P2_SHOWN
                    )
                ReaderTab.CUSTOM ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_CUSTOM_TAB_SHOWN
                    )
                ReaderTab.TAGS_FEED ->
                    analyticsTrackerWrapper.track(
                        Stat.READER_TAGS_FEED_SHOWN
                    )
            }
            appPrefsWrapper.setReaderActiveTab(readerTab)
        }
    }

    override fun onAppGoesToBackground() {
        appPrefsWrapper.setReaderActiveTab(null)
    }

    override fun onBottomNavigationTabChanged() {
        appPrefsWrapper.setReaderActiveTab(null)
    }

    /* TRACK */

    override fun track(stat: Stat) {
        analyticsTrackerWrapper.track(stat)
    }

    override fun track(stat: Stat, source: String) {
        val properties = mutableMapOf<String, Any>(
            SOURCE_KEY to source
        )
        track(stat, properties)
    }

    override fun track(
        stat: Stat,
        properties: MutableMap<String, *>
    ) {
        analyticsTrackerWrapper.track(stat, properties)
    }

    /* BLOG */

    override fun trackBlog(
        stat: Stat,
        blogId: Long,
        feedId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
            BLOG_ID_KEY to blogId(blogId, feedId),
            FEED_ID_KEY to feedId
        )
        track(stat, properties)
    }

    override fun trackBlog(
        stat: Stat,
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

    override fun trackBlog(
        stat: Stat,
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

    override fun trackBlog(
        stat: Stat,
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
     * The [org.wordpress.android.models.ReaderBlog.fromJson] method
     * has a logic where it checks whether the blogs 'blogId' is 0,
     * if it is, then it checks whether the 'feedId' is not 0. If
     * both conditions are met then it assigns the 'feedId' to
     * 'blogId'.
     *
     * This tracking function captures that and does the opposite to
     * make sure that the tracking is done correctly.
     */
    private fun blogId(blogId: Long, feedId: Long) =
        if (feedId == blogId) 0 else blogId

    /* TAG */

    override fun trackTag(
        stat: Stat,
        tag: String,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
            TAG_KEY to tag,
            SOURCE_KEY to source
        )
        track(stat, properties)
    }

    override fun trackTagQuantity(stat: Stat, quantity: Int) {
        val properties = mutableMapOf<String, Any>(
            QUANTITY_KEY to quantity
        )
        track(stat, properties)
    }

    /* POST */

    override fun trackBlogPost(
        stat: Stat,
        blogId: Long,
        postId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
            BLOG_ID_KEY to blogId,
            POST_ID_KEY to postId
        )
        track(stat, properties)
    }

    override fun trackBlogPost(
        stat: Stat,
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

    override fun trackBlogPostAuthor(
        stat: Stat,
        blogId: Long,
        postId: Long,
        isJetpack: Boolean,
        userId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
            BLOG_ID_KEY to blogId,
            POST_ID_KEY to postId,
            IS_JETPACK_KEY to isJetpack,
            USER_ID_KEY to userId
        )
        track(stat, properties)
    }

    override fun trackBlogPost(
        stat: Stat,
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

    override fun trackFeedPost(
        stat: Stat,
        feedId: Long,
        feedItemId: Long
    ) {
        val properties = mutableMapOf<String, Any>(
            FEED_ID_KEY to feedId,
            FEED_ITEM_ID_KEY to feedItemId
        )
        track(stat, properties)
    }

    override fun trackPost(stat: Stat, post: ReaderPost?) {
        trackPost(stat, post, mutableMapOf<String, Any>())
    }

    fun trackPost(
        stat: Stat,
        post: ReaderPost?,
        readingPreferences: ReaderReadingPreferences,
    ) {
        trackPost(
            stat,
            post,
            readingPreferencesTracker.getPropertiesForPreferences(
                readingPreferences,
                READING_PREFERENCES_KEYS_PREFIX
            )
        )
    }

    override fun trackPost(
        stat: Stat,
        post: ReaderPost?,
        source: String
    ) {
        val properties = mutableMapOf<String, Any>(
            SOURCE_KEY to source
        )
        trackPost(stat, post, properties)
    }

    override fun trackPost(
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

    override fun trackPostComments(
        stat: Stat,
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

    override fun trackUri(stat: Stat, interceptedUri: String) {
        val properties = mutableMapOf<String, Any>(
            INTERCEPTED_URI_KEY to interceptedUri
        )
        track(stat, properties)
    }

    override fun trackQuery(stat: Stat, query: String) {
        val properties = mutableMapOf<String, Any>(
            QUERY_KEY to query
        )
        track(stat, properties)
    }

    override fun trackDeepLink(
        stat: Stat,
        action: String,
        host: String,
        uri: Uri?
    ) {
        analyticsUtilsWrapper.trackWithDeepLinkData(
            stat, action, host, uri
        )
    }

    override fun trackRailcar(railcarJson: String) {
        analyticsUtilsWrapper.trackRailcarRender(railcarJson)
    }

    override fun trackDropdownMenuOpened() {
        analyticsTrackerWrapper.track(
            Stat.READER_DROPDOWN_MENU_OPENED
        )
    }

    override fun trackDropdownMenuItemTapped(readerTag: ReaderTag) {
        when {
            readerTag.isDiscover -> "discover"
            readerTag.isFollowedSites -> "following"
            readerTag.isBookmarked -> "saved"
            readerTag.isPostsILike -> "liked"
            readerTag.isA8C -> "a8c"
            readerTag.isListTopic -> "list"
            readerTag.isP2 -> "p2"
            readerTag.isTags -> "tags"
            else -> null
        }?.let { trackingId ->
            analyticsTrackerWrapper.track(
                stat = Stat.READER_DROPDOWN_MENU_ITEM_TAPPED,
                properties = mapOf("id" to trackingId)
            )
        }
    }

    private fun trackFollowedCount(
        type: String,
        numberOfItems: Int
    ) {
        val props: MutableMap<String, String> = HashMap()
        props["type"] = type
        props["count"] = numberOfItems.toString()
        AnalyticsTracker.track(Stat.READER_FOLLOWING_FETCHED, props)
    }

    override fun trackFollowedTagsCount(numberOfItems: Int) {
        trackFollowedCount("tags", numberOfItems)
    }

    override fun trackSubscribedSitesCount(numberOfItems: Int) {
        trackFollowedCount("sites", numberOfItems)
    }

    /* HELPER */

    override fun getSource(
        postListType: ReaderPostListType,
        readerTab: ReaderTab?
    ): String =
        if (postListType == ReaderPostListType.TAG_FOLLOWED) {
            readerTab?.source ?: UNKNOWN_VALUE
        } else {
            postListType.source
        }

    override fun getSource(
        postListType: ReaderPostListType
    ): String = getSource(postListType, null)

    companion object {
        private const val BLOG_ID_KEY = "blog_id"
        private const val POST_ID_KEY = "post_id"
        private const val USER_ID_KEY = "user_id"
        private const val IS_JETPACK_KEY = "is_jetpack"
        private const val COMMENT_ID_KEY = "comment_id"
        private const val FEED_ID_KEY = "feed_id"
        private const val FEED_ITEM_ID_KEY = "feed_item_id"
        private const val FOLLOW_KEY = "follow"
        private const val TAG_KEY = "tag"
        private const val QUANTITY_KEY = "quantity"
        private const val INTERCEPTED_URI_KEY = "intercepted_uri"
        private const val QUERY_KEY = "query"
        private const val READING_PREFERENCES_KEYS_PREFIX =
            "reading_preferences"
        private const val SOURCE_KEY = "source"
        private const val UNKNOWN_VALUE = "unknown"
    }
}
