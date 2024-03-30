package org.wordpress.android.viewmodel.pages

import android.util.Log
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.NothingToUpload
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

class PostModelUploadUiStateUseCase @Inject constructor() {
    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    fun createUploadUiState(
        post: PostModel,
        site: SiteModel,
        uploadStatusTracker: PostModelUploadStatusTracker
    ): PostUploadUiState {
        val postStatus = PostStatus.fromPost(post)
        val uploadStatus = uploadStatusTracker.getUploadStatus(post, site)
        val x = when {
            // the upload error is not null on retry -> it needs to be evaluated after UploadingMedia and UploadingPost
            uploadStatus.uploadError != null -> UploadFailed(
                uploadStatus.uploadError,
                uploadStatus.isEligibleForAutoUpload,
                uploadStatus.uploadWillPushChanges
            )
            uploadStatus.hasInProgressMediaUpload -> UploadingMedia(
                uploadStatus.mediaUploadProgress
            )
            uploadStatus.isUploading -> UploadingPost(
                postStatus == DRAFT
            )
            uploadStatus.hasPendingMediaUpload ||
                    uploadStatus.isQueued ||
                    uploadStatus.isUploadingOrQueued -> UploadQueued
            uploadStatus.isEligibleForAutoUpload -> UploadWaitingForConnection(postStatus)
            else -> NothingToUpload
        }

        Log.d("uiState", "PostModelUploadUiStateUseCase.createUploadUiState(): returns PostUploadUiState = $x")

        return x
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
}
