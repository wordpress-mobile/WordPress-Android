package org.wordpress.android.models.networkresource

import android.arch.lifecycle.LiveData
import android.os.Handler
import org.wordpress.android.models.ListNetworkResourceState
import kotlin.properties.Delegates

interface IListNetworkResource<T> {
    val liveData: LiveData<List<T>>
    val liveStatus: LiveData<ListNetworkResourceState>
    fun pullToRefresh()
    fun loadMore()
}

interface ISearchListNetworkResource<T> : IListNetworkResource<T> {
    val searchQuery: String
    val shouldShowEmptySearchResultsView: Boolean
}

class ListNetworkResource<T>(private var fetchFunction: (Boolean) -> Unit) : IListNetworkResource<T> {
    private val base = BaseListNetworkResource<T> { loadMore ->
        fetchFunction(loadMore)
    }

    override val liveData = base.liveData
    override val liveStatus = base.liveStatus
    fun error(message: String?, wasLoadingMore: Boolean) = base.error(message, wasLoadingMore)
    fun success(items: List<T>, canLoadMore: Boolean) = base.success(items, canLoadMore)
    override fun pullToRefresh() = base.fetchFirstPage()
    override fun loadMore() = base.loadMore()
}

class SearchListNetworkResource<T>(
        minCharacterCount: Int = 2,
        private val delayMillis: Long = 250,
        private val searchFunction: (String, Boolean) -> Unit) : ISearchListNetworkResource<T> {
    private val base = BaseListNetworkResource<T> { loadMore ->
        searchFunction(searchQuery, loadMore)
    }

    override val liveData = base.liveData
    override val liveStatus = base.liveStatus
    fun error(message: String?, wasLoadingMore: Boolean) = base.error(message, wasLoadingMore)
    fun success(items: List<T>, canLoadMore: Boolean) = base.success(items, canLoadMore)
    override fun pullToRefresh() = base.fetchFirstPage()
    override fun loadMore() = base.loadMore()

    private val handler by lazy { Handler() }

    override var searchQuery: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            submitSearch(searchQuery, true)
        }
    }

    private val canSearch: Boolean = searchQuery.length >= minCharacterCount

    override val shouldShowEmptySearchResultsView: Boolean
        get() {
            // Search query is less than min characters
            if (!canSearch) {
                return false
            }
            // Only show empty view if content is empty, we are not fetching new data and no errors occurred
            return when (base.status) {
                is ListNetworkResourceState.Success -> base.items.isEmpty()
                else -> false
            }
        }

    private fun submitSearch(query: String, delayed: Boolean) {
        if (delayed) {
            handler.postDelayed({
                if (query == searchQuery) {
                    submitSearch(query, false)
                }
            }, delayMillis)
        } else {
            base.reset()
            if (canSearch) {
                base.fetchFirstPage()
            }
        }
    }
}
