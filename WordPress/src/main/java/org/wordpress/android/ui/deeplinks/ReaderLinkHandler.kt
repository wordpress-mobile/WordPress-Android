package org.wordpress.android.ui.deeplinks

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_VIEWPOST_INTERCEPTED
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenInReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenReader
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.ViewPostInReader
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
) {
    private val _toast = MutableLiveData<Event<Int>>()
    val toast = _toast as LiveData<Event<Int>>

    /**
     * URIs supported by the Reader are already defined as intent filters in the manifest. Instead of replicating
     * that logic here, we simply check if we can resolve an [Intent] that uses [ReaderConstants.ACTION_VIEW_POST].
     * Since that's a custom action that is only handled by the Reader, we can then assume it supports this URI.
     * Other deeplinks handled:
     * `wordpress://read`
     * `wordpress://viewpost?blogId={blogId}&postId={postId}`
     */
    fun isReaderUrl(uri: UriWrapper) =
            DEEP_LINK_HOST_READ == uri.host || DEEP_LINK_HOST_VIEWPOST == uri.host || intentUtils.canResolveWith(
                    ReaderConstants.ACTION_VIEW_POST,
                    uri
            )

    fun buildOpenInReaderNavigateAction(uri: UriWrapper): NavigateAction {
        return when (uri.host) {
            DEEP_LINK_HOST_READ -> OpenReader
            DEEP_LINK_HOST_VIEWPOST -> {
                val blogId = uri.getQueryParameter("blogId")?.toLongOrNull()
                val postId = uri.getQueryParameter("postId")?.toLongOrNull()
                if (blogId != null && postId != null) {
                    analyticsUtilsWrapper.trackWithBlogPostDetails(READER_VIEWPOST_INTERCEPTED, blogId, postId)
                    ViewPostInReader(blogId, postId, uri)
                } else {
                    _toast.value = Event(R.string.error_generic)
                    OpenReader
                }
            }
            else -> OpenInReader(uri)
        }
    }

    companion object {
        private const val DEEP_LINK_HOST_READ = "read"
        private const val DEEP_LINK_HOST_VIEWPOST = "viewpost"
    }
}
