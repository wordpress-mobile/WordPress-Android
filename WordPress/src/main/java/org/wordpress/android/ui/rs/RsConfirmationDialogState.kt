package org.wordpress.android.ui.rs

/**
 * The confirmation an rs screen is waiting on, with the callbacks that resolve it.
 *
 * [T] is each screen's own sealed type of pending action. Those stay separate: the pages list's
 * Delete carries the page title because its prompt interpolates it, and the comments list confirms
 * batches rather than single rows, so one merged type would leave every screen with a `when`
 * branch it can never reach.
 */
data class RsConfirmationDialogState<T>(
    val pending: T? = null,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)
