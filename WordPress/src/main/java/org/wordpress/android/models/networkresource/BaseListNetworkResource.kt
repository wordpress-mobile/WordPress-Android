package org.wordpress.android.models.networkresource

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.models.ListNetworkResourceState

class BaseListNetworkResource<T>(private val fetchFunction: (Boolean) -> Unit) {
    private var _items: List<T> = ArrayList()
    private var _status: ListNetworkResourceState = ListNetworkResourceState.Init
    private var _liveData = MutableLiveData<List<T>>()
    private var _liveStatus = MutableLiveData<ListNetworkResourceState>()

    val items: List<T>
        get() = _items
    val status: ListNetworkResourceState
        get() = _status
    val liveData: LiveData<List<T>>
        get() = _liveData
    val liveStatus: LiveData<ListNetworkResourceState>
        get() = _liveStatus

    fun ready(items: List<T>) {
        // Only update the status and fetch first page if this is the first time
        if (status === ListNetworkResourceState.Init) {
            updateItemsAndStatus(items, ListNetworkResourceState.Ready)
            fetchFirstPage()
        }
    }

    fun error(message: String?, wasLoadingMore: Boolean) {
        val type = if (wasLoadingMore)
            ListNetworkResourceState.ErrorType.PAGINATION_ERROR else ListNetworkResourceState.ErrorType.FETCH_ERROR
        updateItemsAndStatus(newStatus = ListNetworkResourceState.Error(type, message))
    }

    fun success(items: List<T>, canLoadMore: Boolean) {
        updateItemsAndStatus(items, ListNetworkResourceState.Success(canLoadMore))
    }

    fun reset() {
        updateItemsAndStatus(ArrayList(), ListNetworkResourceState.Ready)
    }

    fun fetchFirstPage() {
        require(status !== ListNetworkResourceState.Init) {
            "ready() needs to be called before first page can be fetched!"
        }
        if (!_status.isFetchingFirstPage()) {
            loading(false)
            fetchFunction(false)
        }
    }

    fun loadMore() {
        val currentStatus = _status
        if (currentStatus is ListNetworkResourceState.Success && currentStatus.canLoadMore) {
            loading(true)
            fetchFunction(true)
        }
    }

    private fun loading(isLoadingMore: Boolean) {
        updateItemsAndStatus(newStatus = ListNetworkResourceState.Loading(isLoadingMore))
    }

    // Attempt to post value updates together after both values are updated
    private fun updateItemsAndStatus(newItems: List<T>? = null, newStatus: ListNetworkResourceState? = null) {
        newItems?.let {
            _items = it
        }
        newStatus?.let {
            _status = it
        }
        if (newItems != null) _liveData.postValue(_items)
        if (newStatus != null) _liveStatus.postValue(_status)
    }
}
