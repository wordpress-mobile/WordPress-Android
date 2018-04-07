package org.wordpress.android.models.networkresource

import org.wordpress.android.util.AppLog

sealed class ListNetworkResource<T>(val data: List<T>) {
    fun ready(data: List<T>): ListNetworkResource<T> =  Ready(this, data)

    fun success(data: List<T>, canLoadMore: Boolean = false) = Success(this, data, canLoadMore)

    fun loading(loadingMore: Boolean) = Loading(this, loadingMore)

    fun error(errorMessage: String?) = Error(this, errorMessage)

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

    class Init<T> : ListNetworkResource<T>(ArrayList()) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = this
    }

    class Ready<T>(previous: ListNetworkResource<T>, data: List<T>) : ListNetworkResource<T>(data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = Ready(this, transform(data))
    }

    class Success<T>(previous: ListNetworkResource<T>, data: List<T>, val canLoadMore: Boolean = false)
        : ListNetworkResource<T>(data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Success(this, transform(data), canLoadMore)
    }

    class Loading<T> private constructor(previous: ListNetworkResource<T>,
                                               data: List<T>,
                                               val loadingMore: Boolean) : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, loadingMore: Boolean = false)
                : this(previous, previous.data, loadingMore)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Loading(this, transform(data), loadingMore)
    }

    class Error<T> private constructor(previous: ListNetworkResource<T>, data: List<T>, val errorMessage: String?)
        : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, errorMessage: String?)
                : this(previous, previous.data, errorMessage)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Error(this, transform(data), errorMessage)
    }
}
