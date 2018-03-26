package org.wordpress.android.models

import android.arch.lifecycle.MutableLiveData

class ListNetworkResource<in T> constructor(private val paginationAvailable: Boolean = true) {
    private enum class Status {
        READY, // Initial state or manual reset (mostly for search)

        // Success states
        SUCCESS, // All data has been successfully fetched - unless user triggers a manual fetch, it's the final state
        CAN_LOAD_MORE, // Some, but not all, data has been successfully fetched - next page can be requested

        // Error states
        FETCH_ERROR, // Initial fetch has failed
        PAGINATION_ERROR, // Initial fetch was successful, but there was a pagination error
        CONNECTION_ERROR, // Fetch action never dispatched due to no connectivity

        // Loading states
        FETCHING_FIRST_PAGE, // Fetching or refreshing first page
        LOADING_MORE // Pagination
    }
    private var status: MutableLiveData<Status> = MutableLiveData()
    private var errorMessage: String? = null

    init {
        status.value = Status.READY
    }

    // Checking Status

    fun shouldFetch(loadMore: Boolean) = if (loadMore) shouldLoadMore() else shouldFetchFirstPage()

    private fun shouldLoadMore() = paginationAvailable && status.value == Status.CAN_LOAD_MORE

    private fun shouldFetchFirstPage() = status.value != Status.FETCHING_FIRST_PAGE

    // Updating Status

    fun fetching(loadingMore: Boolean = false) = if (loadingMore) loadingMore() else fetchingFirstPage()

    private fun fetchingFirstPage() = updateStatusIfChanged(Status.FETCHING_FIRST_PAGE)

    private fun loadingMore() = updateStatusIfChanged(Status.LOADING_MORE)

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

    fun fetchedSuccessfully(newData: List<T>, wasLoadingMore: Boolean = false) {
        TODO()
    }

    fun manuallyUpdateData(newData: List<T>) {
        TODO()
    }

    // Helpers

    fun shouldShowEmptyView(shouldShowWhileError: Boolean) : Boolean {
        TODO()
    }

    // Utils

    private fun updateStatusIfChanged(newStatus: Status) {
        if (status.value != newStatus) {
            status.postValue(newStatus)
        }
    }
}
