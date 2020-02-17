package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.util.ToastUtils.Duration.SHORT
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder

class PageConflictResolver(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val getPageByLocalPostId: (Int) -> PostModel?,
    private val invalidateList: () -> Unit,
    private val showSnackbar: (SnackbarMessageHolder) -> Unit,
    private val showToast: (ToastMessageHolder) -> Unit
) {
    private var originalPageCopyForConflictUndo: PostModel? = null
    private var pageIdForFetchingRemoteVersionOfConflictedPage: LocalId? = null

    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
    }

    fun doesPageHaveUnhandledConflict(page: PostModel): Boolean {
        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPage = pageIdForFetchingRemoteVersionOfConflictedPage != null &&
                pageIdForFetchingRemoteVersionOfConflictedPage == LocalId(page.id)
        return !isFetchingConflictedPage && PostUtils.isPostInConflictWithRemote(page)
    }

    fun updateConflictedPageWithRemoteVersion(pageId: LocalId) {
        // TODO ensure that this function is being ran from a network check block.

        val page = getPageByLocalPostId.invoke(pageId.value)
        if (page != null) {
            originalPageCopyForConflictUndo = page.clone()
            dispatcher.dispatch(PostActionBuilder.newFetchPostAction(RemotePostPayload(page, site)))
            showToast.invoke(ToastMessageHolder(string.toast_conflict_updating_post, SHORT))
        }
    }

    fun updateConflictedPageWithLocalVersion(pageId: LocalId) {
        // TODO ensure that this function is being ran from a network check block.

        // Keep a reference to which page is being updated with the local version so we can avoid showing the conflicted
        // label during the undo snackBar.
        pageIdForFetchingRemoteVersionOfConflictedPage = pageId
        invalidateList.invoke()

        val page = getPageByLocalPostId.invoke(pageId.value) ?: return

        // and now show a snackBar, acting as if the page was pushed, but effectively push it after the snackbar is gone
        var isUndoed = false
        val undoAction = {
            isUndoed = true

            // Remove the reference for the page being updated and re-show the conflicted label on undo
            pageIdForFetchingRemoteVersionOfConflictedPage = null
            invalidateList.invoke()
        }

        val onDismissAction = {
            if (!isUndoed) {
                pageIdForFetchingRemoteVersionOfConflictedPage = null
                PostUtils.trackSavePostAnalytics(page, site)
                dispatcher.dispatch(
                        PostActionBuilder.newPushPostAction(
                                RemotePostPayload(
                                        page,
                                        site
                                )
                        )
                )
            }
        }
        val snackBarHolder = SnackbarMessageHolder(
                string.snackbar_conflict_web_version_discarded,
                string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
    }

    fun onPageSuccessfullyUpdated() {
        originalPageCopyForConflictUndo?.id?.let {
            val updatedPost = getPageByLocalPostId.invoke(it)
            // Conflicted post has been successfully updated with its remote version
            if (!PostUtils.isPostInConflictWithRemote(updatedPost)) {
                conflictedPostUpdatedWithRemoteVersion()
            }
        }
    }

    private fun conflictedPostUpdatedWithRemoteVersion() {
        val undoAction = {
            // here replace the post with whatever we had before, again
            if (originalPageCopyForConflictUndo != null) {
                dispatcher.dispatch(
                        PostActionBuilder.newUpdatePostAction(
                                originalPageCopyForConflictUndo
                        )
                )
            }
        }
        val onDismissAction = {
            originalPageCopyForConflictUndo = null
        }
        val snackBarHolder = SnackbarMessageHolder(
                R.string.snackbar_conflict_local_version_discarded,
                R.string.snackbar_conflict_undo, undoAction, onDismissAction
        )
        showSnackbar.invoke(snackBarHolder)
    }
}
