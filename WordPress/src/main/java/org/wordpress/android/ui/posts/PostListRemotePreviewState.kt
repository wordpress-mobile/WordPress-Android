package org.wordpress.android.ui.posts

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString.UiStringRes

/**
 * This enum class is used to keep the current Remote Preview state.
 * It also contains the [progressDialogUiState] that can be used to update the ProgressDialog
 * managed by this state.
 */
enum class PostListRemotePreviewState(val value: Int, val progressDialogUiState: ProgressDialogUiState) {
    NONE(0, ProgressDialogUiState.HiddenProgressDialog),
    UPLOADING_FOR_PREVIEW(
        1, ProgressDialogUiState.VisibleProgressDialog(
            UiStringRes(R.string.post_preview_saving_draft),
            cancelable = false,
            indeterminate = true
        )
    ),
    REMOTE_AUTO_SAVING_FOR_PREVIEW(
        2, ProgressDialogUiState.VisibleProgressDialog(
            UiStringRes(R.string.post_preview_remote_auto_saving_post),
            cancelable = false,
            indeterminate = true
        )
    ),
    PREVIEWING(3, ProgressDialogUiState.HiddenProgressDialog),
    REMOTE_AUTO_SAVE_PREVIEW_ERROR(4, ProgressDialogUiState.HiddenProgressDialog);

    companion object {
        fun fromInt(value: Int): PostListRemotePreviewState =
            PostListRemotePreviewState.values().firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("PostListRemotePreviewState wrong value $value")
    }
}
