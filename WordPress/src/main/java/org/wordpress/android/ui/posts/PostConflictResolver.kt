package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
@Suppress("LongParameterList")
class PostConflictResolver(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val getPostByLocalPostId: (Int) -> PostModel?,
    private val invalidateList: () -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val showSnackbar: (SnackbarMessageHolder) -> Unit,
    private val showToast: (ToastMessageHolder) -> Unit,
    private val uploadStore: UploadStore,
    private val postStore: PostStore
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
            showToast.invoke(ToastMessageHolder(R.string.toast_conflict_updating_post, Duration.SHORT))
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
        showSnackbar.invoke(snackBarHolder)

        PostUtils.trackSavePostAnalytics(post, site)
        val remotePostPayload = RemotePostPayload(post, site)
        remotePostPayload.shouldSkipConflictResolutionCheck = true
        dispatcher.dispatch(PostActionBuilder.newPushPostAction(remotePostPayload))
    }

    fun doesPostHaveUnhandledConflict(post: PostModel): Boolean =
        uploadStore.getUploadErrorForPost(post)?.postError?.type == PostErrorType.OLD_REVISION ||
                PostUtils.isPostInConflictWithRemote(post)

    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
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
        showSnackbar.invoke(snackBarHolder)
    }
}
