package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes

@Suppress("LongParameterList")
class PostConflictResolver(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val postStore: PostStore,
    private val uploadStore: UploadStore,
    private val getPostByLocalPostId: (Int) -> PostModel?,
    private val invalidateList: () -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val showSnackBar: (SnackbarMessageHolder) -> Unit
) {
    private var originalPostId: Int? = null

    fun updateConflictedPostWithRemoteVersion(localPostId: Int) {
        // We need network connection to load a remote post
        if (!checkNetworkConnection()) {
            return
        }
        val post = getPostByLocalPostId.invoke(localPostId)
        if (post != null) {
            originalPostId = post.id
            post.error = null
            post.setIsLocallyChanged(false)
            post.setAutoSaveExcerpt(null)
            post.setAutoSaveRevisionId(0)
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(post, site)))
        }
    }

    fun updateConflictedPostWithLocalVersion(localPostId: Int) {
        // We need network connection to push local version to remote
        if (!checkNetworkConnection()) {
            return
        }
        invalidateList.invoke()
        val post = getPostByLocalPostId.invoke(localPostId) ?: return
        post.error = null
        uploadStore.clearUploadErrorForPost(post)
        val snackBarHolder = SnackbarMessageHolder(
            UiStringRes(R.string.snackbar_conflict_web_version_discarded)
        )
        showSnackBar.invoke(snackBarHolder)
        PostUtils.trackSavePostAnalytics(post, site)
        val remotePostPayload = RemotePostPayload(post, site)
        remotePostPayload.shouldSkipConflictResolutionCheck = true
        dispatcher.dispatch(PostActionBuilder.newPushPostAction(remotePostPayload))
    }

    fun onPostSuccessfullyUpdated() {
        originalPostId?.let { id ->
            val updatedPost = getPostByLocalPostId.invoke(id)
            originalPostId = null
            // Conflicted post has been successfully updated with its remote version
            uploadStore.clearUploadErrorForPost(updatedPost)
            postStore.removeLocalRevision(updatedPost)
            if (!PostUtils.isPostInConflictWithRemote(updatedPost)) {
                conflictedPostUpdatedWithRemoteVersion()
            }
        }
    }

    private fun conflictedPostUpdatedWithRemoteVersion() {
        val snackBarHolder = SnackbarMessageHolder(
            UiStringRes(R.string.snackbar_conflict_local_version_discarded)
        )
        showSnackBar.invoke(snackBarHolder)
    }
}
