package org.wordpress.android.fluxc.model.list

import android.arch.paging.DataSource
import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.list.datastore.InternalPagedListDataStore

/**
 * A [DataSource.Factory] instance for `ListStore` lists.
 *
 * @param createDataStore A function that creates an instance of [InternalPagedListDataStore].
 */
class PagedListFactory<LD: ListDescriptor, ID, T>(
    private val createDataStore: () -> InternalPagedListDataStore<LD, ID, T>
) : DataSource.Factory<Int, T>() {
    private var currentSource: PagedListPositionalDataSource<LD, ID, T>? = null

    override fun create(): DataSource<Int, T> {
        val source = PagedListPositionalDataSource(dataStore = createDataStore.invoke())
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

/**
 * A positional data source for [T].
 *
 * @param dataStore Describes how to take certain actions such as fetching list for the item type [T].
 */
private class PagedListPositionalDataSource<LD: ListDescriptor, ID, T>(
    private val dataStore: InternalPagedListDataStore<LD, ID, T>
) : PositionalDataSource<T>() {
    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<T>) {
        val totalSize = dataStore.totalSize
        val startPosition = computeInitialLoadPosition(params, totalSize)
        val loadSize = computeInitialLoadSize(params, startPosition, totalSize)
        val items = loadRangeInternal(startPosition, loadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, totalSize)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<T>) {
        val items = loadRangeInternal(params.startPosition, params.loadSize)
        callback.onResult(items)
    }

    private fun loadRangeInternal(startPosition: Int, loadSize: Int): List<T> {
        val endPosition = startPosition + loadSize
        if (startPosition == endPosition) {
            return emptyList()
        }
        return dataStore.getItemsInRange(startPosition, endPosition)
    }
}
