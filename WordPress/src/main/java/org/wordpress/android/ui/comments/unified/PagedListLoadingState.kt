package org.wordpress.android.ui.comments.unified

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState.Error
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Empty
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.EmptyError
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Idle
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.Refreshing

sealed class PagedListLoadingState {
    object Loading : PagedListLoadingState()
    object Refreshing : PagedListLoadingState()
    object Idle : PagedListLoadingState()
    object Empty : PagedListLoadingState()
    data class Error(val throwable: Throwable) : PagedListLoadingState()
    data class EmptyError(val throwable: Throwable) : PagedListLoadingState()
}

fun CombinedLoadStates.toPagedListLoadingState(hasContent: Boolean): PagedListLoadingState {
    val isLoading = refresh is Loading && !hasContent
    val isRefreshing = refresh is Loading && hasContent
    val isNothingToShow = refresh is NotLoading && append.endOfPaginationReached && !hasContent
    val isError = refresh is Error
    val isPagingError = append is Error

    return when {
        isLoading -> {
            PagedListLoadingState.Loading
        }
        isRefreshing -> {
            Refreshing
        }
        isNothingToShow -> {
            Empty
        }
        isError -> {
            if (!hasContent) {
                EmptyError((refresh as Error).error)
            } else {
                PagedListLoadingState.Error((refresh as Error).error)
            }
        }
        isPagingError -> {
            PagedListLoadingState.Error((append as Error).error)
        }
        else -> {
            Idle
        }
    }
}
