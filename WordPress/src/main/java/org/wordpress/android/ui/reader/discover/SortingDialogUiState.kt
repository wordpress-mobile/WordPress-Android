package org.wordpress.android.ui.reader.discover

sealed class SortingDialogUiState(val isVisible: Boolean) {
    object DialogVisible : SortingDialogUiState(true)
    object DialogHidden : SortingDialogUiState(false)
}
