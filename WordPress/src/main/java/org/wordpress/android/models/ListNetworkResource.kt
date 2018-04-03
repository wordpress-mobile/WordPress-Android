package org.wordpress.android.models

import android.os.Handler
import kotlin.properties.Delegates

private class BaseListNetworkResource<in T>(private val fetchFunction: (Boolean) -> Unit) {
    private var items: List<T> = ArrayList()
    private var status: ListNetworkResourceState = ListNetworkResourceState.Ready

    fun error(message: String?, isLoadingMore: Boolean) {
        val type = if (isLoadingMore)
            ListNetworkResourceState.ErrorType.PAGINATION_ERROR else ListNetworkResourceState.ErrorType.FETCH_ERROR
        updateStatus(ListNetworkResourceState.Error(type, message))
    }

    fun loading(isLoadingMore: Boolean) {
        updateStatus(ListNetworkResourceState.Loading(isLoadingMore))
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
            fetchFunction(false)
        }
    }

    fun loadMore() {
        val currentStatus = status
        if (currentStatus is ListNetworkResourceState.Success && currentStatus.canLoadMore) {
            fetchFunction(true)
        }
    }

    private fun shouldFetch(loadMore: Boolean): Boolean {
        val currentStatus = status
        return when (currentStatus) {
            is ListNetworkResourceState.Success -> !loadMore || currentStatus.canLoadMore
            else -> true
        }
    }

    private fun updateItems(newItems: List<T>) {
        items = newItems
    }

    private fun updateStatus(newStatus: ListNetworkResourceState) {
        status = newStatus
    }
}

class ListNetworkResource<in T>(private var fetchFunction: (Boolean) -> Unit) {
    private val base = BaseListNetworkResource<T> { loadMore ->
        fetchFunction(loadMore)
    }

    fun error(message: String?, isLoadingMore: Boolean = false) = base.error(message, isLoadingMore)
    fun loading(isLoadingMore: Boolean = false) = base.loading(isLoadingMore)
    fun success(items: List<T>, canLoadMore: Boolean = false) = base.success(items, canLoadMore)
    fun pullToRefresh() = base.fetchFirstPage()
    fun loadMore() = base.loadMore()
}

class SearchListNetworkResource<in T>(private val minCharacterCount: Int = 2,
                                      private val delayMillis: Long = 250,
                                      private val searchFunction: (String, Boolean) -> Unit) {
    private val base = BaseListNetworkResource<T> { loadMore ->
        searchFunction(searchQuery, loadMore)
    }

    private val handler by lazy { Handler() }

    var searchQuery: String by Delegates.observable("") { _, old, new ->
        if (new != old) {
            submitSearch(searchQuery, true)
        }
    }

    fun error(message: String?, isLoadingMore: Boolean = false) = base.error(message, isLoadingMore)
    fun loading(isLoadingMore: Boolean = false) = base.loading(isLoadingMore)
    fun success(items: List<T>, canLoadMore: Boolean = false) = base.success(items, canLoadMore)
    fun pullToRefresh() = base.fetchFirstPage()
    fun loadMore() = base.loadMore()

    private fun submitSearch(query: String, delayed: Boolean) {
        if (delayed) {
            handler.postDelayed({
                if (query == searchQuery) {
                    submitSearch(query, false)
                }
            }, delayMillis)
        } else {
            base.reset()
            if (query.length > minCharacterCount) {
                base.fetchFirstPage()
            }
        }
    }
}
