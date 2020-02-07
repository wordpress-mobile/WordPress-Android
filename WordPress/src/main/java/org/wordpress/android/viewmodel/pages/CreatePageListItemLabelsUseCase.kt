package org.wordpress.android.viewmodel.pages

import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.POSTS
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.CreatePageUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

class CreatePageListItemLabelsUseCase @Inject constructor() {
    fun createLabels(pagePostModel: PostModel, uploadUiState: PostUploadUiState): List<UiString> {
        return getLabels(
                PostStatus.fromPost(pagePostModel),
                pagePostModel.isLocalDraft,
                pagePostModel.isLocallyChanged,
                uploadUiState,
                false, // TODO use conflict resolver
                false // TODO use conflict resolver
        )
    }

    private fun getLabels(
        postStatus: PostStatus,
        isLocalDraft: Boolean,
        isLocallyChanged: Boolean,
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean,
        hasAutoSave: Boolean
    ): List<UiString> {
        val labels: MutableList<UiString> = ArrayList()
        when {
            uploadUiState is PostUploadUiState.UploadFailed -> {
                getErrorLabel(uploadUiState, postStatus)?.let { labels.add(it) }
            }
            uploadUiState is UploadingPost -> if (uploadUiState.isDraft) {
                labels.add(UiStringRes(R.string.post_uploading_draft))
            } else {
                labels.add(UiStringRes(R.string.post_uploading))
            }
            uploadUiState is UploadingMedia -> labels.add(UiStringRes(R.string.uploading_media))
            uploadUiState is UploadQueued -> labels.add(UiStringRes(R.string.post_queued))
            uploadUiState is UploadWaitingForConnection -> {
                when (uploadUiState.postStatus) {
                    UNKNOWN, PUBLISHED -> labels.add(UiStringRes(R.string.post_waiting_for_connection_publish))
                    PRIVATE -> labels.add(UiStringRes(R.string.post_waiting_for_connection_private))
                    PENDING -> labels.add(UiStringRes(R.string.post_waiting_for_connection_pending))
                    SCHEDULED -> labels.add(UiStringRes(R.string.post_waiting_for_connection_scheduled))
                    DRAFT -> labels.add(UiStringRes(R.string.post_waiting_for_connection_draft))
                    TRASHED -> AppLog.e(
                            POSTS,
                            "Developer error: This state shouldn't happen. Trashed post is in " +
                                    "UploadWaitingForConnection state."
                    )
                }
            }
            hasUnhandledConflicts -> labels.add(UiStringRes(R.string.local_post_is_conflicted))
            hasAutoSave -> labels.add(UiStringRes(R.string.local_post_autosave_revision_available))
        }

        // we want to show either single error/progress label or 0-n info labels.
        if (labels.isEmpty()) {
            if (isLocalDraft) {
                labels.add(UiStringRes(R.string.local_draft))
            } else if (isLocallyChanged) {
                labels.add(UiStringRes(R.string.local_changes))
            }
            if (postStatus == PRIVATE) {
                labels.add(UiStringRes(R.string.post_status_post_private))
            }
            if (postStatus == PENDING) {
                labels.add(UiStringRes(R.string.post_status_pending_review))
            }
        }
        return labels
    }

    private fun getErrorLabel(uploadUiState: UploadFailed, postStatus: PostStatus): UiString? {
        return when {
            uploadUiState.error.mediaError != null -> getMediaUploadErrorLabel(
                    uploadUiState,
                    postStatus
            )
            uploadUiState.error.postError != null -> UploadUtils.getErrorMessageResIdFromPostError(
                    postStatus,
                    false,
                    uploadUiState.error.postError,
                    uploadUiState.isEligibleForAutoUpload
            )
            else -> {
                val errorMsg = "MediaError and postError are both null."
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(errorMsg)
                } else {
                    AppLog.e(POSTS, errorMsg)
                }
                UiStringRes(R.string.error_generic)
            }
        }
    }

    private fun getMediaUploadErrorLabel(
        uploadUiState: UploadFailed,
        postStatus: PostStatus
    ): UiStringRes {
        return when {
            uploadUiState.isEligibleForAutoUpload -> when (postStatus) {
                PUBLISHED -> UiStringRes(R.string.error_media_recover_post_not_published_retrying)
                PRIVATE -> UiStringRes(R.string.error_media_recover_post_not_published_retrying_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_post_not_scheduled_retrying)
                PENDING -> UiStringRes(R.string.error_media_recover_post_not_submitted_retrying)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_generic_error_retrying)
            }
            uploadUiState.retryWillPushChanges -> when (postStatus) {
                PUBLISHED -> UiStringRes(R.string.error_media_recover_post_not_published)
                PRIVATE -> UiStringRes(R.string.error_media_recover_post_not_published_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_post_not_scheduled)
                PENDING -> UiStringRes(R.string.error_media_recover_post_not_submitted)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_media_recover_post)
            }
            else -> UiStringRes(R.string.error_media_recover_post)
        }
    }
}
