package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val CONFIRM_DELETE_POST_DIALOG_TAG = "CONFIRM_DELETE_POST_DIALOG_TAG"
private const val CONFIRM_PUBLISH_POST_DIALOG_TAG = "CONFIRM_PUBLISH_POST_DIALOG_TAG"
private const val CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG = "CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG"
private const val CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG = "CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG"

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val checkNetworkConnection: () -> Boolean
) {
    // Since we are using DialogFragments we need to hold onto which post will be published or trashed / resolved
    private var localPostIdForDeleteDialog: Int? = null
    private var localPostIdForPublishDialog: Int? = null
    private var localPostIdForTrashPostWithLocalChangesDialog: Int? = null
    private var localPostIdForConflictResolutionDialog: Int? = null

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

    fun showPublishConfirmationDialog(post: PostModel) {
        if (localPostIdForPublishDialog != null) {
            // We can only handle one publish dialog at once
            return
        }
        if (!checkNetworkConnection.invoke()) {
            return
        }
        val dialogHolder = DialogHolder(
                tag = CONFIRM_PUBLISH_POST_DIALOG_TAG,
                title = UiStringRes(R.string.dialog_confirm_publish_title),
                message = UiStringRes(R.string.dialog_confirm_publish_message_post),
                positiveButton = UiStringRes(R.string.dialog_confirm_publish_yes),
                negativeButton = UiStringRes(R.string.cancel)
        )
        localPostIdForPublishDialog = post.id
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

    fun onPositiveClickedForBasicDialog(
        instanceTag: String,
        trashPostWithLocalChanges: (Int) -> Unit,
        deletePost: (Int) -> Unit,
        publishPost: (Int) -> Unit,
        updateConflictedPostWithRemoteVersion: (Int) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForDeleteDialog?.let {
                localPostIdForDeleteDialog = null
                deletePost(it)
            }
            CONFIRM_PUBLISH_POST_DIALOG_TAG -> localPostIdForPublishDialog?.let {
                localPostIdForPublishDialog = null
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
            else -> throw IllegalArgumentException("Dialog's positive button click is not handled: $instanceTag")
        }
    }

    fun onNegativeClickedForBasicDialog(
        instanceTag: String,
        updateConflictedPostWithLocalVersion: (Int) -> Unit
    ) {
        when (instanceTag) {
            CONFIRM_DELETE_POST_DIALOG_TAG -> localPostIdForDeleteDialog = null
            CONFIRM_PUBLISH_POST_DIALOG_TAG -> localPostIdForPublishDialog = null
            CONFIRM_TRASH_POST_WITH_LOCAL_CHANGES_DIALOG_TAG -> localPostIdForTrashPostWithLocalChangesDialog = null
            CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG -> localPostIdForConflictResolutionDialog?.let {
                updateConflictedPostWithLocalVersion(it)
            }
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }

    fun onDismissByOutsideTouchForBasicDialog(
        instanceTag: String,
        updateConflictedPostWithLocalVersion: (Int) -> Unit
    ) {
        // Cancel and outside touch dismiss works the same way for all, except for conflict resolution dialog,
        // for which tapping outside and actively tapping the "edit local" have different meanings
        if (instanceTag != CONFIRM_ON_CONFLICT_LOAD_REMOTE_POST_DIALOG_TAG) {
            onNegativeClickedForBasicDialog(
                    instanceTag = instanceTag,
                    updateConflictedPostWithLocalVersion = updateConflictedPostWithLocalVersion
            )
        }
    }
}
