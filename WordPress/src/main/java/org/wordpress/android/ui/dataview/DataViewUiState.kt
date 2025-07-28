package org.wordpress.android.ui.dataview

import uniffi.wp_api.WpApiParamOrder

/**
 * Consolidated UI state for DataView components
 */
data class DataViewUiState(
    val loadingState: LoadingState = LoadingState.LOADING,
    val items: List<DataViewItem> = emptyList(),
    val currentFilter: DataViewDropdownItem? = null,
    val currentSortBy: DataViewDropdownItem? = null,
    val sortOrder: WpApiParamOrder = WpApiParamOrder.ASC,
    val errorMessage: String? = null,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val canLoadMore: Boolean = true,
    val currentPage: Int = 1
)

enum class LoadingState {
    LOADING,
    LOADING_MORE,
    LOADED,
    EMPTY,
    EMPTY_SEARCH,
    ERROR,
    OFFLINE
}
