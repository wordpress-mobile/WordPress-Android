package org.wordpress.android.ui.deeplinks.handlers

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_VIEWPOST_INTERCEPTED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReaderDiscover
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

    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        return when {
            uri.host == DEEP_LINK_HOST_READ -> OpenReader
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
            else -> {
                buildString {
                    val segments = uri.pathSegments
                    // Handled URLs look like this: http[s]://wordpress.com/read/feeds/{feedId}/posts/{feedItemId}
                    // with the first segment being 'read'.
                    append(stripHost(uri))
                    val firstSegment = segments.firstOrNull()
                    if (firstSegment == PATH_READ || firstSegment == "reader") {
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
        when (segments.getOrNull(BLOGS_FEEDS_PATH_POSITION)) {
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
        private const val DEEP_LINK_HOST_READ = "read"
        private const val DEEP_LINK_HOST_VIEWPOST = "viewpost"
        private const val PATH_READ = "read"
        private const val PATH_DISCOVER = "discover"
        private const val BLOG_ID = "blogId"
        private const val POST_ID = "postId"
        private const val FEED_ID = "feedId"
        private const val CUSTOM_DOMAIN_POSITION = 3
        private const val BLOGS_FEEDS_PATH_POSITION = 1
        private const val POSTS_PATH_POSITION = 3
        private const val DATE_URL_SEGMENTS = 3
    }
}
