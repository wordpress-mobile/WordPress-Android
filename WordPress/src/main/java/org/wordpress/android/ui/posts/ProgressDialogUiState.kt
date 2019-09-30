package org.wordpress.android.ui.posts

import org.wordpress.android.ui.utils.UiString

/**
 * This sealed class is used to hold the UI state of a Progress Dialog.
 *
 * HiddenProgressDialog: use this UI state to indicate the dialog must be dismissed
 * VisibleProgressDialog: use this UI state to indicate the dialog must be shown; this UI state holds
 * a messageString, a cancelable and indeterminate flags to personalize the dialog
 * IgnoreProgressDialog: use this UI state to indicate the state of the dialog should remain as it is
 */

sealed class ProgressDialogUiState {
    object HiddenProgressDialog : ProgressDialogUiState()

    data class VisibleProgressDialog(
        val messageString: UiString,
        val cancelable: Boolean,
        val indeterminate: Boolean
    ) : ProgressDialogUiState()

    object IgnoreProgressDialog : ProgressDialogUiState()
}
