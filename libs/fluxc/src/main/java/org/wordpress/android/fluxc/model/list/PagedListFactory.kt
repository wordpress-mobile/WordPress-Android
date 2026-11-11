package org.wordpress.android.fluxc.model.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource

/**
 * Creates the [PagingSource] instances for `ListStore` lists and keeps track of the current one so it can be
 * invalidated.
 *
 * @param createDataSource A function that creates an instance of [InternalPagedListDataSource].
 * @param onRefreshLoaded Called with the total size of the list whenever a new [PagingSource] completes its first load.
 * @param onItemAtEndLoaded Called whenever the last item of the list is loaded, so the next page can be fetched.
 */
class PagedListFactory<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM : Any>(
    private val createDataSource: () -> InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
    private val onRefreshLoaded: (totalSize: Int) -> Unit = {},
    private val onItemAtEndLoaded: () -> Unit = {}
) {
    @Volatile
    private var currentSource: PagingSource<Int, LIST_ITEM>? = null

    fun create(): PagingSource<Int, LIST_ITEM> {
        val source = PositionalPagingSource(createDataSource, onRefreshLoaded, onItemAtEndLoaded)
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

/**
 * A positional [PagingSource] for [LIST_ITEM], keyed by the position of the first item in each page.
 *
 * The [InternalPagedListDataSource] is created lazily on the first load, so its snapshot of the list's identifiers is
 * taken off the main thread.
 */
private class PositionalPagingSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM : Any>(
    createDataSource: () -> InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>,
    private val onRefreshLoaded: (totalSize: Int) -> Unit,
    private val onItemAtEndLoaded: () -> Unit
) : PagingSource<Int, LIST_ITEM>() {
    private val dataSource by lazy(createDataSource)

    override val jumpingSupported = true

    override fun getRefreshKey(state: PagingState<Int, LIST_ITEM>): Int? =
        state.anchorPosition?.let { maxOf(0, it - state.config.initialLoadSize / 2) }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, LIST_ITEM> = withContext(Dispatchers.IO) {
        val totalSize = dataSource.totalSize
        val key = params.key ?: 0
        val start = when (params) {
            is LoadParams.Prepend -> maxOf(0, key - params.loadSize)
            is LoadParams.Append -> key
            is LoadParams.Refresh -> key.coerceIn(0, maxOf(0, totalSize - params.loadSize))
        }
        val end = if (params is LoadParams.Prepend) key else minOf(start + params.loadSize, totalSize)
        val items = if (start < end) dataSource.getItemsInRange(start, end) else emptyList()

        // a superseded generation mustn't overwrite the state reported by the one that replaced it
        if (!invalid) {
            if (params is LoadParams.Refresh) {
                onRefreshLoaded(totalSize)
            }
            if (end == totalSize && totalSize > 0) {
                onItemAtEndLoaded()
            }
        }

        LoadResult.Page(
            data = items,
            prevKey = start.takeIf { it > 0 },
            nextKey = end.takeIf { it < totalSize },
            itemsBefore = start,
            itemsAfter = totalSize - end
        )
    }
}
