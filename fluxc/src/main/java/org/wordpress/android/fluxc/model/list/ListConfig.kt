package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.store.ListStore
import androidx.paging.PagedList.Config.Builder

private const val DB_PAGE_SIZE = 10
private const val INITIAL_LOAD_SIZE = 20
private const val NETWORK_PAGE_SIZE = 60
private const val PRE_FETCH_DISTANCE = DB_PAGE_SIZE * 3

/**
 * Configures how the [ListStore] loads content from its DataSource.
 *
 * @param networkPageSize The number of items to request when fetching a page of data from the API.
 * @param initialLoadSize How many items to load when first load occurs. Typically larger than [networkPageSize] so
 * a large enough range of content is loaded to cover small scrolls.
 * See [Builder.setInitialLoadSizeHint] for more information.
 * @param dbPageSize The number of items loaded at once from the DataSource (should be several times the number
 * of visible items onscreen). Smaller page sizes improve memory usage, latency, and avoid GC churn. Larger pages
 * generally improve loading throughput, to a point.
 * See [Builder.setPageSize] for more information.
 * @param prefetchDistance ?
 */
class ListConfig(val networkPageSize: Int, val initialLoadSize: Int, val dbPageSize: Int, val prefetchDistance: Int) {
    companion object {
        val default = ListConfig(
                networkPageSize = NETWORK_PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
                dbPageSize = DB_PAGE_SIZE,
                prefetchDistance = PRE_FETCH_DISTANCE
        )
    }
}
