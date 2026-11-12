package org.wordpress.android.viewmodel.posts

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
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

/**
 * The status labels a post row shows: a single upload error or progress label, otherwise "Local draft",
 * "Private" and the like. Shared by the legacy and rs post lists so both word upload states the same way;
 * [org.wordpress.android.viewmodel.pages.CreatePageListItemLabelsUseCase]
 * is the pages counterpart.
 */
class CreatePostListItemLabelsUseCase @Inject constructor() {
    private fun getErrorAndProgressStatuses(
        uploadUiState: PostUploadUiState,
        postStatus: PostStatus,
        hasUnhandledConflicts: Boolean,
        hasAutoSave: Boolean
    ): MutableList<UiString> {
        val labels: MutableList<UiString> = ArrayList()
        when {
            uploadUiState is UploadFailed -> {
                getErrorLabel(uploadUiState, postStatus).let { labels.add(it) }
            }

            uploadUiState is UploadingPost -> if (uploadUiState.isDraft) {
                labels.add(UiStringRes(R.string.post_uploading_draft))
            } else {
                labels.add(UiStringRes(R.string.post_uploading))
            }

            uploadUiState is UploadingMedia -> labels.add(UiStringRes(R.string.uploading_media))
            uploadUiState is UploadQueued -> labels.add(UiStringRes(R.string.post_queued))
            uploadUiState is UploadWaitingForConnection -> {
                getWaitingForConnectionStatus(uploadUiState.postStatus)?.let {
                    labels.add(it)
                }
            }

            hasUnhandledConflicts -> labels.add(UiStringRes(R.string.local_post_is_conflicted))
            hasAutoSave -> labels.add(UiStringRes(R.string.local_post_autosave_revision_available))
        }
        return labels
    }

    private fun getWaitingForConnectionStatus(postStatus: PostStatus): UiString? {
        return when (postStatus) {
            UNKNOWN, PUBLISHED -> (UiStringRes(R.string.post_waiting_for_connection_publish))
            PRIVATE -> (UiStringRes(R.string.post_waiting_for_connection_private))
            PENDING -> (UiStringRes(R.string.post_waiting_for_connection_pending))
            SCHEDULED -> (UiStringRes(R.string.post_waiting_for_connection_scheduled))
            DRAFT -> (UiStringRes(R.string.post_waiting_for_connection_draft))
            TRASHED -> {
                AppLog.e(
                    POSTS,
                    "Developer error: This state shouldn't happen. Trashed post is in " +
                            "UploadWaitingForConnection state."
                )
                return null
            }
        }
    }

    fun getStatuses(
        postStatus: PostStatus,
        post: PostModel,
        uploadUiState: PostUploadUiState,
        hasUnhandledConflicts: Boolean,
        hasAutoSave: Boolean
    ): List<UiString> {
        val labels = getErrorAndProgressStatuses(
            uploadUiState,
            postStatus,
            hasUnhandledConflicts,
            hasAutoSave
        )

        // we want to show either single error/progress label or 0-n info labels.
        if (labels.isEmpty()) {
            if (post.isLocalDraft) {
                labels.add(UiStringRes(R.string.local_draft))
            } else if (post.isLocallyChanged) {
                labels.add(UiStringRes(R.string.local_changes))
            }
            if (postStatus == PRIVATE) {
                labels.add(UiStringRes(R.string.post_status_post_private))
            }
            if (postStatus == PENDING) {
                labels.add(UiStringRes(R.string.post_status_pending_review))
            }
            if (post.sticky) {
                labels.add(UiStringRes(R.string.post_status_sticky))
            }
        }
        return labels
    }

    private fun getErrorLabel(uploadUiState: UploadFailed, postStatus: PostStatus): UiString {
        return when {
            uploadUiState.error.mediaError != null -> getMediaUploadErrorMessage(uploadUiState, postStatus)
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

    private fun getMediaUploadErrorMessage(uploadUiState: UploadFailed, postStatus: PostStatus): UiStringRes {
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
