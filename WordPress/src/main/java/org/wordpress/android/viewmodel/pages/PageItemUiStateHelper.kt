package org.wordpress.android.viewmodel.pages

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import javax.inject.Inject

typealias ShouldShowOverlay = Boolean

class PageItemUiStateHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val postStore: PostStore,
    private val createPageUploadUiStateUseCase: CreatePageUploadUiStateUseCase
) {
    fun getProgressStateForPage(
        post: PostModel?,
        uploadUiState: PostUploadUiState
    ): Pair<ProgressBarUiState, ShouldShowOverlay> {
        // TODO ideally don't accept nullable PostModel
        post?.let {
            val shouldShowOverlay = shouldShowOverlay(uploadUiState)
            return Pair(getProgressBarState(uploadUiState), shouldShowOverlay)
        }
        return Pair(ProgressBarUiState.Hidden, false)
    }

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    private fun getProgressBarState(
        uploadUiState: PostUploadUiState
    ): ProgressBarUiState {
        return if (shouldShowProgress(uploadUiState)) {
            if (uploadUiState is UploadingMedia) {
                ProgressBarUiState.Determinate(uploadUiState.progress)
            } else {
                ProgressBarUiState.Indeterminate
            }
        } else {
            ProgressBarUiState.Hidden
        }
    }

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    private fun shouldShowProgress(
        uploadUiState: PostUploadUiState
    ): Boolean {
        return uploadUiState is UploadingPost || uploadUiState is UploadingMedia ||
                uploadUiState is UploadQueued
    }
   // TODO MERGE CONFLICT
    fun setupPageActions(listType: PageListType, pageId: LocalId, site: SiteModel): Set<Action> {
        return when (listType) {
            SCHEDULED, PUBLISHED -> mutableSetOf(VIEW_PAGE, SET_PARENT, MOVE_TO_DRAFT, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(pageId, site)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }
            DRAFTS -> mutableSetOf(VIEW_PAGE, SET_PARENT, PUBLISH_NOW, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(pageId, site)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }
            TRASHED -> setOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY)
        }
    }
    // TODO MERGE CONFLICT
    @VisibleForTesting(otherwise = PRIVATE)
    fun canCancelPendingAutoUpload(pageId: LocalId, site: SiteModel): Boolean {
        val post = postStore.getPostByLocalPostId(pageId.value)

        post?.let {
            val uploadUiState = createPageUploadUiStateUseCase.createUploadUiState(post, site)
            return (uploadUiState is UploadWaitingForConnection ||
                    (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload))
        }

        return false
    }

    private fun shouldShowOverlay(uploadUiState: PostUploadUiState): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return (uploadUiState is UploadingPost ||
                (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }
}
