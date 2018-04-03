package org.wordpress.android.models.networkresource

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.models.ListNetworkResourceState

class BaseListNetworkResource<T>(private val fetchFunction: (Boolean) -> Unit) {
    private var _items: List<T> = ArrayList()
    private var _status: ListNetworkResourceState = ListNetworkResourceState.Ready
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

    fun error(message: String?, wasLoadingMore: Boolean) {
        val type = if (wasLoadingMore)
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
        updateStatus(ListNetworkResourceState.Loading(isLoadingMore))
    }

    private fun updateItems(newItems: List<T>) {
        _items = newItems
        _liveData.postValue(_items)
    }

    private fun updateStatus(newStatus: ListNetworkResourceState) {
        _status = newStatus
        _liveStatus.postValue(_status)
    }
}
