package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_SHOWN
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.DialogHolder
import java.lang.NullPointerException

private const val CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG = "CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG"
private const val CONFIRM_DELETE_PAGE_DIALOG_TAG = "CONFIRM_DELETE_PAGE_DIALOG_TAG"
private const val POST_TYPE = "post_type"

class PageListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    private var pageIdForAutosaveRevisionResolutionDialog: RemoteId? = null
    private var pageIdForDeleteDialog: RemoteId? = null

    fun showAutoSaveRevisionDialog(page: PostModel) {
        analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_SHOWN, mapOf(POST_TYPE to "page"))
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_autosave_title),
                message = PostUtils.getCustomStringForAutosaveRevisionDialog(page),
                positiveButton = UiStringRes(R.string.dialog_confirm_autosave_restore_button),
                negativeButton = UiStringRes(R.string.dialog_confirm_autosave_dont_restore_button)
        )
        pageIdForAutosaveRevisionResolutionDialog = RemoteId(page.remotePostId)
        showDialog.invoke(dialogHolder)
    }

    fun showDeletePageConfirmationDialog(pageId: RemoteId, pageTitle: String) {
        val dialogHolder = DialogHolder(
                tag = CONFIRM_DELETE_PAGE_DIALOG_TAG,
                title = UiStringRes(R.string.delete_page),
                message = UiStringResWithParams(
                        R.string.page_delete_dialog_message,
                        listOf(UiStringText(pageTitle))
                ),
                positiveButton = UiStringRes(R.string.delete),
                negativeButton = UiStringRes(R.string.cancel)
        )
        pageIdForDeleteDialog = pageId
        showDialog.invoke(dialogHolder)
    }

    fun onPositiveClickedForBasicDialog(
        instanceTag: String,
        deletePage: (RemoteId) -> Unit,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_PAGE_DIALOG_TAG -> pageIdForDeleteDialog?.let {
                pageIdForDeleteDialog = null
                deletePage(it)
            } ?: throw NullPointerException("pageIdForDeleteDialog shouldn't be null.")
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> pageIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the restored auto save
                pageIdForAutosaveRevisionResolutionDialog = null
                editPage(it, true)
                analyticsTracker.track(
                        UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED,
                        mapOf(POST_TYPE to "page")
                )
            }
                    ?: throw NullPointerException("pageIdForAutosaveRevisionResolutionDialog shouldn't be null.")

            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(
        instanceTag: String,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_PAGE_DIALOG_TAG -> pageIdForDeleteDialog = null
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> pageIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the local page (don't use the auto save version)
                editPage(it, false)
                analyticsTracker.track(
                        UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED,
                        mapOf(POST_TYPE to "page")
                )
            }
                    ?: throw NullPointerException("pageIdForAutosaveRevisionResolutionDialog shouldn't be null.")

            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }
}
