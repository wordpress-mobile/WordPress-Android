package org.wordpress.android.viewmodel.giphy

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.DataSource
import android.arch.paging.DataSource.Factory
import com.giphy.sdk.core.network.api.GPHApiClient
import org.wordpress.android.BuildConfig
import javax.inject.Inject

/**
 * Creates instances of [GiphyPickerDataSource]
 *
 * Whenever the [searchQuery] is changed:
 *
 * 1. The last [GiphyPickerDataSource] is invalidated
 * 2. The [LivePagedListBuilder] will create a new [GiphyPickerDataSource] by calling [create]
 * 3. The new [GiphyPickerDataSource] will start another paged API request
 */
class GiphyPickerDataSourceFactory @Inject constructor() : Factory<Int, GiphyMediaViewModel>() {
    /**
     * The Giphy API client
     *
     * The API key should be set in a `wp.giphy.api_key` setting in `gradle.properties`.
     */
    private val apiClient: GPHApiClient by lazy { GPHApiClient(BuildConfig.GIPHY_API_KEY) }

    /**
     * The active search query.
     *
     * When changed, the current [GiphyPickerDataSource] will be invalidated. A new API search will be performed.
     */
    var searchQuery: String = ""
        set(value) {
            field = value
            dataSource.value?.invalidate()
        }

    /**
     * The last [dataSource] that was created
     *
     * We retain this so we can invalidate it later when [searchQuery] is changed.
     */
    private val dataSource = MutableLiveData<GiphyPickerDataSource>()

    /**
     * The [GiphyPickerDataSource.initialLoadError] of the current [dataSource]
     */
    val initialLoadError: Throwable? get() = dataSource.value?.initialLoadError
    /**
     * The [GiphyPickerDataSource.rangeLoadErrorEvent] of the current [dataSource]
     */
    val rangeLoadErrorEvent: LiveData<Throwable> = Transformations.switchMap(dataSource) { it.rangeLoadErrorEvent }

    /**
     * Retries all previously failed page loads.
     *
     * @see [GiphyPickerDataSource.retryAllFailedRangeLoads]
     */
    fun retryAllFailedRangeLoads() = dataSource.value?.retryAllFailedRangeLoads()

    override fun create(): DataSource<Int, GiphyMediaViewModel> {
        val dataSource = GiphyPickerDataSource(apiClient, searchQuery)
        this.dataSource.postValue(dataSource)
        return dataSource
    }
}
