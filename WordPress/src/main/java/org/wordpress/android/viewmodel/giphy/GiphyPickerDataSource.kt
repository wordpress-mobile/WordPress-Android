package org.wordpress.android.viewmodel.giphy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PositionalDataSource

/**
 * The PagedListDataSource that is created and managed by [GiphyPickerDataSourceFactory]
 *
 * This performs paged API requests using the [apiClient]. A new instance of this class must be created if the
 * [searchQuery] is changed by the user.
 */
class GiphyPickerDataSource(
    private val searchQuery: String
) : PositionalDataSource<GiphyMediaViewModel>() {
    /**
     * The data structure used for storing failed [loadRange] calls so they can be retried later.
     */
    private data class RangeLoadArguments(
        val params: LoadRangeParams,
        val callback: LoadRangeCallback<GiphyMediaViewModel>
    )

    /**
     * The error received when [loadInitial] fails.
     *
     * Unlike [rangeLoadErrorEvent], this is not a [LiveData] because the consumer of this method
     * [GiphyPickerViewModel] simply uses it to check for null values and reacts to a different event.
     *
     * This is cleared when [loadInitial] is started.
     */
    var initialLoadError: Throwable? = null
        private set

    private val _rangeLoadErrorEvent = MutableLiveData<Throwable>()
    /**
     * Contains errors received during [loadRange].
     *
     * If this already contains a [Throwable], it will not be replaced until it is cleared during [loadInitial] or
     * [retryAllFailedRangeLoads].
     */
    val rangeLoadErrorEvent: LiveData<Throwable> = _rangeLoadErrorEvent

    /**
     * A list of [RangeLoadArguments] from failed [loadRange] calls.
     *
     * This will be used for retrying them in [retryAllFailedRangeLoads].
     */
    private val failedRangeLoadArguments = mutableListOf<RangeLoadArguments>()

    /**
     * Always the load the first page (startingPosition = 0) from the Giphy API
     *
     * The [GiphyPickerDataSourceFactory] recreates [GiphyPickerDataSource] instances whenever a new [searchQuery]
     * is queued. The [LoadInitialParams.requestedStartPosition] may have a value that is only valid for the
     * previous [searchQuery]. If that value is greater than the total search results of the new [searchQuery],
     * a crash will happen.
     *
     * Using `0` as the `startPosition` forces the [GiphyPickerDataSource] consumer to reset the list (UI) from the
     * top.
     */
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<GiphyMediaViewModel>) {
        val startPosition = 0

        initialLoadError = null
        _rangeLoadErrorEvent.postValue(null)

        // Do not do any API call if the [searchQuery] is empty
        if (searchQuery.isBlank()) {
            callback.onResult(emptyList(), startPosition, 0)
            return
        }

        callback.onResult(emptyList(), startPosition, 0)
    }

    /**
     * Load a given range of items ([params]) from the Giphy API.
     *
     * Errors are dispatched to [rangeLoadErrorEvent]. If successful, previously failed calls of this method are
     * automatically retried using [retryAllFailedRangeLoads].
     */
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<GiphyMediaViewModel>) {
        // Logic removed for now.
    }

    /**
     * Retry all previously failed [loadRange] calls.
     *
     * This is not done automatically by the Paging Library for us so we implement our own system in here. This is
     * automatically called after every successful [loadRange]. Manually calling this is also fine.
     */
    fun retryAllFailedRangeLoads() {
        _rangeLoadErrorEvent.postValue(null)

        // Use toList() to operate on a copy of failedRangeLoadArguments and prevent concurrency issues.
        failedRangeLoadArguments.toList().forEach { args ->
            loadRange(args.params, args.callback)
            failedRangeLoadArguments.remove(args)
        }
    }
}
