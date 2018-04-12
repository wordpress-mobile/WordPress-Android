package org.wordpress.android.models.networkresource

import org.wordpress.android.util.AppLog

sealed class ListNetworkResource<T : Any>(val data: List<T>) {
    class Init<T : Any> : ListNetworkResource<T>(ArrayList()) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = this
    }

    class Ready<T : Any>(data: List<T>) : ListNetworkResource<T>(data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = Ready(transform(data))
    }

    class Success<T : Any>(data: List<T>, val canLoadMore: Boolean = false) : ListNetworkResource<T>(data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Success(transform(data), canLoadMore)
    }

    class Loading<T : Any> private constructor(data: List<T>, val loadingMore: Boolean) : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, loadingMore: Boolean = false): this(previous.data, loadingMore)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Loading(transform(data), loadingMore)
    }

    class Error<T : Any> private constructor(data: List<T>, val errorMessage: String?, val wasLoadingMore: Boolean)
        : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, errorMessage: String?, wasLoadingMore: Boolean = false)
                : this(previous.data, errorMessage, wasLoadingMore)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Error(transform(data), errorMessage, wasLoadingMore)
    }

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Init -> { // Not ready yet
                // TODO: Don't use T.MAIN for the log
                AppLog.e(AppLog.T.MAIN, "ListNetworkResource should be ready before fetching")
                false
            }
            is Loading -> false // Already fetching
            is Success -> if (loadMore) canLoadMore else true // Trying to load more or refreshing
            else -> !loadMore // First page can be fetched since we are not fetching anything else
        }
    }

    fun isFetchingFirstPage(): Boolean = if (this is Loading) !loadingMore else false

    fun isLoadingMore(): Boolean = (this as? Loading)?.loadingMore == true

    abstract fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>): ListNetworkResource<T>
}
