package org.wordpress.android.ui.posts.editor

import org.wordpress.android.R
import org.wordpress.android.ui.posts.ProgressDialogUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes

enum class PostLoadingState(
    val value: Int,
    val progressDialogUiState: ProgressDialogUiState
) {
    NONE(
        value = 0,
        progressDialogUiState = ProgressDialogUiState.HiddenProgressDialog
    ),
    LOADING_REVISION(
        value = 3,
        progressDialogUiState = ProgressDialogUiState.VisibleProgressDialog(
            UiStringRes(R.string.history_loading_revision),
            cancelable = false,
            indeterminate = true
        )
    ),
    UPLOADING_FOR_PREVIEW(
        value = 4,
        progressDialogUiState = ProgressDialogUiState.VisibleProgressDialog(
            UiStringRes(R.string.post_preview_saving_draft),
            cancelable = false,
            indeterminate = true
        )
    ),
    REMOTE_AUTO_SAVING_FOR_PREVIEW(
        value = 5,
        progressDialogUiState = ProgressDialogUiState.VisibleProgressDialog(
            UiStringRes(R.string.post_preview_remote_auto_saving_post),
            cancelable = false,
            indeterminate = true
        )
    ),
    PREVIEWING(
        value = 6,
        progressDialogUiState = ProgressDialogUiState.HiddenProgressDialog
    ),
    REMOTE_AUTO_SAVE_PREVIEW_ERROR(
        value = 7,
        progressDialogUiState = ProgressDialogUiState.HiddenProgressDialog
    );

    companion object {
        @JvmStatic
        fun fromInt(value: Int): PostLoadingState {
            return requireNotNull(
                values().firstOrNull { it.value == value },
                { "PostLoadingState wrong value $value" })
        }
    }
}
