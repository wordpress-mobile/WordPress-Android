package org.wordpress.android.models.networkresource

sealed class NetworkResource<out T> {
    open val data: T? = null

    class Init<out T> : NetworkResource<T>()
    class Ready<out T>(override val data: T) : NetworkResource<T>()
    class Error<out T>(previous: NetworkResource<T>, val wasLoadingMore: Boolean, val errorMessage: String)
        : NetworkResource<T>() {
        override val data = previous.data
    }

    class Loading<out T>(previous: NetworkResource<T>, val loadingMore: Boolean) : NetworkResource<T>() {
        override val data = previous.data
    }

    class Success<out T>(override val data: T, val canLoadMore: Boolean) : NetworkResource<T>()

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Loading -> false // Already fetching
            is Success -> loadMore && canLoadMore // Trying to load more and it's possible to do so
            else -> !loadMore // First page can be fetched since we are not fetching anything else
        }
    }

    fun isFetchingFirstPage(): Boolean {
        return when (this) {
            is Loading -> !loadingMore
            else -> false
        }
    }

    fun isLoadingMore(): Boolean {
        return when (this) {
            is Loading -> loadingMore
            else -> false
        }
    }
}
