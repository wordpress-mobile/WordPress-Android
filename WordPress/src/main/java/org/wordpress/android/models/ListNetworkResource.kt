package org.wordpress.android.models

import android.arch.lifecycle.MutableLiveData
import java.util.Date

class ListNetworkResource<T> constructor(private val paginationAvailable: Boolean = true) {
    private enum class Status {
        READY, // Initial state, data has not yet been fetched or refreshed

        // Success states
        SUCCESS, // All data has been successfully fetched - unless user triggers a manual fetch, it's the final state
        CAN_LOAD_MORE, // Some, but not all, data has been successfully fetched - next page can be requested

        // Error states
        FETCH_ERROR, // Initial fetch has failed
        PAGINATION_ERROR, // Initial fetch was successful, but there was a pagination error
        NETWORK_ERROR, // Fetch action never dispatched due to no connectivity

        // Loading states
        FETCHING_FIRST_PAGE, // Fetching or refreshing first page
        LOADING_MORE // Pagination
    }
    private var status: MutableLiveData<Status> = MutableLiveData()
    private var lastFetchDate: Date? = null

    init {
        status.value = Status.READY
    }

    fun shouldFetchFirstPage() = status.value != Status.FETCHING_FIRST_PAGE

    fun fetchingFirstPage() {
        lastFetchDate = Date()
        if (status.value != Status.FETCHING_FIRST_PAGE) {
            status.postValue(Status.FETCHING_FIRST_PAGE)
        }
    }

    fun shouldLoadMore() = paginationAvailable && status.value == Status.CAN_LOAD_MORE

    fun loadingMore() {
        if (status.value != Status.LOADING_MORE) {
            status.postValue(Status.LOADING_MORE)
        }
    }
}
