package org.wordpress.android.ui.rs

/**
 * A request to select [tab] and scroll to [remoteId] within it, raised when the editor saves
 * something the list may not be showing yet.
 */
data class RsReveal<TAB>(val tab: TAB, val remoteId: Long)
