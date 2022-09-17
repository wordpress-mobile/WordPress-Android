package org.wordpress.android.ui

import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel.WebPreviewUiState.WebPreviewFullscreenUiState

/**
 * This enum could be expanded to allow to re-use the WPWebViewActivity (including the direct usage of actionable empty
 * view) in other scenarios with different WebPreviewUiState, also menu can be customized with the same principle.
 */
enum class WPWebViewUsageCategory constructor(val value: Int, val menuUiState: WPWebViewMenuUiState) {
    WEBVIEW_STANDARD(0, WPWebViewMenuUiState()),
    REMOTE_PREVIEW_NOT_AVAILABLE(1, WPWebViewMenuUiState(
            browserMenuVisible = false,
            shareMenuVisible = false,
            refreshMenuVisible = false
    )),
    REMOTE_PREVIEW_NO_NETWORK(2, WPWebViewMenuUiState(
            browserMenuVisible = false,
            shareMenuVisible = false,
            refreshMenuVisible = false
    )),
    REMOTE_PREVIEWING(3, WPWebViewMenuUiState(
            browserMenuVisible = false,
            shareMenuVisible = false,
            refreshMenuVisible = true,
            refreshMenuShowAsAction = true
    ));

    companion object {
        @JvmStatic
        fun fromInt(value: Int): WPWebViewUsageCategory =
                WPWebViewUsageCategory.values().firstOrNull { it.value == value }
                        ?: throw IllegalArgumentException("WebViewUsageCategory wrong value $value")

        @JvmStatic
        fun isActionableDirectUsage(state: WPWebViewUsageCategory) =
            state == WPWebViewUsageCategory.REMOTE_PREVIEW_NOT_AVAILABLE ||
                    state == WPWebViewUsageCategory.REMOTE_PREVIEW_NO_NETWORK

        @JvmStatic
        fun actionableDirectUsageToWebPreviewUiState(
            state: WPWebViewUsageCategory
        ): WPWebViewViewModel.WebPreviewUiState {
            return when (state) {
                REMOTE_PREVIEW_NOT_AVAILABLE -> WebPreviewFullscreenUiState.WebPreviewFullscreenNotAvailableUiState
                REMOTE_PREVIEW_NO_NETWORK -> WebPreviewFullscreenUiState.WebPreviewFullscreenErrorUiState(
                        buttonVisibility = false
                )
                WEBVIEW_STANDARD, REMOTE_PREVIEWING ->
                    throw IllegalArgumentException("Mapping of $state to WebPreviewUiState not allowed.")
            }
        }
    }
}
