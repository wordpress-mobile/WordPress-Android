package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_SHOWN
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostResolutionConfirmationType
import org.wordpress.android.ui.posts.PostResolutionOverlayActionEvent
import org.wordpress.android.ui.posts.PostResolutionType
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG = "CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG"
private const val CONFIRM_DELETE_PAGE_DIALOG_TAG = "CONFIRM_DELETE_PAGE_DIALOG_TAG"
private const val CONFIRM_COPY_CONFLICT_DIALOG_TAG = "CONFIRM_COPY_CONFLICT_DIALOG_TAG"
private const val POST_TYPE = "post_type"

class PageListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val showConflictResolutionOverlay: ((PostResolutionOverlayActionEvent.ShowDialogAction) -> Unit)? = null,
    private val isPostConflictResolutionEnabled: Boolean
) {
    private var pageIdForConflictResolutionDialog: Int? = null
    private var pageIdForAutosaveRevisionResolutionDialog: RemoteId? = null
    private var pageIdForDeleteDialog: RemoteId? = null
    private var pageIdForCopyDialog: RemoteId? = null

    fun showAutoSaveRevisionDialog(page: PostModel) {
        pageIdForAutosaveRevisionResolutionDialog = RemoteId(page.remotePostId)
        if (isPostConflictResolutionEnabled) {
            showConflictResolutionOverlay?.invoke(
                PostResolutionOverlayActionEvent.ShowDialogAction(
                    page, PostResolutionType.AUTOSAVE_REVISION_CONFLICT
                )
            )
        } else {
            analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_SHOWN, mapOf(POST_TYPE to "page"))
            val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_autosave_title),
                message = PostUtils.getCustomStringForAutosaveRevisionDialog(page),
                positiveButton = UiStringRes(R.string.dialog_confirm_autosave_restore_button),
                negativeButton = UiStringRes(R.string.dialog_confirm_autosave_dont_restore_button)
            )
            showDialog.invoke(dialogHolder)
        }
    }

    fun showConflictedPostResolutionDialog(page: PostModel) {
        pageIdForConflictResolutionDialog = page.id
        showConflictResolutionOverlay?.invoke(
            PostResolutionOverlayActionEvent.ShowDialogAction(
                page,
                PostResolutionType.SYNC_CONFLICT
            )
        )
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

    fun showCopyConflictDialog(page: PostModel) {
        val dialogHolder = DialogHolder(
            tag = CONFIRM_COPY_CONFLICT_DIALOG_TAG,
            title = UiStringRes(R.string.dialog_confirm_copy_local_title),
            message = UiStringRes(R.string.dialog_confirm_copy_local_message),
            positiveButton = UiStringRes(R.string.dialog_confirm_copy_local_edit_button),
            negativeButton = UiStringRes(R.string.dialog_confirm_copy_local_copy_button)
        )
        pageIdForCopyDialog = RemoteId(page.remotePostId)
        showDialog.invoke(dialogHolder)
    }

    fun onPositiveClickedForBasicDialog(
        instanceTag: String,
        deletePage: (RemoteId) -> Unit,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit,
        editPageFirst: (RemoteId) -> Unit
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
            CONFIRM_COPY_CONFLICT_DIALOG_TAG -> pageIdForCopyDialog?.let {
                pageIdForCopyDialog = null
                editPageFirst(it)
            } ?: throw NullPointerException("pageIdForCopyDialog shouldn't be null.")
            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(
        instanceTag: String,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit,
        copyPage: (RemoteId) -> Unit
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
            CONFIRM_COPY_CONFLICT_DIALOG_TAG -> pageIdForCopyDialog?.let {
                pageIdForCopyDialog = null
                copyPage(it)
            } ?: throw NullPointerException("pageIdForCopyDialog shouldn't be null.")
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }

    fun onPostResolutionConfirmed(
        event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit,
        updateConflictedPostWithRemoteVersion: (Int) -> Unit,
        updateConflictedPostWithLocalVersion: (Int) -> Unit
    ) {
        when (event.postResolutionType) {
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT -> {
                handleAutosaveRevisionConflict(event, editPage)
            }

            PostResolutionType.SYNC_CONFLICT -> {
                handleSyncRevisionConflict(
                    event,
                    updateConflictedPostWithLocalVersion,
                    updateConflictedPostWithRemoteVersion
                )
            }
        }
    }

    private fun handleAutosaveRevisionConflict(
        event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent,
        editPage: (RemoteId, LoadAutoSaveRevision) -> Unit
    ) {
        when (event.postResolutionConfirmationType) {
            PostResolutionConfirmationType.CONFIRM_LOCAL -> {
                pageIdForAutosaveRevisionResolutionDialog?.let {
                    // open the editor with the local page (don't use the auto save version)
                    editPage(it, false)
                }
            }

            PostResolutionConfirmationType.CONFIRM_OTHER -> {
                pageIdForAutosaveRevisionResolutionDialog?.let {
                    // open the editor with the restored auto save
                    pageIdForAutosaveRevisionResolutionDialog = null
                    editPage(it, true)
                }
            }
        }
    }

    private fun handleSyncRevisionConflict(
        event: PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent,
        updateConflictedPostWithLocalVersion: (Int) -> Unit,
        updateConflictedPostWithRemoteVersion: (Int) -> Unit
    ) {
        when (event.postResolutionConfirmationType) {
            PostResolutionConfirmationType.CONFIRM_LOCAL -> {
                pageIdForConflictResolutionDialog?.let {
                    // load version from local
                    updateConflictedPostWithLocalVersion(it)
                }
            }

            PostResolutionConfirmationType.CONFIRM_OTHER -> {
                pageIdForConflictResolutionDialog?.let {
                    pageIdForConflictResolutionDialog = null
                    // load version from remote
                    updateConflictedPostWithRemoteVersion(it)
                }
            }
        }
    }
}
