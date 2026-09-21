package org.wordpress.android.ui.rs

/**
 * A message the rs screens ask their host to show, with an optional action.
 *
 * Separate from FluxC-era [org.wordpress.android.ui.pages.SnackbarMessageHolder]: these screens
 * resolve their strings in the view model, so what crosses the boundary is text, not resource ids.
 */
data class RsSnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)
