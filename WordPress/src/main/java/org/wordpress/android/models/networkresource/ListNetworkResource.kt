package org.wordpress.android.models.networkresource

@Suppress("unused")
sealed class ListNetworkResource<T>(val data: List<T>) {
    class Init<T> : ListNetworkResource<T>(ArrayList())
    class Ready<T>(data: List<T>) : ListNetworkResource<T>(data)
    class Success<T>(data: List<T>, val canLoadMore: Boolean = false) : ListNetworkResource<T>(data)
    class Loading<T>(previous: ListNetworkResource<T>, val loadingMore: Boolean = false)
        : ListNetworkResource<T>(previous.data)

    class Error<T>(previous: ListNetworkResource<T>, val errorMessage: String?, val wasLoadingMore: Boolean = false)
        : ListNetworkResource<T>(previous.data)

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Loading -> false // Already fetching
            is Success -> if (loadMore) canLoadMore else true // Trying to load more or refreshing
            else -> !loadMore // First page can be fetched since we are not fetching anything else
        }
    }

    fun isFetchingFirstPage(): Boolean = if (this is Loading) !loadingMore else false
    fun isLoadingMore(): Boolean = (this as? Loading)?.loadingMore == true
}
