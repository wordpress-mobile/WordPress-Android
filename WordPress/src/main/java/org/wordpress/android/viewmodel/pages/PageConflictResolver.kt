package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import javax.inject.Inject

class PageConflictResolver @Inject constructor() {
    private var originalPostCopyForConflictUndo: PostModel? = null
    private var postIdForFetchingRemoteVersionOfConflictedPost: LocalId? = null

    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
    }

    fun doesPostHaveUnhandledConflict(post: PostModel): Boolean {
        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPost = postIdForFetchingRemoteVersionOfConflictedPost != null &&
                postIdForFetchingRemoteVersionOfConflictedPost == LocalId(post.id)
        return !isFetchingConflictedPost && PostUtils.isPostInConflictWithRemote(post)
    }

    fun updateConflictedPostWithRemoteVersion(localPostId: Int) {
        // TODO ensure that this function is being ran from a network check block.

        val post = getPostByLocalPostId.invoke(localPostId)
        if (post != null) {
            originalPostCopyForConflictUndo = post.clone()
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(post, site)))
            showToast.invoke(ToastMessageHolder(string.toast_conflict_updating_post, SHORT))
        }
    }

    fun updateConflictedPostWithLocalVersion(postId: LocalId) {
        // TODO ensure that this function is being ran from a network check block.


        // Keep a reference to which post is being updated with the local version so we can avoid showing the conflicted
        // label during the undo snackBar.
        postIdForFetchingRemoteVersionOfConflictedPost = postId
        invalidateList.invoke()

        val post = getPostByLocalPostId.invoke(postId) ?: return

        // and now show a snackBar, acting as if the Post was pushed, but effectively push it after the snackbar is gone
        var isUndoed = false
        val undoAction = {
            isUndoed = true

            // Remove the reference for the post being updated and re-show the conflicted label on undo
            postIdForFetchingRemoteVersionOfConflictedPost = null
            invalidateList.invoke()
        }

        val onDismissAction = {
            if (!isUndoed) {
                postIdForFetchingRemoteVersionOfConflictedPost = null
                PostUtils.trackSavePostAnalytics(post, site)
                dispatcher.dispatch(PostActionBuilder.newPushPostAction(RemotePostPayload(post, site)))
            }
        }
        val snackBarHolder = SnackbarMessageHolder(
                string.snackbar_conflict_web_version_discarded,
                string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
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
        val onDismissAction = {
            originalPostCopyForConflictUndo = null
        }
        val snackBarHolder = SnackbarMessageHolder(
                R.string.snackbar_conflict_local_version_discarded,
                R.string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
    }
}
