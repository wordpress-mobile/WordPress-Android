package org.wordpress.android.viewmodel.giphy

import android.arch.paging.DataSource
import android.arch.paging.DataSource.Factory
import com.giphy.sdk.core.network.api.GPHApiClient
import org.wordpress.android.BuildConfig

/**
 * Creates instances of [GiphyPickerDataSource]
 *
 * Whenever the [searchQuery] is changed:
 *
 * 1. The last [GiphyPickerDataSource] is invalidated
 * 2. The [LivePagedListBuilder] will create a new [GiphyPickerDataSource] by calling [create]
 * 3. The new [GiphyPickerDataSource] will start another paged API request
 */
class GiphyPickerDataSourceFactory : Factory<Int, GiphyMediaViewModel>() {
    /**
     * The Giphy API client
     *
     * The API key should be set in a `wp.giphy.api_key` setting in `gradle.properties`.
     */
    private val apiClient: GPHApiClient by lazy { GPHApiClient(BuildConfig.GIPHY_API_KEY) }

    private var searchQuery: String = ""

    /**
     * The last [dataSource] that was created
     *
     * We retain this so we can invalidate it later in [setSearchQuery]
     */
    private var dataSource: DataSource<Int, GiphyMediaViewModel>? = null

    /**
     * Set the current [searchQuery] and invalidate the current [GiphyPickerDataSource]
     */
    fun setSearchQuery(searchQuery: String) {
        this.searchQuery = searchQuery
        dataSource?.invalidate()
    }

    override fun create(): DataSource<Int, GiphyMediaViewModel> {
        val dataSource = GiphyPickerDataSource(apiClient, searchQuery)
        this.dataSource = dataSource
        return dataSource
    }
}
