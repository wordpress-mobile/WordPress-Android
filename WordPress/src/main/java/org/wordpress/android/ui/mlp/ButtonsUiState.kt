package org.wordpress.android.ui.mlp

/**
 * The buttons visibility state
 */
data class ButtonsUiState(
    val createBlankPageVisible: Boolean = true,
    val createPageVisible: Boolean = false,
    val previewVisible: Boolean = false,
    val retryVisible: Boolean = false
)
