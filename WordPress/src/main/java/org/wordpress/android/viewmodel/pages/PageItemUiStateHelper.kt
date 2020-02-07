package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.NothingToUpload
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PageItemUiStateHelper.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import javax.inject.Inject

typealias ShouldShowOverlay = Boolean

class PageItemUiStateHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val postStore: PostStore,
    val uploadStatusTracker: PostModelUploadStatusTracker
) {
    fun getProgressStateForPage(
        pageId: LocalId,
        site: SiteModel
    ): Pair<ProgressBarUiState, ShouldShowOverlay> {
        val post = postStore.getPostByLocalPostId(pageId.value)

        post?.let {
            val uploadStatus = uploadStatusTracker.getUploadStatus(
                    post, site
            )
            val uploadUiState = createUploadUiState(uploadStatus, post)

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

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    sealed class PostUploadUiState {
        data class UploadingMedia(val progress: Int) : PostUploadUiState()
        data class UploadingPost(val isDraft: Boolean) : PostUploadUiState()
        data class UploadFailed(
            val error: UploadError,
            val isEligibleForAutoUpload: Boolean,
            val retryWillPushChanges: Boolean
        ) : PostUploadUiState()

        data class UploadWaitingForConnection(val postStatus: PostStatus) : PostUploadUiState()
        object UploadQueued : PostUploadUiState()
        object NothingToUpload : PostUploadUiState()
    }

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    private fun createUploadUiState(
        uploadStatus: PostListItemUploadStatus,
        post: PostModel
    ): PostUploadUiState {
        val postStatus = PostStatus.fromPost(post)
        return when {
            uploadStatus.hasInProgressMediaUpload -> UploadingMedia(
                    uploadStatus.mediaUploadProgress
            )
            uploadStatus.isUploading -> UploadingPost(
                    postStatus == DRAFT
            )
            // the upload error is not null on retry -> it needs to be evaluated after UploadingMedia and UploadingPost
            uploadStatus.uploadError != null -> UploadFailed(
                    uploadStatus.uploadError,
                    uploadStatus.isEligibleForAutoUpload,
                    uploadStatus.uploadWillPushChanges
            )
            uploadStatus.hasPendingMediaUpload ||
                    uploadStatus.isQueued ||
                    uploadStatus.isUploadingOrQueued -> UploadQueued
            uploadStatus.isEligibleForAutoUpload -> UploadWaitingForConnection(postStatus)
            else -> NothingToUpload
        }
    }

    fun setupPageActions(listType: PageListType, pageId: LocalId, site: SiteModel): Set<Action> {
        return when (listType) {
            PUBLISHED -> mutableSetOf(VIEW_PAGE, SET_PARENT, MOVE_TO_DRAFT, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(pageId, site)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }

            DRAFTS -> mutableSetOf(VIEW_PAGE, SET_PARENT, PUBLISH_NOW, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(pageId, site)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }

            SCHEDULED -> mutableSetOf(VIEW_PAGE, SET_PARENT, MOVE_TO_DRAFT, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(pageId, site)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }
            TRASHED -> setOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY)
        }
    }

    private fun canCancelPendingAutoUpload(pageId: LocalId, site: SiteModel): Boolean {
        val post = postStore.getPostByLocalPostId(pageId.value)
        val uploadUiState = createUploadUiState(
                uploadStatusTracker.getUploadStatus(post, site),
                post
        )
        return (uploadUiState is UploadWaitingForConnection ||
                (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload))
    }

    private fun shouldShowOverlay(uploadUiState: PostUploadUiState): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return (uploadUiState is UploadingPost ||
                (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }
}
