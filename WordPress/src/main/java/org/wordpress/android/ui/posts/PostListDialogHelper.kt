package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_SHOWN
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val CONFIRM_DELETE_POST_DIALOG_TAG = "CONFIRM_DELETE_POST_DIALOG_TAG"
private const val CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG = "CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG"
private const val CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG = "CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG"
private const val CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG = "CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG"
private const val CONFIRM_SYNC_SCHEDULED_POST_DIALOG_TAG = "CONFIRM_SYNC_SCHEDULED_POST_DIALOG_TAG"
private const val POST_TYPE = "post_type"

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    // Since we are using DialogFragments we need to hold onto which post will be published or trashed / resolved
    private var localPostIdForDeleteDialog: Int? = null
    private var localPostIdForTrashPostWithLocalChangesDialog: Int? = null
    private var localPostIdForConflictResolutionDialog: Int? = null
    private var localPostIdForAutosaveRevisionResolutionDialog: Int? = null
    private var localPostIdForScheduledPostSyncDialog: Int? = null

    fun showDeletePostConfirmationDialog(post: PostModel) {
        // We need network connection to delete a remote post, but not a local draft
        if (!post.isLocalDraft && !checkNetworkConnection.invoke()) {
            return
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_DELETE_POST_DIALOG_TAG,
                title = UiStringRes(R.string.delete_post),
                message = UiStringRes(R.string.dialog_confirm_delete_permanently_post),
                positiveButton = UiStringRes(R.string.delete),
                negativeButton = UiStringRes(R.string.cancel)
        )
        localPostIdForDeleteDialog = post.id
        showDialog.invoke(dialogHolder)
    }

    fun showSyncScheduledPostConfirmationDialog(post: PostModel) {
        if (localPostIdForScheduledPostSyncDialog != null) {
            // We can only handle one sync post dialog at once
            return
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_SYNC_SCHEDULED_POST_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_scheduled_post_sync_title),
                message = UiStringRes(R.string.dialog_confirm_scheduled_post_sync_message),
                positiveButton = UiStringRes(R.string.dialog_confirm_scheduled_post_sync_yes),
                negativeButton = UiStringRes(R.string.cancel)
        )
        localPostIdForScheduledPostSyncDialog = post.id
        showDialog.invoke(dialogHolder)
    }

    fun showTrashPostWithLocalChangesConfirmationDialog(post: PostModel) {
        if (!checkNetworkConnection.invoke()) {
            return
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_trash_losing_local_changes_title),
                message = UiStringRes(R.string.dialog_confirm_trash_losing_local_changes_message),
                positiveButton = UiStringRes(R.string.dialog_button_ok),
                negativeButton = UiStringRes(R.string.dialog_button_cancel)
        )
        localPostIdForTrashPostWithLocalChangesDialog = post.id
        showDialog.invoke(dialogHolder)
    }

    fun showConflictedPostResolutionDialog(post: PostModel) {
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_load_remote_post_title),
                message = UiStringText(PostUtils.getConflictedPostCustomStringForDialog(post)),
                positiveButton = UiStringRes(R.string.dialog_confirm_load_remote_post_discard_local),
                negativeButton = UiStringRes(R.string.dialog_confirm_load_remote_post_discard_web)
        )
        localPostIdForConflictResolutionDialog = post.id
        showDialog.invoke(dialogHolder)
    }

    fun showAutoSaveRevisionDialog(post: PostModel) {
        analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_SHOWN, mapOf(POST_TYPE to "post"))
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_autosave_title),
                message = PostUtils.getCustomStringForAutosaveRevisionDialog(post),
                positiveButton = UiStringRes(R.string.dialog_confirm_autosave_restore_button),
                negativeButton = UiStringRes(R.string.dialog_confirm_autosave_dont_restore_button)
        )
        localPostIdForAutosaveRevisionResolutionDialog = post.id
        showDialog.invoke(dialogHolder)
    }

    fun onPositiveClickedForBasicDialog(
        instanceTag: String,
        trashPostWithLocalChanges: (Int) -> Unit,
        deletePost: (Int) -> Unit,
        publishPost: (Int) -> Unit,
        updateConflictedPostWithRemoteVersion: (Int) -> Unit,
        editRestoredAutoSavePost: (Int) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForDeleteDialog?.let {
                localPostIdForDeleteDialog = null
                deletePost(it)
            }
            CONFIRM_SYNC_SCHEDULED_POST_DIALOG_TAG -> localPostIdForScheduledPostSyncDialog?.let {
                localPostIdForScheduledPostSyncDialog = null
                publishPost(it)
            }
            CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG -> localPostIdForConflictResolutionDialog?.let {
                localPostIdForConflictResolutionDialog = null
                // here load version from remote
                updateConflictedPostWithRemoteVersion(it)
            }
            CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG -> localPostIdForTrashPostWithLocalChangesDialog?.let {
                localPostIdForTrashPostWithLocalChangesDialog = null
                trashPostWithLocalChanges(it)
            }
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPostIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the restored auto save
                localPostIdForAutosaveRevisionResolutionDialog = null
                editRestoredAutoSavePost(it)
                analyticsTracker.track(
                        UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED,
                        mapOf(POST_TYPE to "post"))
            }
            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(
        instanceTag: String,
        updateConflictedPostWithLocalVersion: (Int) -> Unit,
        editLocalPost: (Int) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForDeleteDialog = null
            CONFIRM_SYNC_SCHEDULED_POST_DIALOG_TAG -> localPostIdForScheduledPostSyncDialog = null
            CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG -> localPostIdForTrashPostWithLocalChangesDialog = null
            CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG -> localPostIdForConflictResolutionDialog?.let {
                updateConflictedPostWithLocalVersion(it)
            }
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPostIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the local post (don't use the auto save version)
                editLocalPost(it)
                analyticsTracker.track(
                        UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED,
                        mapOf(POST_TYPE to "post")
                )
            }
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }

    fun onDismissByOutsideTouchForBasicDialog(
        instanceTag: String,
        updateConflictedPostWithLocalVersion: (Int) -> Unit,
        editLocalPost: (Int) -> Unit
    ) {
        // Cancel and outside touch dismiss works the same way for all, except for conflict and autosave revision
        // dialogs, for which tapping outside and actively tapping the "edit local" have different meanings
        if (instanceTag != CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG &&
                instanceTag != CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG) {
            onNegativeClickedForBasicDialog(
                    instanceTag = instanceTag,
                    updateConflictedPostWithLocalVersion = updateConflictedPostWithLocalVersion,
                    editLocalPost = editLocalPost
            )
        }
    }
}
