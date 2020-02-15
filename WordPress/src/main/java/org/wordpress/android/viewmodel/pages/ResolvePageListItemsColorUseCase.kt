package org.wordpress.android.viewmodel.pages

import androidx.annotation.ColorRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.fromPost
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

const val ERROR_COLOR = R.color.error
const val PROGRESS_INFO_COLOR = R.color.neutral_50
const val STATE_INFO_COLOR = R.color.warning_dark

class ResolvePageListItemsColorUseCase @Inject constructor(
    private val pageConflictResolver: PageConflictResolver
) {
    @ColorRes fun getLabelsColor(post: PostModel, uploadUiState: PostUploadUiState): Int? {
        return getLabelColor(
                fromPost(post),
                post.isLocalDraft,
                post.isLocallyChanged,
                uploadUiState,
                pageConflictResolver.doesPostHaveUnhandledConflict(post),
                pageConflictResolver.hasUnhandledAutoSave(post)
        )
    }

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    @ColorRes private fun getLabelColor(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean,
        hasAutoSave: Boolean
    ): Int? {
        // TODO consider removing this logic and explicitly list which labels have which color
        val isError = (uploadUiState is UploadFailed && !uploadUiState.isEligibleForAutoUpload) ||
                hasUnhandledConflicts
        val isProgressInfo = uploadUiState is UploadingPost || uploadUiState is UploadingMedia ||
                uploadUiState is UploadQueued
        val isStateInfo = (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload) ||
                isLocalDraft || isLocallyChanged || postStatus == PRIVATE || postStatus == PENDING ||
                uploadUiState is UploadWaitingForConnection || hasAutoSave

        return when {
            isError -> ERROR_COLOR
            isProgressInfo -> PROGRESS_INFO_COLOR
            isStateInfo -> STATE_INFO_COLOR
            else -> null
        }
    }
}
