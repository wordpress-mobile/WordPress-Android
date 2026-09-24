package org.wordpress.android.ui.rs

/** A snackbar the rs screens show, with an optional action. Strings are resolved in the view model. */
data class RsSnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)
