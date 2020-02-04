package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_SHOWN
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG = "CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG"
private const val CONFIRM_DELETE_PAGE_DIALOG_TAG = "CONFIRM_DELETE_PAGE_DIALOG_TAG"


class PageListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    private var localPageIdForAutosaveRevisionResolutionDialog: LocalId? = null
    private var remotePageIdForDeleteDialog: RemoteId? = null

    fun showAutoSaveRevisionDialog(post: PostModel) {
        analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_SHOWN)
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_autosave_title),
                message = PostUtils.getCustomStringForAutosaveRevisionDialog(post, true),
                positiveButton = UiStringRes(R.string.dialog_confirm_autosave_restore_button),
                negativeButton = UiStringRes(R.string.dialog_confirm_autosave_dont_restore_button)
        )
        localPageIdForAutosaveRevisionResolutionDialog = LocalId(post.id)
        showDialog.invoke(dialogHolder)
    }

    fun showDeletePostConfirmationDialog(pageId : RemoteId) {
        val dialogHolder = DialogHolder(
                tag = CONFIRM_DELETE_PAGE_DIALOG_TAG,
                title = UiStringRes(R.string.delete_page),
                message = UiStringRes(R.string.page_delete_dialog_message),
                positiveButton = UiStringRes(R.string.delete),
                negativeButton = UiStringRes(R.string.cancel)
        )
        remotePageIdForDeleteDialog = pageId
        showDialog.invoke(dialogHolder)
    }

    fun onPositiveClickedForBasicDialog(
        instanceTag: String,
        deletePage: (RemoteId) -> Unit,
        editRestoredAutoSavePage: (LocalId) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_PAGE_DIALOG_TAG -> remotePageIdForDeleteDialog?.let {
                remotePageIdForDeleteDialog = null
                deletePage(it)
            }
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPageIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the restored auto save
                localPageIdForAutosaveRevisionResolutionDialog = null
                editRestoredAutoSavePage(it)
                analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED)
            }
            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(
        instanceTag: String,
        editLocalPage: (LocalId) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_PAGE_DIALOG_TAG -> remotePageIdForDeleteDialog = null
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPageIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the local post (don't use the auto save version)
                editLocalPage(it)
                analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED)
            }
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }
}
