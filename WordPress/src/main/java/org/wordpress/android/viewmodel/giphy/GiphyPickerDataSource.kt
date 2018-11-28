package org.wordpress.android.viewmodel.giphy

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

        // Do not do any API call if the [searchQuery] is empty
        if (searchQuery.isBlank()) {
            callback.onResult(emptyList(), startPosition, 0)
            return
        }

        apiClient.search(searchQuery, gif, params.requestedLoadSize, startPosition, null, null) { response, _ ->
            if (response != null) {
                callback.onResult(response.data.toGiphyMediaViewModels(), startPosition, response.pagination.totalCount)
            }
        }
    }

    /**
     * Load a given range of items ([params]) from the Giphy API
     */
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<GiphyMediaViewModel>) {
        apiClient.search(searchQuery, gif, params.loadSize, params.startPosition, null, null) { response, _ ->
            if (response != null) {
                callback.onResult(response.data.toGiphyMediaViewModels())
            }
        }
    }

    private fun List<Media>.toGiphyMediaViewModels(): List<GiphyMediaViewModel> = map { MutableGiphyMediaViewModel(it) }
}
