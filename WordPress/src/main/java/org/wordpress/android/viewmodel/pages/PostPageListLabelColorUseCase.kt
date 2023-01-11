package org.wordpress.android.viewmodel.pages

import androidx.annotation.ColorRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

const val ERROR_COLOR = R.color.error
const val PROGRESS_INFO_COLOR = R.color.neutral_50
const val STATE_INFO_COLOR = R.color.warning_dark

class PostPageListLabelColorUseCase @Inject constructor() {
    @ColorRes
    fun getLabelsColor(
        post: PostModel,
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean,
        hasUnhandledAutoSave: Boolean
    ): Int? {
        val isError = isError(uploadUiState, hasUnhandledConflicts)
        val isProgressInfo = isProgressInfo(uploadUiState)
        val isStateInfo = isStateInfoState(uploadUiState, post, hasUnhandledAutoSave)

        return when {
            isError -> ERROR_COLOR
            isProgressInfo -> PROGRESS_INFO_COLOR
            isStateInfo -> STATE_INFO_COLOR
            else -> null
        }
    }

    private fun isError(
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean
    ) = (uploadUiState is UploadFailed && !uploadUiState.isEligibleForAutoUpload) ||
            hasUnhandledConflicts

    private fun isProgressInfo(uploadUiState: PostUploadUiState) =
        uploadUiState is UploadingPost || uploadUiState is UploadingMedia ||
                uploadUiState is UploadQueued

    private fun isStateInfoState(
        uploadUiState: PostUploadUiState,
        post: PostModel,
        hasUnhandledAutoSave: Boolean
    ): Boolean {
        val postStatus = PostStatus.fromPost(post)

        return (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload) ||
                post.isLocalDraft || post.isLocallyChanged || postStatus == PRIVATE || postStatus == PENDING ||
                post.sticky || uploadUiState is UploadWaitingForConnection || hasUnhandledAutoSave
    }
}
