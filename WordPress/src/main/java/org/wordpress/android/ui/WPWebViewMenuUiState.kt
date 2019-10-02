package org.wordpress.android.ui

/**
 * This class is used to model the UI State of the Menu in the WPWebViewActivity
 * See [WPWebViewUsageCategory]
 */
data class WPWebViewMenuUiState(
    val browserMenuVisible: Boolean = true,
    val browserMenuShowAsAction: Boolean = true,
    val shareMenuVisible: Boolean = true,
    val shareMenuShowAsAction: Boolean = true,
    val refreshMenuVisible: Boolean = true,
    val refreshMenuShowAsAction: Boolean = false
)
