package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.pages.PagesListAction
import org.wordpress.android.ui.pages.PagesListAction.CANCEL_AUTO_UPLOAD
import org.wordpress.android.ui.pages.PagesListAction.COPY
import org.wordpress.android.ui.pages.PagesListAction.DELETE_PERMANENTLY
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_DRAFT
import org.wordpress.android.ui.pages.PagesListAction.MOVE_TO_TRASH
import org.wordpress.android.ui.pages.PagesListAction.PROMOTE_WITH_BLAZE
import org.wordpress.android.ui.pages.PagesListAction.PUBLISH_NOW
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_HOMEPAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_AS_POSTS_PAGE
import org.wordpress.android.ui.pages.PagesListAction.SET_PARENT
import org.wordpress.android.ui.pages.PagesListAction.SHARE
import org.wordpress.android.ui.pages.PagesListAction.VIEW_PAGE
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.DRAFTS
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.PUBLISHED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.SCHEDULED
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType.TRASHED
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadFailed
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState.UploadWaitingForConnection
import javax.inject.Inject

class CreatePageListItemActionsUseCase @Inject constructor() {
    @SuppressWarnings("ReturnCount")
    fun setupPageActions(
        listType: PageListType,
        uploadUiState: PostUploadUiState,
        siteModel: SiteModel,
        remoteId: Long,
        isPageEligibleForBlaze: Boolean = false,
        hasVersionConflict: Boolean = false
    ): List<PagesListAction> {
        return when (listType) {
            SCHEDULED -> return getScheduledPageActions(uploadUiState)
            PUBLISHED -> return getPublishedPageActions(
                siteModel,
                remoteId,
                listType,
                uploadUiState,
                isPageEligibleForBlaze,
                hasVersionConflict
            )

            DRAFTS -> getDraftsPageActions(uploadUiState)
            TRASHED -> mutableListOf(MOVE_TO_DRAFT, DELETE_PERMANENTLY).sortedWith(
                compareBy(
                    { it.actionGroup },
                    { it.positionInGroup })
            ).toList()
        }
    }

    private fun getScheduledPageActions(uploadUiState: PostUploadUiState): List<PagesListAction> {
        return mutableListOf(
            VIEW_PAGE,
            SET_PARENT,
            SHARE,
            MOVE_TO_DRAFT,
            MOVE_TO_TRASH
        ).apply {
            if (canCancelPendingAutoUpload(uploadUiState)) {
                add(CANCEL_AUTO_UPLOAD)
            }
        }.sortedWith(compareBy({ it.actionGroup }, { it.positionInGroup })).toList()
    }

    private fun canCancelPendingAutoUpload(uploadUiState: PostUploadUiState) =
        (uploadUiState is UploadWaitingForConnection ||
                (uploadUiState is UploadFailed && uploadUiState.isEligibleForAutoUpload))

    @Suppress("LongParameterList")
    private fun getPublishedPageActions(
        siteModel: SiteModel,
        remoteId: Long,
        listType: PageListType,
        uploadUiState: PostUploadUiState,
        isPageEligibleForBlaze: Boolean,
        hasVersionConflict: Boolean
    ): List<PagesListAction> {
        return mutableListOf(
            COPY,
            SHARE,
            SET_PARENT
        ).apply {
            if (!hasVersionConflict) add(VIEW_PAGE)
            if (siteModel.isUsingWpComRestApi &&
                siteModel.showOnFront == ShowOnFront.PAGE.value &&
                remoteId > 0
            ) {
                if (siteModel.pageOnFront != remoteId) {
                    add(SET_AS_HOMEPAGE)
                }
                if (siteModel.pageForPosts != remoteId) {
                    add(SET_AS_POSTS_PAGE)
                }
            }

            if (siteModel.pageOnFront != remoteId && listType == PUBLISHED) {
                add(MOVE_TO_DRAFT)
                add(MOVE_TO_TRASH)
            }

            if (canCancelPendingAutoUpload(uploadUiState)) {
                add(CANCEL_AUTO_UPLOAD)
            }

            if (isPageEligibleForBlaze) {
                add(PROMOTE_WITH_BLAZE)
            }
        }.sortedWith(compareBy({ it.actionGroup }, { it.positionInGroup })).toList()
    }

    private fun getDraftsPageActions(uploadUiState: PostUploadUiState): List<PagesListAction> {
        return mutableListOf(VIEW_PAGE, SET_PARENT, PUBLISH_NOW, MOVE_TO_TRASH, COPY, SHARE).apply {
            if (canCancelPendingAutoUpload(uploadUiState)) {
                add(CANCEL_AUTO_UPLOAD)
            }
        }.sortedWith(compareBy({ it.actionGroup }, { it.positionInGroup })).toList()
    }
}
