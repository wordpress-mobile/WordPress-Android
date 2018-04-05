package org.wordpress.android.models.networkresource

@Suppress("unused")
sealed class ListNetworkResource<T>(val data: List<T>) {
    class Init<T> : ListNetworkResource<T>(ArrayList()) {
        override fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T> {
            return this
        }
    }

    class Ready<T>(data: List<T>) : ListNetworkResource<T>(data) {
        override fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T> {
            return Ready(updatedData(newItem, compare))
        }
    }

    class Success<T>(data: List<T>, val canLoadMore: Boolean = false) : ListNetworkResource<T>(data) {
        override fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T> {
            return Success(updatedData(newItem, compare), canLoadMore)
        }
    }

    class Loading<T> private constructor(data: List<T>, val loadingMore: Boolean) : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, loadingMore: Boolean = false): this(previous.data, loadingMore)

        override fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T> {
            return Loading(updatedData(newItem, compare), loadingMore)
        }
    }

    class Error<T>private constructor(data: List<T>, val errorMessage: String?, val wasLoadingMore: Boolean)
        : ListNetworkResource<T>(data) {
        constructor(previous: ListNetworkResource<T>, errorMessage: String?, wasLoadingMore: Boolean = false)
                : this(previous.data, errorMessage, wasLoadingMore)
        override fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T> {
            return Error(updatedData(newItem, compare), errorMessage, wasLoadingMore)
        }
    }

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Loading -> false // Already fetching
            is Success -> if (loadMore) canLoadMore else true // Trying to load more or refreshing
            else -> !loadMore // First page can be fetched since we are not fetching anything else
        }
    }

    fun isFetchingFirstPage(): Boolean = if (this is Loading) !loadingMore else false

    fun isLoadingMore(): Boolean = (this as? Loading)?.loadingMore == true

    abstract fun updated(newItem: T, compare: (old: T, new: T) -> Boolean): ListNetworkResource<T>

    protected fun updatedData(newItem: T, compare: (old: T, new: T) -> Boolean): List<T> {
        return data.map { if (compare(it, newItem)) newItem else it }
    }
}
