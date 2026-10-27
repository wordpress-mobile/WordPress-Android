package org.wordpress.android.ui.rs

/** The confirmation an rs screen is waiting on; [T] is the screen's own pending-action type. */
data class RsConfirmationDialogState<T>(
    val pending: T? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)
