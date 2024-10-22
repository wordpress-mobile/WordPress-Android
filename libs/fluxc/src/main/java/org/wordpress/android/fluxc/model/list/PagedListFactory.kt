package org.wordpress.android.fluxc.model.list

import androidx.paging.DataSource
import androidx.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource

/**
 * A [DataSource.Factory] instance for `ListStore` lists.
 *
 * @param createDataSource A function that creates an instance of [InternalPagedListDataSource].
 */
class PagedListFactory<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM>(
    private val createDataSource: () -> InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) : DataSource.Factory<Int, LIST_ITEM>() {
    private var currentSource: PagedListPositionalDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>? = null

    override fun create(): DataSource<Int, LIST_ITEM> {
        val source = PagedListPositionalDataSource(dataSource = createDataSource.invoke())
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

/**
 * A positional data source for [LIST_ITEM].
 *
 * @param dataSource Describes how to take certain actions such as fetching list for the item type [LIST_ITEM].
 */
private class PagedListPositionalDataSource<LIST_DESCRIPTOR : ListDescriptor, ITEM_IDENTIFIER, LIST_ITEM>(
    private val dataSource: InternalPagedListDataSource<LIST_DESCRIPTOR, ITEM_IDENTIFIER, LIST_ITEM>
) : PositionalDataSource<LIST_ITEM>() {
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<LIST_ITEM>) {
        val totalSize = dataSource.totalSize
        val startPosition = computeInitialLoadPosition(params, totalSize)
        val loadSize = computeInitialLoadSize(params, startPosition, totalSize)
        val items = loadRangeInternal(startPosition, loadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, totalSize)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<LIST_ITEM>) {
        val items = loadRangeInternal(params.startPosition, params.loadSize)
        callback.onResult(items)
    }

    private fun loadRangeInternal(startPosition: Int, loadSize: Int): List<LIST_ITEM> {
        val endPosition = startPosition + loadSize
        if (startPosition == endPosition) {
            return emptyList()
        }
        return dataSource.getItemsInRange(startPosition, endPosition)
    }
}
