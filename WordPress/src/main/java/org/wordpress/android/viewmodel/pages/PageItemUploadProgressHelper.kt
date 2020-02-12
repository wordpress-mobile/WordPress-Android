package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import javax.inject.Inject

typealias ShouldShowOverlay = Boolean

class PageItemUploadProgressHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper
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

    private fun shouldShowOverlay(uploadUiState: PostUploadUiState): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return (uploadUiState is UploadingPost ||
                (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }
}
