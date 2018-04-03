package org.wordpress.android.models

import android.os.Handler
import kotlin.properties.Delegates

private class BaseListNetworkResource<T>(private val fetchFunction: (Boolean) -> Unit) {
    var items: List<T> = ArrayList()
    var status: ListNetworkResourceState = ListNetworkResourceState.Ready

    fun error(message: String?, isLoadingMore: Boolean) {
        val type = if (isLoadingMore)
            ListNetworkResourceState.ErrorType.PAGINATION_ERROR else ListNetworkResourceState.ErrorType.FETCH_ERROR
        updateStatus(ListNetworkResourceState.Error(type, message))
    }

    fun success(items: List<T>, canLoadMore: Boolean) {
        updateItems(items)
        updateStatus(ListNetworkResourceState.Success(canLoadMore))
    }

    fun reset() {
        updateItems(ArrayList())
        updateStatus(ListNetworkResourceState.Ready)
    }

    fun fetchFirstPage() {
        if (!status.isFetchingFirstPage()) {
            loading(false)
            fetchFunction(false)
        }
    }

    fun loadMore() {
        val currentStatus = status
        if (currentStatus is ListNetworkResourceState.Success && currentStatus.canLoadMore) {
            loading(true)
            fetchFunction(true)
        }
    }

    private fun loading(isLoadingMore: Boolean) {
        updateStatus(ListNetworkResourceState.Loading(isLoadingMore))
    }

    private fun updateItems(newItems: List<T>) {
        items = newItems
    }

    private fun updateStatus(newStatus: ListNetworkResourceState) {
        status = newStatus
    }
}

interface IListNetworkResource<in T> {
    fun error(message: String?, isLoadingMore: Boolean = false)
    fun success(items: List<T>, canLoadMore: Boolean = false)
    fun pullToRefresh()
    fun loadMore()
}

class ListNetworkResource<in T>(private var fetchFunction: (Boolean) -> Unit) : IListNetworkResource<T> {
    private val base = BaseListNetworkResource<T> { loadMore ->
        fetchFunction(loadMore)
    }

    override fun error(message: String?, isLoadingMore: Boolean = false) = base.error(message, isLoadingMore)
    override fun success(items: List<T>, canLoadMore: Boolean = false) = base.success(items, canLoadMore)
    override fun pullToRefresh() = base.fetchFirstPage()
    override fun loadMore() = base.loadMore()
}

class SearchListNetworkResource<in T>(minCharacterCount: Int = 2,
                                      private val delayMillis: Long = 250,
                                      private val searchFunction: (String, Boolean) -> Unit) : IListNetworkResource<T> {
    private val base = BaseListNetworkResource<T> { loadMore ->
        searchFunction(searchQuery, loadMore)
    }

    override fun error(message: String?, isLoadingMore: Boolean = false) = base.error(message, isLoadingMore)
    override fun success(items: List<T>, canLoadMore: Boolean = false) = base.success(items, canLoadMore)
    override fun pullToRefresh() = base.fetchFirstPage()
    override fun loadMore() = base.loadMore()

    private val handler by lazy { Handler() }

    var searchQuery: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            submitSearch(searchQuery, true)
        }
    }

    private val canSearch: Boolean = searchQuery.length >= minCharacterCount
    val shouldShowEmptySearchResultsView: Boolean
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
