package org.wordpress.android.models

sealed class ListNetworkResourceState {
    enum class ErrorType {
        FETCH_ERROR,
        PAGINATION_ERROR
        // TODO: When we add the ability to handle connection errors in VMs, we should add `CONNECTION_ERROR` type
    }

    object Init : ListNetworkResourceState() // not setup or ready yet (for example site is null)
    object Ready : ListNetworkResourceState()
    data class Error(val type: ErrorType, val message: String?) : ListNetworkResourceState()
    data class Loading(val loadingMore: Boolean) : ListNetworkResourceState()
    data class Success(val canLoadMore: Boolean) : ListNetworkResourceState()

    fun isFetchingFirstPage(): Boolean = when (this) {
        is ListNetworkResourceState.Loading -> !loadingMore
        else -> false
    }

    fun isLoadingMore(): Boolean = when (this) {
        is ListNetworkResourceState.Loading -> loadingMore
        else -> false
    }

    // This is a temporary method since Java doesn't work well with sealed classes, once PluginBrowserActivity is
    // converted to Kotlin, we can get rid of it
    fun errorMessage(): String? = when (this) {
        is Error -> message
        else -> null
    }
}
