package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore.UploadError
import org.wordpress.android.ui.posts.PostListUploadStatusTracker
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

class PageItemProgressHelper @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val postStore: PostStore,
    val uploadStatusTracker: PostListUploadStatusTracker
) {
    private lateinit var site: SiteModel

    /**
     * This was done so that this class could be injectable for testing, as this dependency wouldn't be provided by DI
     */
    fun attachSite(site: SiteModel) {
        this.site = site
    }

    /**
     * Copied from PostListItemUiStateHelper since the behavior is similar for the Page List UI State.
     */
    private fun getProgressBarState(
        uploadUiState: PostUploadUiState
    ): PostListItemProgressBar {
        return if (shouldShowProgress(uploadUiState)) {
            if (uploadUiState is UploadingMedia) {
                PostListItemProgressBar.Determinate(uploadUiState.progress)
            } else {
                PostListItemProgressBar.Indeterminate
            }
        } else {
            PostListItemProgressBar.Hidden
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

    private fun shouldShowOverlay(uploadUiState: PostUploadUiState): Boolean {
        // show overlay when post upload is in progress or (media upload is in progress and the user is not using Aztec)
        return (uploadUiState is UploadingPost ||
                (!appPrefsWrapper.isAztecEditorEnabled && uploadUiState is UploadingMedia))
    }

    fun getProgressStateForPage(
        pageId: LocalId
    ): Pair<PostListItemProgressBar, ShouldShowOverlay> {
        val post = postStore.getPostByLocalPostId(pageId.value)
        val uploadStatus = uploadStatusTracker.getUploadStatus(
                post, site
        )
        val uploadUiState = createUploadUiState(uploadStatus, post)

        val shouldShowOverlay = shouldShowOverlay(uploadUiState)
        return Pair(getProgressBarState(uploadUiState), shouldShowOverlay)
    }
}
