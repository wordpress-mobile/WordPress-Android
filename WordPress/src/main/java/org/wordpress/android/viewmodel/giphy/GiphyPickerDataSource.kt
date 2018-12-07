package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.paging.PositionalDataSource
import com.giphy.sdk.core.models.Media
import com.giphy.sdk.core.models.enums.MediaType.gif
import com.giphy.sdk.core.network.api.GPHApiClient

/**
 * The PagedListDataSource that is created and managed by [GiphyPickerDataSourceFactory]
 *
 * This performs paged API requests using the [apiClient]. A new instance of this class must be created if the
 * [searchQuery] is changed by the user.
 */
class GiphyPickerDataSource(
    private val apiClient: GPHApiClient,
    private val searchQuery: String
) : PositionalDataSource<GiphyMediaViewModel>() {

    private data class RangeLoadArguments(
        val params: LoadRangeParams,
        val callback: LoadRangeCallback<GiphyMediaViewModel>
    )

    var initialLoadError: Throwable? = null
        private set

    private val _rangeLoadErrorEvent = MutableLiveData<Throwable>()
    val rangeLoadErrorEvent: LiveData<Throwable> = _rangeLoadErrorEvent

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

        apiClient.search(searchQuery, gif, params.requestedLoadSize, startPosition, null, null) { response, error ->
            if (response != null) {
                callback.onResult(response.data.toGiphyMediaViewModels(), startPosition, response.pagination.totalCount)
            } else {
                initialLoadError = error
                callback.onResult(emptyList(), startPosition, 0)
            }
        }
    }

    /**
     * Load a given range of items ([params]) from the Giphy API
     */
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<GiphyMediaViewModel>) {
        apiClient.search(searchQuery, gif, params.loadSize, params.startPosition, null, null) { response, error ->
            if (response != null) {
                callback.onResult(response.data.toGiphyMediaViewModels())

                retryAllFailedRangeLoads()
            } else {
                failedRangeLoadArguments.add(RangeLoadArguments(params, callback))

                // Do not replace the error if we already dispatched one. This makes the UI better since loadRange()
                // gets called every time we load a part in the endless scroll. The user would be seeing an endless
                // stream of error messages if we don't throttle it here.
                if (_rangeLoadErrorEvent.value == null) {
                    _rangeLoadErrorEvent.value = error
                }
            }
        }
    }

    fun retryAllFailedRangeLoads() {
        _rangeLoadErrorEvent.postValue(null)

        // Use toList() to operate on a copy of failedRangeLoadArguments and prevent concurrency issues.
        failedRangeLoadArguments.toList().forEach { args ->
            loadRange(args.params, args.callback)
            failedRangeLoadArguments.remove(args)
        }
    }

    private fun List<Media>.toGiphyMediaViewModels(): List<GiphyMediaViewModel> = map { MutableGiphyMediaViewModel(it) }
}
