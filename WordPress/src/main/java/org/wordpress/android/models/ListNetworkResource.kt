package org.wordpress.android.models

import android.arch.lifecycle.LiveData

interface ListNetworkResource<T> {
    enum class Status {
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
    val data: LiveData<List<T>>
    val status: LiveData<Status>

    fun isEmpty(): Boolean
}
