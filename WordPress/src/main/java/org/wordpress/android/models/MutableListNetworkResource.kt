package org.wordpress.android.models

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.wordpress.android.models.ListNetworkResource.Status

class MutableListNetworkResource<T>(private val paginationAvailable: Boolean = true) : ListNetworkResource<T> {
    private var _data: MutableLiveData<List<T>> = MutableLiveData()
    private var _status: MutableLiveData<Status> = MutableLiveData()
    private var errorMessage: String? = null

    override val data: LiveData<List<T>>
        get() = _data

    override val status: LiveData<Status>
        get() = _status

    init {
        _status.value = Status.READY
    }

    // Checking Status

    override fun shouldFetch(loadMore: Boolean) = if (loadMore) shouldLoadMore() else shouldFetchFirstPage()

    private fun shouldLoadMore() = paginationAvailable && status.value == Status.CAN_LOAD_MORE

    private fun shouldFetchFirstPage() = status.value != Status.FETCHING_FIRST_PAGE

    // Updating Status

    fun fetching(loadingMore: Boolean = false) =
            updateStatusIfChanged(if (loadingMore) Status.LOADING_MORE else Status.FETCHING_FIRST_PAGE)

    fun connectionError() = updateStatusIfChanged(Status.CONNECTION_ERROR)

    fun fetchError(message: String?, wasLoadingMore: Boolean = false) {
        // Update the error message before the status, so the observer can use it
        errorMessage = message
        val newStatus = if (wasLoadingMore) Status.PAGINATION_ERROR else Status.FETCH_ERROR
        updateStatusIfChanged(newStatus)
    }

    fun resetStatus() {
        updateStatusIfChanged(Status.READY)
    }

    // Data Management

    fun fetchedSuccessfully(newData: List<T>, canLoadMore: Boolean = false) {
        _data.postValue(newData)
        updateStatusIfChanged(if (canLoadMore) Status.CAN_LOAD_MORE else Status.SUCCESS)
    }

    fun manuallyUpdateData(newData: List<T>) {
        _data.postValue(newData)
    }

    // Utils

    private fun updateStatusIfChanged(newStatus: Status) {
        if (status.value != newStatus) {
            _status.postValue(newStatus)
        }
    }
}
