package org.wordpress.android.ui.rs

/**
 * One tab of an rs list. Deliberately has no `isEmpty`: the pages list can hold a Site Editor row
 * with no pages, so it asks `hasRealPages` instead.
 */
data class RsTabUiState<T>(
    val items: List<T> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val isAuthError: Boolean = false
)
