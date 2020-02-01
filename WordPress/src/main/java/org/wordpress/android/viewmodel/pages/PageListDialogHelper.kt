package org.wordpress.android.viewmodel.pages

import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.UNPUBLISHED_REVISION_DIALOG_SHOWN
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.helpers.DialogHolder

private const val CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG = "CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG"

class PageListDialogHelper(
    private val showDialog: (DialogHolder) -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val analyticsTracker: AnalyticsTrackerWrapper
) {
    private var localPostIdForAutosaveRevisionResolutionDialog: Int? = null

    fun showAutoSaveRevisionDialog(post: PostModel) {
        analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_SHOWN)
        val dialogHolder = DialogHolder(
                tag = CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG,
                title = UiStringRes(string.dialog_confirm_autosave_title),
                message = PostUtils.getCustomStringForAutosaveRevisionDialog(post),
                positiveButton = UiStringRes(string.dialog_confirm_autosave_restore_button),
                negativeButton = UiStringRes(string.dialog_confirm_autosave_dont_restore_button)
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
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPostIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the restored auto save
                localPostIdForAutosaveRevisionResolutionDialog = null
                editRestoredAutoSavePost(it)
                analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED)
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
            CONFIRM_ON_AUTOSAVE_REVISION_DIALOG_TAG -> localPostIdForAutosaveRevisionResolutionDialog?.let {
                // open the editor with the local post (don't use the auto save version)
                editLocalPost(it)
                analyticsTracker.track(UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED)
            }
            else -> throw IllegalArgumentException("Dialog's negative button click is not handled: $instanceTag")
        }
    }
}
