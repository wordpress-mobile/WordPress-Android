package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.NothingToUpload
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PageItemProgressHelper.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.posts.PostListItemProgressBar
import org.wordpress.android.viewmodel.posts.PostListItemUploadStatus

import javax.inject.Inject

typealias ShouldShowOverlay = Boolean

class PageItemProgressHelper @Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) {
    fun getProgressBarState(
        uploadUiState: PostUploadUiState,
        performingCriticalAction: Boolean
    ): PostListItemProgressBar {
        return if (shouldShowProgress(uploadUiState, performingCriticalAction)) {
            if (uploadUiState is UploadingMedia) {
                PostListItemProgressBar.Determinate(uploadUiState.progress)
            } else {
                PostListItemProgressBar.Indeterminate
            }
        } else {
            PostListItemProgressBar.Hidden
        }
    }

    private fun shouldShowProgress(
        uploadUiState: PostUploadUiState,
        performingCriticalAction: Boolean
    ): Boolean {
        return performingCriticalAction || uploadUiState is UploadingPost || uploadUiState is UploadingMedia ||
                uploadUiState is UploadQueued
    }

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

    fun createUploadUiState(
        uploadStatus: PostListItemUploadStatus,
        post: PostModel
    ):PostUploadUiState {
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

    fun shouldShowOverlay(
        uploadUiState: PostUploadUiState,
        performingCriticalAction: Boolean
    ): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return performingCriticalAction ||
                (uploadUiState is UploadingPost ||
                        (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }
}
