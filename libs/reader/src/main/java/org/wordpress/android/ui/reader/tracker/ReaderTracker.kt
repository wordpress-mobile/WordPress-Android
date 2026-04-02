package org.wordpress.android.ui.reader.tracker

import android.net.Uri
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType

@Suppress("TooManyFunctions")
interface ReaderTracker {
    fun setupTrackers()
    fun start(type: ReaderTrackerType)
    fun stop(type: ReaderTrackerType)
    fun isRunning(type: ReaderTrackerType): Boolean
    fun getAnalyticsData(): Map<String, Any>

    fun trackReaderTabIfNecessary(readerTab: ReaderTab)
    fun onAppGoesToBackground()
    fun onBottomNavigationTabChanged()

    fun track(stat: Stat)
    fun track(stat: Stat, source: String)
    fun track(stat: Stat, properties: MutableMap<String, *>)

    fun trackBlog(stat: Stat, blogId: Long, feedId: Long)
    fun trackBlog(stat: Stat, blogId: Long, feedId: Long, isFollowed: Boolean?)
    fun trackBlog(stat: Stat, blogId: Long, feedId: Long, source: String)
    fun trackBlog(
        stat: Stat,
        blogId: Long,
        feedId: Long,
        isFollowed: Boolean?,
        source: String
    )

    fun trackTag(stat: Stat, tag: String, source: String)
    fun trackTagQuantity(stat: Stat, quantity: Int)

    fun trackBlogPost(stat: Stat, blogId: Long, postId: Long)
    fun trackBlogPost(stat: Stat, blogId: Long, postId: Long, isJetpack: Boolean)
    fun trackBlogPostAuthor(
        stat: Stat,
        blogId: Long,
        postId: Long,
        isJetpack: Boolean,
        userId: Long
    )
    fun trackBlogPost(stat: Stat, blogId: String, postId: String, commentId: Int)

    fun trackFeedPost(stat: Stat, feedId: Long, feedItemId: Long)

    fun trackPost(stat: Stat, post: ReaderPost?)
    fun trackPost(stat: Stat, post: ReaderPost?, source: String)
    fun trackPost(
        stat: Stat,
        post: ReaderPost?,
        properties: MutableMap<String, *>
    )

    fun trackPostComments(
        stat: Stat,
        blogId: Long,
        postId: Long,
        post: ReaderPost?,
        properties: MutableMap<String, *>
    )

    fun trackUri(stat: Stat, interceptedUri: String)
    fun trackQuery(stat: Stat, query: String)
    fun trackDeepLink(stat: Stat, action: String, host: String, uri: Uri?)
    fun trackRailcar(railcarJson: String)
    fun trackDropdownMenuOpened()
    fun trackDropdownMenuItemTapped(readerTag: ReaderTag)
    fun trackFollowedTagsCount(numberOfItems: Int)
    fun trackSubscribedSitesCount(numberOfItems: Int)

    fun getSource(
        postListType: ReaderPostListType,
        readerTab: ReaderTab?
    ): String

    fun getSource(postListType: ReaderPostListType): String =
        getSource(postListType, null)

    companion object {
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
        const val SOURCE_TAGS_FEED = "tags_feed"
        const val SOURCE_POST_DETAIL = "post_detail"
        const val SOURCE_POST_DETAIL_TOOLBAR = "post_detail_toolbar"
        const val SOURCE_POST_DETAIL_COMMENT_SNIPPET =
            "post_detail_comment_snippet"
        const val SOURCE_COMMENT = "comment"
        const val SOURCE_USER = "user"
        const val SOURCE_STATS = "stats"
        const val SOURCE_NOTIFICATION = "notification"
        const val SOURCE_READER_LIKE_LIST = "reader_like_list"
        const val SOURCE_READER_LIKE_LIST_USER_PROFILE =
            "reader_like_list_user_profile"
        const val SOURCE_NOTIF_LIKE_LIST_USER_PROFILE =
            "notif_like_list_user_profile"
        const val SOURCE_USER_PROFILE_UNKNOWN =
            "user_profile_source_unknown"
        const val SOURCE_ACTIVITY_LOG_DETAIL = "activity_log_detail"
        const val SOURCE_BLOGGING_PROMPTS_VIEW_ANSWERS =
            "blogging_prompts_my_site_card_view_answers"
        const val SOURCE_POST_LIST_SAVED_POST_NOTICE =
            "post_list_saved_post_notice"

        @JvmStatic
        fun trackTag(stat: Stat, tag: String) {
            val properties = mutableMapOf<String, Any>("tag" to tag)
            AnalyticsTracker.track(stat, properties)
        }

        fun isUserProfileSource(source: String): Boolean {
            return (source == SOURCE_READER_LIKE_LIST_USER_PROFILE ||
                source == SOURCE_NOTIF_LIKE_LIST_USER_PROFILE ||
                source == SOURCE_USER_PROFILE_UNKNOWN)
        }
    }
}
