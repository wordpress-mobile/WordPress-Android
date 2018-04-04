package org.wordpress.android.models.networkresource

sealed class NetworkResource<T> {
    open val data: T? = null

    class Init<T> : NetworkResource<T>()
    class Ready<T>(override val data: T) : NetworkResource<T>()
    class Error<T>(previous: NetworkResource<T>, val errorMessage: String?, val wasLoadingMore: Boolean = false)
        : NetworkResource<T>() {
        override val data = previous.data
    }

    class Loading<T>(previous: NetworkResource<T>, val loadingMore: Boolean = false) : NetworkResource<T>() {
        override val data = previous.data
    }

    class Success<T>(override val data: T, val canLoadMore: Boolean = false) : NetworkResource<T>()

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Loading -> false // Already fetching
            is Success -> loadMore && canLoadMore // Trying to load more and it's possible to do so
            else -> !loadMore // First page can be fetched since we are not fetching anything else
        }
    }

    fun isFetching(loadMore: Boolean = false): Boolean {
        return when (this) {
            is Loading -> if (loadMore) loadingMore else !loadingMore
            else -> false
        }
    }
}
