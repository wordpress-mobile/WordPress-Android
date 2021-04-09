package org.wordpress.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditor
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenEditorForSite
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenInEditor
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class EditorLinkHandler
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val siteStore: SiteStore,
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
     * Converts HOST name of a site to SiteModel. It finds the Site in the current local sites and matches the name
     * to the host.
     */
    private fun String.toSite(): SiteModel? {
        val site = extractSiteModelFromTargetHost(this)
        val host = deepLinkUriUtils.extractHostFromSite(site)
        // Check if a site is available with given targetHost
        return if (site != null && host != null && host == this) {
            site
        } else {
            null
        }
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
            else -> OpenInEditor(site, post.id)
        }
    }

    private fun extractSiteModelFromTargetHost(host: String): SiteModel? {
        return siteStore.getSitesByNameOrUrlMatching(host).firstOrNull()
    }
}
