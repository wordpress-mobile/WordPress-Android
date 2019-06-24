package org.wordpress.android.ui.posts

import org.wordpress.android.ui.utils.UiString

sealed class ProgressDialogUiState {
    object HiddenProgressDialog : ProgressDialogUiState()

    data class VisibleProgressDialog(
        val messageString: UiString,
        val cancelable: Boolean,
        val indeterminate: Boolean
    ) : ProgressDialogUiState()

    object IgnoreProgressDialog : ProgressDialogUiState()
}
