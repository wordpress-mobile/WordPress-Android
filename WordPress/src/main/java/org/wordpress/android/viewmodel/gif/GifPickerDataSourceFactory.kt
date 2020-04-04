package org.wordpress.android.viewmodel.gif

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.DataSource.Factory
import org.wordpress.android.viewmodel.gif.provider.GifProvider
import javax.inject.Inject

/**
 * Creates instances of [GifPickerDataSource]
 *
 * Whenever the [searchQuery] is changed:
 *
 * 1. The last [GifPickerDataSource] is invalidated
 * 2. The [LivePagedListBuilder] will create a new [GifPickerDataSource] by calling [create]
 * 3. The new [GifPickerDataSource] will start another paged API request
 */
class GifPickerDataSourceFactory @Inject constructor(
    private val gifProvider: GifProvider
) : Factory<Int, GifMediaViewModel>() {
    /**
     * The active search query.
     *
     * When changed, the current [GifPickerDataSource] will be invalidated. A new API search will be performed.
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
    private val dataSource = MutableLiveData<GifPickerDataSource>()

    /**
     * The [GifPickerDataSource.initialLoadError] of the current [dataSource]
     */
    val initialLoadError: Throwable? get() = dataSource.value?.initialLoadError
    /**
     * The [GifPickerDataSource.rangeLoadErrorEvent] of the current [dataSource]
     */
    val rangeLoadErrorEvent: LiveData<Throwable> = Transformations.switchMap(dataSource) { it.rangeLoadErrorEvent }

    /**
     * Retries all previously failed page loads.
     *
     * @see [GifPickerDataSource.retryAllFailedRangeLoads]
     */
    fun retryAllFailedRangeLoads() = dataSource.value?.retryAllFailedRangeLoads()

    override fun create(): DataSource<Int, GifMediaViewModel> {
        val dataSource = GifPickerDataSource(gifProvider, searchQuery)
        this.dataSource.postValue(dataSource)
        return dataSource
    }
}
