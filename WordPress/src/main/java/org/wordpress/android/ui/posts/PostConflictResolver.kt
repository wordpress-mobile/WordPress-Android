package org.wordpress.android.ui.posts

import android.util.Log
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
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
    private val uploadStore: UploadStore
) {
    private var originalPostCopyForConflictUndo: PostModel? = null
    private var localPostIdForFetchingRemoteVersionOfConflictedPost: Int? = null

    fun updateConflictedPostWithRemoteVersion(localPostId: Int) {
        // We need network connection to load a remote post
        if (!checkNetworkConnection()) {
            return
        }

        val post = getPostByLocalPostId.invoke(localPostId)
        if (post != null) {

            // todo : do we need something more here?
            post.error = null
            uploadStore.clearUploadErrorForPost(post)

            originalPostCopyForConflictUndo = post.clone()
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(post, site)))
            showToast.invoke(ToastMessageHolder(R.string.toast_conflict_updating_post, Duration.SHORT))
        }
    }

    fun updateConflictedPostWithLocalVersion(localPostId: Int) {
        // We need network connection to push local version to remote
        if (!checkNetworkConnection()) {
            return
        }

        // Keep a reference to which post is being updated with the local version so we can avoid showing the conflicted
        // label during the undo snackBar.
        localPostIdForFetchingRemoteVersionOfConflictedPost = localPostId
        invalidateList.invoke()

        val post = getPostByLocalPostId.invoke(localPostId) ?: return

        // todo : do we need something more here?
        post.error = null
        uploadStore.clearUploadErrorForPost(post)


        // and now show a snackBar, acting as if the Post was pushed, but effectively push it after the snackbar is gone
        var isUndoed = false
        val undoAction = {
            isUndoed = true

            // Remove the reference for the post being updated and re-show the conflicted label on undo
            localPostIdForFetchingRemoteVersionOfConflictedPost = null
            invalidateList.invoke()
        }

        val onDismissAction = { _: Int ->
            if (!isUndoed) {
                localPostIdForFetchingRemoteVersionOfConflictedPost = null
                PostUtils.trackSavePostAnalytics(post, site)
                dispatcher.dispatch(PostActionBuilder.newPushPostAction(RemotePostPayload(post, site)))
            }
        }
        val snackBarHolder = SnackbarMessageHolder(
            UiStringRes(R.string.snackbar_conflict_web_version_discarded),
            UiStringRes(R.string.snackbar_conflict_undo),
            undoAction,
            onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
    }

    fun doesPostHaveUnhandledConflict(post: PostModel): Boolean {
        Log.d("uiState", "doesPostHaveUnhandledConflict() entered for post: ${post.title}")
        var isOldRevision = false
        val uploadError = uploadStore.getUploadErrorForPost(post)
        if (uploadError != null) {
            if (uploadError.postError.type == PostErrorType.OLD_REVISION) {
                isOldRevision = true
            }
        }
       /* val isOldRevision = uploadStore.getUploadErrorForPost(post)?.postError?.type.toString() ==
                PostErrorType.OLD_REVISION.toString()*/

        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPost = localPostIdForFetchingRemoteVersionOfConflictedPost != null &&
                localPostIdForFetchingRemoteVersionOfConflictedPost == post.id
        val x = isOldRevision || (!isFetchingConflictedPost && PostUtils.isPostInConflictWithRemote(post))
        Log.d("uiState", "doesPostHaveUnhandledConflict() exited for post: ${post.title} with value: $x")

        return x
    }

    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
    }

    fun onPostSuccessfullyUpdated() {
        originalPostCopyForConflictUndo?.id?.let {
            val updatedPost = getPostByLocalPostId.invoke(it)
            // Conflicted post has been successfully updated with its remote version
            if (!PostUtils.isPostInConflictWithRemote(updatedPost)) {
                conflictedPostUpdatedWithRemoteVersion()
            }
        }
    }

    private fun conflictedPostUpdatedWithRemoteVersion() {
        val undoAction = {
            // here replace the post with whatever we had before, again
            if (originalPostCopyForConflictUndo != null) {
                dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(originalPostCopyForConflictUndo))
            }
        }
        val onDismissAction = { _: Int ->
            originalPostCopyForConflictUndo = null
        }
        val snackBarHolder = SnackbarMessageHolder(
            UiStringRes(R.string.snackbar_conflict_local_version_discarded),
            UiStringRes(R.string.snackbar_conflict_undo),
            undoAction,
            onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
    }
}
