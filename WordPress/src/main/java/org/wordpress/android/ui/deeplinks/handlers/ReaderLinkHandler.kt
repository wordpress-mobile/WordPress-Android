package org.wordpress.android.ui.deeplinks.handlers

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_VIEWPOST_INTERCEPTED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenFeedInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReaderDiscover
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReaderSearch
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenTagInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ViewPostInReader
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.APPLINK_SCHEME
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.SITE_DOMAIN
import org.wordpress.android.ui.reader.ReaderConstants
import org.wordpress.android.ui.utils.IntentUtils
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class ReaderLinkHandler
@Inject constructor(
    private val intentUtils: IntentUtils,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper
) : DeepLinkHandler {
    private val _toast = MutableLiveData<Event<Int>>()
    val toast = _toast as LiveData<Event<Int>>

    /**
     * URIs supported by the Reader are already defined as intent filters in the manifest. Instead of replicating
     * that logic here, we simply check if we can resolve an [Intent] that uses [ReaderConstants.ACTION_VIEW_POST].
     * Since that's a custom action that is only handled by the Reader, we can then assume it supports this URI.
     * Other deeplinks handled:
     * `wordpress://read`
     * `wordpress://viewpost?blogId={blogId}&postId={postId}`
     * `wordpress.com/read`
     * `wordpress.com/discover`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return DEEP_LINK_HOST_READ == uri.host ||
            DEEP_LINK_HOST_VIEWPOST == uri.host ||
            isWordPressComReaderUrl(uri) ||
            isWordPressComDiscoverUrl(uri) ||
            isWordPressComFeedUrl(uri) ||
            isWordPressComReaderSearchUrl(uri) ||
            isWordPressComTagUrl(uri) ||
            intentUtils.canResolveWith(ReaderConstants.ACTION_VIEW_POST, uri)
    }

    private fun isWordPressComReaderUrl(uri: UriWrapper): Boolean {
        return uri.host == HOST_WORDPRESS_COM &&
            uri.pathSegments.size == 1 &&
            uri.pathSegments.firstOrNull() == PATH_READ
    }

    private fun isWordPressComDiscoverUrl(uri: UriWrapper): Boolean {
        return uri.host == HOST_WORDPRESS_COM &&
            uri.pathSegments.size == 1 &&
            uri.pathSegments.firstOrNull() == PATH_DISCOVER
    }

    /**
     * Checks if this is a feed URL like wordpress.com/read/feeds/{feedId} or wordpress.com/reader/feeds/{feedId}
     * but NOT a post URL like wordpress.com/read/feeds/{feedId}/posts/{postId}
     */
    private fun isWordPressComFeedUrl(uri: UriWrapper): Boolean {
        val segments = uri.pathSegments
        return uri.host == HOST_WORDPRESS_COM &&
            segments.size == FEED_URL_SEGMENTS &&
            isReadOrReaderPath(segments.firstOrNull()) &&
            segments.getOrNull(SECOND_PATH_POSITION) == PATH_FEEDS
    }

    /**
     * Checks if this is a reader search URL like wordpress.com/read/search or wordpress.com/reader/search
     */
    private fun isWordPressComReaderSearchUrl(uri: UriWrapper): Boolean {
        val segments = uri.pathSegments
        return uri.host == HOST_WORDPRESS_COM &&
            segments.size == SEARCH_URL_SEGMENTS &&
            isReadOrReaderPath(segments.firstOrNull()) &&
            segments.getOrNull(SECOND_PATH_POSITION) == PATH_SEARCH
    }

    private fun isReadOrReaderPath(segment: String?) = segment == PATH_READ || segment == PATH_READER

    /**
     * Checks if this is a tag URL like wordpress.com/tag/{tagSlug}
     */
    private fun isWordPressComTagUrl(uri: UriWrapper): Boolean {
        val segments = uri.pathSegments
        return uri.host == HOST_WORDPRESS_COM &&
            segments.size == TAG_URL_SEGMENTS &&
            segments.firstOrNull() == PATH_TAG
    }

    private fun extractFeedId(uri: UriWrapper): Long? {
        return uri.pathSegments.getOrNull(FEED_ID_POSITION)?.toLongOrNull()
    }

    private fun extractTagSlug(uri: UriWrapper): String? {
        return uri.pathSegments.getOrNull(TAG_SLUG_POSITION)
    }

    /**
     * Builds navigate action for applink URIs with host "read"
     * e.g., jetpack://read/feeds/{feedId}/posts/{postId}
     *
     * Path segments for jetpack://read/feeds/138734090/posts/5879194632:
     * [0] = "feeds", [1] = "138734090", [2] = "posts", [3] = "5879194632"
     */
    @Suppress("ComplexCondition")
    private fun buildNavigateActionForReadHost(uri: UriWrapper): NavigateAction {
        val segments = uri.pathSegments
        // Check if it's a post URL: read/feeds/{feedId}/posts/{postId}
        if (segments.size >= APPLINK_POST_URL_SEGMENTS &&
            (segments[APPLINK_FEEDS_PATH_POSITION] == PATH_FEEDS ||
                segments[APPLINK_FEEDS_PATH_POSITION] == PATH_BLOGS) &&
            segments[APPLINK_POSTS_PATH_POSITION] == PATH_POSTS
        ) {
            // Convert applink to https URL that OpenInReader can handle
            val httpsUri = UriWrapper(
                android.net.Uri.parse(
                    "https://$HOST_WORDPRESS_COM/$PATH_READ/${segments.joinToString("/")}"
                )
            )
            return OpenInReader(httpsUri)
        }
        return OpenReader
    }

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        return when {
            uri.host == DEEP_LINK_HOST_READ -> buildNavigateActionForReadHost(uri)
            uri.host == DEEP_LINK_HOST_VIEWPOST -> {
                val blogId = uri.getQueryParameter(BLOG_ID)?.toLongOrNull()
                val postId = uri.getQueryParameter(POST_ID)?.toLongOrNull()
                if (blogId != null && postId != null) {
                    analyticsUtilsWrapper.trackWithBlogPostDetails(READER_VIEWPOST_INTERCEPTED, blogId, postId)
                    ViewPostInReader(blogId, postId, uri)
                } else {
                    _toast.value = Event(R.string.error_generic)
                    OpenReader
                }
            }
            isWordPressComReaderUrl(uri) -> OpenReader
            isWordPressComDiscoverUrl(uri) -> OpenReaderDiscover
            isWordPressComReaderSearchUrl(uri) -> OpenReaderSearch
            isWordPressComFeedUrl(uri) -> {
                val feedId = extractFeedId(uri)
                if (feedId != null) {
                    OpenFeedInReader(feedId)
                } else {
                    _toast.value = Event(R.string.error_generic)
                    OpenReader
                }
            }
            isWordPressComTagUrl(uri) -> {
                val tagSlug = extractTagSlug(uri)
                if (!tagSlug.isNullOrBlank()) {
                    OpenTagInReader(tagSlug)
                } else {
                    _toast.value = Event(R.string.error_generic)
                    OpenReader
                }
            }
            else -> OpenInReader(uri)
        }
    }

    /**
     * URLs handled here
     * `wordpress://read`
     * `wordpress://viewpost?blogId={blogId}&postId={postId}`
     * wordpress.com/read
     * wordpress.com/read/feeds/feedId/posts/feedItemId
     * wordpress.com/read/blogs/feedId/posts/feedItemId
     * wordpress.com/reader/feeds/feedId/posts/feedItemId
     * wordpress.com/discover
     * domain.wordpress.com/2.../../../postId
     * domain.wordpress.com/19../../../postId
     */
    override fun stripUrl(uri: UriWrapper): String {
        return when {
            uri.host == DEEP_LINK_HOST_READ -> "$APPLINK_SCHEME$DEEP_LINK_HOST_READ"
            uri.host == DEEP_LINK_HOST_VIEWPOST -> {
                val hasBlogId = uri.getQueryParameter(BLOG_ID) != null
                val hasPostId = uri.getQueryParameter(POST_ID) != null
                buildString {
                    append("$APPLINK_SCHEME$DEEP_LINK_HOST_VIEWPOST")
                    if (hasBlogId || hasPostId) {
                        append("?")
                        if (hasBlogId) {
                            append("$BLOG_ID=$BLOG_ID")
                            if (hasPostId) {
                                append("&")
                            }
                        }
                        if (hasPostId) {
                            append("$POST_ID=$POST_ID")
                        }
                    }
                }
            }
            isWordPressComReaderUrl(uri) -> "$HOST_WORDPRESS_COM/$PATH_READ"
            isWordPressComDiscoverUrl(uri) -> "$HOST_WORDPRESS_COM/$PATH_DISCOVER"
            isWordPressComReaderSearchUrl(uri) -> "$HOST_WORDPRESS_COM/$PATH_READ/$PATH_SEARCH"
            isWordPressComFeedUrl(uri) -> "$HOST_WORDPRESS_COM/$PATH_READ/$PATH_FEEDS/$FEED_ID"
            isWordPressComTagUrl(uri) -> "$HOST_WORDPRESS_COM/$PATH_TAG/$TAG_SLUG"
            else -> {
                buildString {
                    val segments = uri.pathSegments
                    // Handled URLs look like this: http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
                    // or http[s]://wordpress.com/reader/feeds/{feedId}/posts/{feedItemId}
                    // with the first segment being 'read' or 'reader'.
                    append(stripHost(uri))
                    if (isReadOrReaderPath(segments.firstOrNull())) {
                        appendReadPath(segments)
                    } else if (segments.size > DATE_URL_SEGMENTS) {
                        append("/YYYY/MM/DD/$POST_ID")
                    }
                }.ifEmpty { uri.host + uri.pathSegments.firstOrNull() }
            }
        }
    }

    private fun stripHost(uri: UriWrapper): String {
        val domains = uri.host?.split(".") ?: listOf()
        return if (domains.size >= CUSTOM_DOMAIN_POSITION &&
            domains[domains.size - CUSTOM_DOMAIN_POSITION] != "www"
        ) {
            "$SITE_DOMAIN.$HOST_WORDPRESS_COM"
        } else {
            uri.host ?: HOST_WORDPRESS_COM
        }
    }

    private fun StringBuilder.appendReadPath(segments: List<String>) {
        append("/read")
        when (segments.getOrNull(SECOND_PATH_POSITION)) {
            "blogs" -> {
                append("/blogs/$FEED_ID")
            }
            "feeds" -> {
                append("/feeds/$FEED_ID")
            }
        }
        if (segments.getOrNull(POSTS_PATH_POSITION) == "posts") {
            append("/posts/feedItemId")
        }
    }

    companion object {
        // Applink hosts (wordpress://read, wordpress://viewpost)
        private const val DEEP_LINK_HOST_READ = "read"
        private const val DEEP_LINK_HOST_VIEWPOST = "viewpost"

        // URL path segments
        private const val PATH_READ = "read"
        private const val PATH_READER = "reader"
        private const val PATH_DISCOVER = "discover"
        private const val PATH_FEEDS = "feeds"
        private const val PATH_BLOGS = "blogs"
        private const val PATH_POSTS = "posts"
        private const val PATH_SEARCH = "search"
        private const val PATH_TAG = "tag"

        // Query and path parameter names (used for analytics stripping)
        private const val BLOG_ID = "blogId"
        private const val POST_ID = "postId"
        private const val FEED_ID = "feedId"
        private const val TAG_SLUG = "tagSlug"

        // URL segment positions
        private const val SECOND_PATH_POSITION = 1
        private const val FEED_ID_POSITION = 2
        private const val TAG_SLUG_POSITION = 1
        private const val POSTS_PATH_POSITION = 3
        private const val CUSTOM_DOMAIN_POSITION = 3

        // Expected URL segment counts
        private const val DATE_URL_SEGMENTS = 3
        private const val FEED_URL_SEGMENTS = 3
        private const val SEARCH_URL_SEGMENTS = 2
        private const val TAG_URL_SEGMENTS = 2

        // Applink URL segment positions (for jetpack://read/feeds/{feedId}/posts/{postId})
        private const val APPLINK_FEEDS_PATH_POSITION = 0
        private const val APPLINK_POSTS_PATH_POSITION = 2
        private const val APPLINK_POST_URL_SEGMENTS = 4
    }
}
