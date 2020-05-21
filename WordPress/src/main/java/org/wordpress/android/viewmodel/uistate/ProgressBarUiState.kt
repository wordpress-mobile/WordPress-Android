package org.wordpress.android.viewmodel.uistate

sealed class ProgressBarUiState(val visibility: Boolean) {
    object Hidden : ProgressBarUiState(visibility = false)
    object Indeterminate : ProgressBarUiState(visibility = true)
    data class Determinate(val progress: Int) : ProgressBarUiState(visibility = true)
}
