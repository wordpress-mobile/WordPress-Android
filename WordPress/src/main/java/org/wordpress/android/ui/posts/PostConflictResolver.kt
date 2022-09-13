package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
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
    private val showToast: (ToastMessageHolder) -> Unit
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
        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPost = localPostIdForFetchingRemoteVersionOfConflictedPost != null &&
                localPostIdForFetchingRemoteVersionOfConflictedPost == post.id
        return !isFetchingConflictedPost && PostUtils.isPostInConflictWithRemote(post)
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
