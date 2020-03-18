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
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.PAGES
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadQueued
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingMedia
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadingPost
import javax.inject.Inject

typealias LabelColor = Int?
/**
 * Most of this code has been copied from PostListItemUIStateHelper.
 */
class CreatePageListItemLabelsUseCase @Inject constructor(
    private val autoSaveConflictResolver: AutoSaveConflictResolver,
    private val labelColorUseCase: PostPageListLabelColorUseCase,
    private val uploadUtilsWrapper: UploadUtilsWrapper
) {
    fun createLabels(postModel: PostModel, uploadUiState: PostUploadUiState): Pair<List<UiString>, LabelColor> {
        val hasUnhandledAutoSave = autoSaveConflictResolver.hasUnhandledAutoSave(postModel)
        val hasUnhandledConflicts = false // version conflicts aren't currently supported on page list
        val labels = getLabels(
                PostStatus.fromPost(postModel),
                postModel.isLocalDraft,
                postModel.isLocallyChanged,
                uploadUiState,
                hasUnhandledConflicts,
                hasUnhandledAutoSave
        )
        val labelColor = labelColorUseCase.getLabelsColor(
                postModel,
                uploadUiState,
                hasUnhandledConflicts,
                hasUnhandledAutoSave
        )
        return Pair(labels, labelColor)
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
            uploadUiState is UploadFailed -> {
                getErrorLabel(uploadUiState, postStatus)?.let { labels.add(it) }
            }
            uploadUiState is UploadingPost -> if (uploadUiState.isDraft) {
                labels.add(UiStringRes(R.string.page_uploading_draft))
            } else {
                labels.add(UiStringRes(R.string.page_uploading))
            }
            uploadUiState is UploadingMedia -> labels.add(UiStringRes(R.string.uploading_media))
            uploadUiState is UploadQueued -> labels.add(UiStringRes(R.string.page_queued))
            uploadUiState is UploadWaitingForConnection -> {
                when (uploadUiState.postStatus) {
                    UNKNOWN, PUBLISHED -> labels.add(UiStringRes(R.string.page_waiting_for_connection_publish))
                    PRIVATE -> labels.add(UiStringRes(R.string.page_waiting_for_connection_private))
                    PENDING -> labels.add(UiStringRes(R.string.page_waiting_for_connection_pending))
                    SCHEDULED -> labels.add(UiStringRes(R.string.page_waiting_for_connection_scheduled))
                    DRAFT -> labels.add(UiStringRes(R.string.page_waiting_for_connection_draft))
                    TRASHED -> AppLog.e(
                            PAGES,
                            "Developer error: This state shouldn't happen. Trashed pages is in " +
                                    "UploadWaitingForConnection state."
                    )
                }
            }
            hasUnhandledConflicts -> labels.add(UiStringRes(R.string.local_page_is_conflicted))
            hasAutoSave -> labels.add(UiStringRes(R.string.local_page_autosave_revision_available))
        }

        // we want to show either single error/progress label or 0-n info labels.
        if (labels.isEmpty()) {
            if (isLocalDraft) {
                labels.add(UiStringRes(R.string.page_local_draft))
            } else if (isLocallyChanged) {
                labels.add(UiStringRes(R.string.page_local_changes))
            }
            if (postStatus == PRIVATE) {
                labels.add(UiStringRes(R.string.page_status_page_private))
            }
            if (postStatus == PENDING) {
                labels.add(UiStringRes(R.string.page_status_pending_review))
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
            uploadUiState.error.postError != null -> uploadUtilsWrapper.getErrorMessageResIdFromPostError(
                    postStatus,
                    true,
                    uploadUiState.error.postError,
                    uploadUiState.isEligibleForAutoUpload
            )
            else -> {
                val errorMsg = "MediaError and postError are both null."
                if (BuildConfig.DEBUG) {
                    throw IllegalStateException(errorMsg)
                } else {
                    AppLog.e(PAGES, errorMsg)
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
                PUBLISHED -> UiStringRes(R.string.error_media_recover_page_not_published_retrying)
                PRIVATE -> UiStringRes(R.string.error_media_recover_page_not_published_retrying_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_page_not_scheduled_retrying)
                PENDING -> UiStringRes(R.string.error_media_recover_page_not_submitted_retrying)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_generic_error_retrying)
            }
            uploadUiState.retryWillPushChanges -> when (postStatus) {
                PUBLISHED -> UiStringRes(R.string.error_media_recover_page_not_published)
                PRIVATE -> UiStringRes(R.string.error_media_recover_page_not_published_private)
                SCHEDULED -> UiStringRes(R.string.error_media_recover_page_not_scheduled)
                PENDING -> UiStringRes(R.string.error_media_recover_page_not_submitted)
                DRAFT, TRASHED, UNKNOWN -> UiStringRes(R.string.error_media_recover_page)
            }
            else -> UiStringRes(R.string.error_media_recover_page)
        }
    }
}
