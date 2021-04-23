package org.wordpress.android.ui.deeplinks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForPost
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class EditorLinkHandler
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val postStore: PostStore
) {
    private val _toast = MutableLiveData<Event<Int>>()
    val toast = _toast as LiveData<Event<Int>>

    /**
     * Builds navigate action from URL like:
     * https://wordpress.com/post/siteNameOrUrl/postId
     * where siteNameOrUrl and postID are optional
     */
    fun buildOpenEditorNavigateAction(uri: UriWrapper): NavigateAction {
        val pathSegments = uri.pathSegments
        val targetSite = pathSegments.getOrNull(1)?.toSite()
        val targetPost = pathSegments.getOrNull(2)?.toPost(targetSite)
        return openEditorForSiteAndPost(targetSite, targetPost)
    }

    /**
     * Returns true if the URI should be handled by EditorLinkHandler.
     * The handled links are `wordpress.com/post...1
     */
    fun isEditorUrl(uri: UriWrapper): Boolean {
        return uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == POST_PATH
    }

    /**
     * Converts HOST name of a site to SiteModel. It finds the Site in the current local sites and matches the name
     * to the host.
     */
    private fun String.toSite(): SiteModel? {
        return deepLinkUriUtils.hostToSite(this)
    }

    /**
     * Converts the post ID in String to the local PostModel (if available).
     */
    private fun String.toPost(site: SiteModel?): PostModel? {
        val remotePostId: Long? = toLongOrNull()
        return if (site != null && remotePostId != null) {
            val post = postStore.getPostByRemotePostId(remotePostId, site)
            if (post == null) {
                _toast.value = Event(R.string.post_not_found)
            }
            post
        } else {
            null
        }
    }

    private fun openEditorForSiteAndPost(site: SiteModel?, post: PostModel?): NavigateAction {
        return when {
            site == null -> {
                // Site not found, or host of site doesn't match the host in url
                _toast.value = Event(R.string.blog_not_found)
                // Open a new post editor with current selected site
                OpenEditor
            }
            post == null -> {
                // Open new post editor for given site
                OpenEditorForSite(site)
            }
            else -> OpenEditorForPost(site, post.id)
        }
    }

    companion object {
        private const val POST_PATH = "post"
    }
}
