package org.wordpress.android.viewmodel.pages

import org.wordpress.android.ui.pages.PageItem.Action
import org.wordpress.android.ui.pages.PageItem.Action.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PageItem.Action.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PageItem.Action.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PageItem.Action.PUBLISH_NOW
import org.wordpress.android.ui.pages.PageItem.Action.SET_PARENT
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import javax.inject.Inject

class CreatePageListItemActionsUseCase @Inject constructor() {
    fun setupPageActions(listType: PageListType, uploadUiState: PostUploadUiState): Set<Action> {
        return when (listType) {
            SCHEDULED, PUBLISHED -> mutableSetOf(
                    VIEW_PAGE,
                    SET_PARENT,
                    MOVE_TO_DRAFT,
                    MOVE_TO_TRASH
            ).apply {
                if (canCancelPendingAutoUpload(uploadUiState)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }
            DRAFTS -> mutableSetOf(VIEW_PAGE, SET_PARENT, PUBLISH_NOW, MOVE_TO_TRASH).apply {
                if (canCancelPendingAutoUpload(uploadUiState)) {
                    add(CANCEL_AUTO_UPLOAD)
                }
            }
            TRASHED -> setOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY)
        }
    }

    private fun canCancelPendingAutoUpload(uploadUiState: PostUploadUiState) =
            (uploadUiState is UploadWaitingForConnection ||
                    (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload))
}
