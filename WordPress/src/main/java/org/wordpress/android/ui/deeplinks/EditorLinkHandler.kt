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
     * or App links like wordpress://post?blogId=798&postId=1231
     */
    fun buildOpenEditorNavigateAction(uri: UriWrapper): NavigateAction {
        var hasSiteParam = false
        var hasPostParam = false
        val (targetSite, targetPost) = if (uri.host == POST_PATH) {
            // Handles wordpress://post?blogId=798&postId=1231
            val targetSite = uri.getQueryParameter(BLOG_ID).also { hasSiteParam = it != null }?.blogIdToSite()
            val targetPost = uri.getQueryParameter(POST_ID).also { hasPostParam = it != null }?.toPost(targetSite)
            targetSite to targetPost
        } else {
            // Handles https://wordpress.com/post/siteNameOrUrl/postId
            val pathSegments = uri.pathSegments
            val targetSite = pathSegments.getOrNull(1).also { hasSiteParam = it != null }?.hostNameToSite()
            val targetPost = pathSegments.getOrNull(2).also { hasPostParam = it != null }?.toPost(targetSite)
            targetSite to targetPost
        }
        return openEditorForSiteAndPost(hasSiteParam, hasPostParam, targetSite, targetPost)
    }

    /**
     * Returns true if the URI should be handled by EditorLinkHandler.
     * The handled links are `wordpress.com/post...1
     */
    fun isEditorUrl(uri: UriWrapper): Boolean {
        return (uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == POST_PATH) || uri.host == POST_PATH
    }

    /**
     * Converts BlogID if long or Site URL for other cases to a SiteModel
     */
    private fun String.blogIdToSite(): SiteModel? {
        return deepLinkUriUtils.blogIdToSite(this) ?: this.hostNameToSite()
    }

    /**
     * Converts HOST name of a site to SiteModel. It finds the Site in the current local sites and matches the name
     * to the host.
     */
    private fun String.hostNameToSite(): SiteModel? {
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

    private fun openEditorForSiteAndPost(
        hasSiteParam: Boolean,
        hasPostParam: Boolean,
        site: SiteModel?,
        post: PostModel?
    ): NavigateAction {
        return when {
            site == null -> {
                if (hasSiteParam) {
                    // Site not found, or host of site doesn't match the host in url
                    _toast.value = Event(R.string.blog_not_found)
                }
                // Open a new post editor with current selected site
                OpenEditor
            }
            post == null -> {
                if (hasPostParam) {
                    // Post not found. Open new post editor for given site.
                    _toast.value = Event(R.string.post_not_found)
                }
                // Open new post editor for given site
                OpenEditorForSite(site)
            }
            else -> OpenEditorForPost(site, post.id)
        }
    }

    companion object {
        private const val POST_PATH = "post"
        private const val BLOG_ID = "blogId"
        private const val POST_ID = "postId"
    }
}
