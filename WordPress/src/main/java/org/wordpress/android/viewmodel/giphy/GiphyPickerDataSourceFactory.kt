package org.wordpress.android.viewmodel.giphy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.DataSource.Factory
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
        val dataSource = GiphyPickerDataSource(searchQuery)
        this.dataSource.postValue(dataSource)
        return dataSource
    }
}
