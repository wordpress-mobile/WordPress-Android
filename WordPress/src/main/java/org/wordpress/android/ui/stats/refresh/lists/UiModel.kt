package org.wordpress.android.ui.stats.refresh.lists

import org.wordpress.android.R.string

sealed class UiModel<T> {
    data class Success<T>(val data: T) : UiModel<T>()
    class Error<T>(val message: Int = string.stats_loading_error) : UiModel<T>()
}