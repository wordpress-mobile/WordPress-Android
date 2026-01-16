@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.posts.editor

import android.app.ProgressDialog
import android.content.Context
import dagger.Reusable
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.ui.posts.ProgressDialogHelper
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.PostEvents.PostPreviewingInEditor
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AppLog
import javax.inject.Inject

/**
 * Manages post loading states and progress dialog visibility during editor operations.
 *
 * This class encapsulates the state machine for post loading states (preview, revision loading, etc.)
 * and coordinates progress dialog display. It extracts state management logic from EditPostActivity
 * to improve testability and separation of concerns.
 */
@Reusable
class PostLoadingStateManager @Inject constructor(
    private val progressDialogHelper: ProgressDialogHelper,
    private val uiHelpers: UiHelpers
) {
    /**
     * Listener interface for state changes that require Activity-level handling.
     */
    interface StateChangeListener {
        /**
         * Called when the state transitions and Activity needs to update its progress dialog.
         * @param newDialog The new progress dialog (may be null if dialog should be hidden).
         */
        fun onProgressDialogChanged(newDialog: ProgressDialog?)

        /**
         * Called when a preview should be launched.
         * @param previewType The type of remote preview to launch.
         */
        fun onLaunchPreview(previewType: RemotePreviewType)

        /**
         * Called when preview upload was successful and Activity state should be updated.
         */
        fun onPreviewUploadSuccess()

        /**
         * Called when preview upload failed and error should be shown.
         */
        fun onPreviewUploadError()
    }

    private var currentState: PostLoadingState = PostLoadingState.NONE
    private var listener: StateChangeListener? = null
    private var currentDialog: ProgressDialog? = null

    /**
     * The current post loading state.
     */
    val state: PostLoadingState
        get() = currentState

    /**
     * Returns true if the editor is in any remote previewing state.
     */
    val isRemotePreviewingFromEditor: Boolean
        get() = currentState == PostLoadingState.UPLOADING_FOR_PREVIEW ||
                currentState == PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW ||
                currentState == PostLoadingState.PREVIEWING ||
                currentState == PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR

    /**
     * Returns true if the post is currently being uploaded for preview.
     */
    val isUploadingPostForPreview: Boolean
        get() = currentState == PostLoadingState.UPLOADING_FOR_PREVIEW ||
                currentState == PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW

    /**
     * Returns true if a remote auto-save preview error occurred.
     */
    val isRemoteAutoSaveError: Boolean
        get() = currentState == PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR

    /**
     * Sets the listener for state change callbacks.
     */
    fun setListener(listener: StateChangeListener?) {
        this.listener = listener
    }

    /**
     * Transitions to a new state, updating the progress dialog as needed.
     *
     * @param context The context for creating progress dialogs.
     * @param newState The new state to transition to.
     * @param post The current post (used for sticky event posting).
     * @return true if the transition occurred, false if already in the requested state.
     */
    fun transitionTo(context: Context, newState: PostLoadingState, post: PostImmutableModel? = null): Boolean {
        // Only process actual transitions
        if (currentState == newState) return false

        AppLog.d(
            AppLog.T.POSTS,
            "Editor post loading state machine: transition from $currentState to $newState"
        )

        // Update the state
        currentState = newState

        // Handle exit/entry actions on state transition
        manageStateTransitions(post)

        // Update the progress dialog
        currentDialog = progressDialogHelper.updateProgressDialogState(
            context,
            currentDialog,
            currentState.progressDialogUiState,
            uiHelpers
        )
        listener?.onProgressDialogChanged(currentDialog)

        return true
    }

    /**
     * Handles the result of a remote preview upload operation.
     *
     * @param context The context for creating progress dialogs.
     * @param isError Whether the upload resulted in an error.
     * @param previewType The type of remote preview.
     * @param post The current post.
     * @return A [RemotePreviewResult] indicating what action should be taken.
     */
    fun handleRemotePreviewUploadResult(
        context: Context,
        isError: Boolean,
        previewType: RemotePreviewType,
        post: PostImmutableModel?
    ): RemotePreviewResult {
        return when {
            !isError && isUploadingPostForPreview -> {
                // Upload succeeded - launch preview
                listener?.onPreviewUploadSuccess()
                listener?.onLaunchPreview(previewType)
                transitionTo(context, PostLoadingState.PREVIEWING, post)
                RemotePreviewResult.PREVIEW_LAUNCHED
            }
            isError || isRemoteAutoSaveError -> {
                // Upload failed - show error
                transitionTo(context, PostLoadingState.NONE)
                listener?.onPreviewUploadError()
                RemotePreviewResult.ERROR
            }
            else -> {
                RemotePreviewResult.NO_ACTION
            }
        }
    }

    /**
     * Gets the state value for saving to instance state.
     */
    fun getStateValue(): Int = currentState.value

    /**
     * Restores the state from a saved instance state value.
     *
     * @param context The context for creating progress dialogs.
     * @param value The saved state value.
     */
    fun restoreState(context: Context, value: Int) {
        val restoredState = PostLoadingState.fromInt(value)
        if (restoredState != currentState) {
            currentState = restoredState
            currentDialog = progressDialogHelper.updateProgressDialogState(
                context,
                currentDialog,
                currentState.progressDialogUiState,
                uiHelpers
            )
            listener?.onProgressDialogChanged(currentDialog)
        }
    }

    /**
     * Clears the current state and dismisses any progress dialog.
     */
    fun clear() {
        currentState = PostLoadingState.NONE
        currentDialog?.dismiss()
        currentDialog = null
    }

    private fun manageStateTransitions(post: PostImmutableModel?) {
        when (currentState) {
            PostLoadingState.NONE -> setPreviewingInEditorSticky(false, post)
            PostLoadingState.UPLOADING_FOR_PREVIEW,
            PostLoadingState.REMOTE_AUTO_SAVING_FOR_PREVIEW,
            PostLoadingState.PREVIEWING,
            PostLoadingState.REMOTE_AUTO_SAVE_PREVIEW_ERROR -> setPreviewingInEditorSticky(true, post)
            PostLoadingState.LOADING_REVISION -> {
                // No action needed
            }
        }
    }

    private fun setPreviewingInEditorSticky(enable: Boolean, post: PostImmutableModel?) {
        if (enable) {
            post?.let {
                EventBus.getDefault().postSticky(
                    PostPreviewingInEditor(it.localSiteId, it.id)
                )
            }
        } else {
            EventBus.getDefault().getStickyEvent(PostPreviewingInEditor::class.java)?.let {
                EventBus.getDefault().removeStickyEvent(it)
            }
        }
    }

    /**
     * Result of handling a remote preview upload.
     */
    enum class RemotePreviewResult {
        /** Preview was launched successfully */
        PREVIEW_LAUNCHED,
        /** An error occurred and was reported */
        ERROR,
        /** No action was taken (state didn't require handling) */
        NO_ACTION
    }
}
