package org.wordpress.android.models.networkresource

@Suppress("unused")
sealed class ListNetworkResource<T> {
    open val data: List<T>? = null

    class Init<T> : ListNetworkResource<T>()
    class Ready<T>(override val data: List<T>) : ListNetworkResource<T>()
    class Error<T>(previous: ListNetworkResource<T>, val errorMessage: String?, val wasLoadingMore: Boolean = false)
        : ListNetworkResource<T>() {
        override val data = previous.data
    }

    class Loading<T>(previous: ListNetworkResource<T>, val loadingMore: Boolean = false) : ListNetworkResource<T>() {
        override val data = previous.data
    }

    class Success<T>(override val data: List<T>, val canLoadMore: Boolean = false) : ListNetworkResource<T>()

    fun shouldFetch(loadMore: Boolean): Boolean {
        return when (this) {
            is Loading -> false // Already fetching
            is Success -> if (loadMore) canLoadMore else true // Trying to load more or refreshing
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

@Suppress("unused")
sealed class SearchNetworkResource<T> {
    protected var listNetworkResource: ListNetworkResource<T> = ListNetworkResource.Init()

    class Ready<T> : SearchNetworkResource<T>() {
        init {
            listNetworkResource = ListNetworkResource.Ready(ArrayList())
        }
    }
    class Error<T>(previous: SearchNetworkResource<T>,
                   val errorMessage: String?,
                   wasLoadingMore: Boolean = false) : SearchNetworkResource<T>() {
        init {
            listNetworkResource = ListNetworkResource.Error(previous.listNetworkResource, errorMessage, wasLoadingMore)
        }
    }
    class Loading<T>(previous: SearchNetworkResource<T>, loadingMore: Boolean = false) : SearchNetworkResource<T>() {
        init {
            listNetworkResource = ListNetworkResource.Loading(previous.listNetworkResource, loadingMore)
        }
    }
    class Success<T>(val data: List<T>, canLoadMore: Boolean = false) : SearchNetworkResource<T>() {
        init {
            listNetworkResource = ListNetworkResource.Success(data, canLoadMore)
        }
    }

    fun shouldFetch(loadMore: Boolean): Boolean = listNetworkResource.shouldFetch(loadMore)
    fun isFetching(loadMore: Boolean = false) = listNetworkResource.isFetching(loadMore)
}
