package org.wordpress.android.models.networkresource

import org.wordpress.android.util.AppLog

sealed class ListNetworkResource<T : Any>(val data: List<T>) {
    class Init<T : Any> : ListNetworkResource<T>(ArrayList()) {
        override fun updatedListNetworkResource(map: (old: T) -> T?) = this
    }

    class Ready<T : Any>(data: List<T>) : ListNetworkResource<T>(data) {
        override fun updatedListNetworkResource(map: (old: T) -> T?) = Ready(mapNotNull(map))
    }

    class Success<T : Any>(data: List<T>, val canLoadMore: Boolean = false) : ListNetworkResource<T>(data) {
        override fun updatedListNetworkResource(map: (old: T) -> T?) = Success(mapNotNull(map), canLoadMore)
    }

    class Loading<T : Any> private constructor(data: List<T>, val loadingMore: Boolean) : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, loadingMore: Boolean = false): this(previous.data, loadingMore)

        override fun updatedListNetworkResource(map: (old: T) -> T?) = Loading(mapNotNull(map), loadingMore)
    }

    class Error<T : Any> private constructor(data: List<T>, val errorMessage: String?, val wasLoadingMore: Boolean)
        : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, errorMessage: String?, wasLoadingMore: Boolean = false)
                : this(previous.data, errorMessage, wasLoadingMore)

        override fun updatedListNetworkResource(map: (old: T) -> T?) =
                Error(mapNotNull(map), errorMessage, wasLoadingMore)
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

    abstract fun updatedListNetworkResource(map: (old: T) -> T?): ListNetworkResource<T>

    protected fun mapNotNull(map: (old: T) -> T?): List<T> {
        return data.mapNotNull { map(it) }
    }
}
