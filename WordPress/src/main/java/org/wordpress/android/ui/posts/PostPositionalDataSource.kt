package org.wordpress.android.ui.posts

import android.arch.paging.DataSource
import android.arch.paging.PositionalDataSource
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.ui.posts.ListItemType.LoadingItem
import org.wordpress.android.ui.posts.ListItemType.ReadyItem

sealed class ListItemType<T> {
    class EndListIndicatorItem<T> : ListItemType<T>()
    class LoadingItem<T>(val remoteItemId: Long) : ListItemType<T>()
    class ReadyItem<T>(val item: T) : ListItemType<T>()
}

class PagedListFactory<T, R>(
    private val dataStore: PagedListDataStoreInterface<T>,
    private val listStore: ListStore,
    private val listDescriptor: ListDescriptor,
    private val transform: (T) -> R
) : DataSource.Factory<Int, ListItemType<R>>() {
    private var currentSource: PagedListPositionalDataSource<T, R>? = null

    override fun create(): DataSource<Int, ListItemType<R>> {
        val source = PagedListPositionalDataSource(dataStore, listStore, listDescriptor, transform)
        currentSource = source
        return source
    }
}

private class PagedListPositionalDataSource<T, R>(
    private val dataStore: PagedListDataStoreInterface<T>,
    listStore: ListStore,
    private val listDescriptor: ListDescriptor,
    private val transform: (T) -> R
) : PositionalDataSource<ListItemType<R>>() {
    private val localItems = dataStore.localItems(listDescriptor)
    private val remoteItemIds: List<Long> = listStore.getList(listDescriptor)
    private val totalSize: Int = localItems.size + remoteItemIds.size

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ListItemType<R>>) {
        val startPosition = if (params.requestedStartPosition < remoteItemIds.size) params.requestedStartPosition else 0
        val items = getItems(startPosition, params.requestedLoadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, remoteItemIds.size + localItems.size)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ListItemType<R>>) {
        // TODO: Take the scope/context as parameter
        CoroutineScope(Dispatchers.Default).launch {
            val items = getItems(params.startPosition, params.loadSize)
            callback.onResult(items)
        }
    }

    private fun getItems(startPosition: Int, loadSize: Int): List<ListItemType<R>> {
        val normalizedStart = normalizedIndex(startPosition)
        val normalizedEnd = normalizedIndex(normalizedStart + loadSize)
        if (normalizedStart == normalizedEnd) {
            return emptyList()
        }
        return (normalizedStart..(normalizedEnd - 1)).map { index ->
            if (index < localItems.size) {
                return@map ReadyItem(transform(localItems[index]))
            }
            val remoteIndex = index - localItems.size
            val remoteItemId = remoteItemIds[remoteIndex]
            val item = dataStore.getItemByRemoteId(listDescriptor, remoteItemId)
            if (item == null) {
                dataStore.fetchItem(listDescriptor, remoteItemId)
                LoadingItem<R>(remoteItemId)
            } else {
                ReadyItem(transform(item))
            }
        }
    }

    private fun normalizedIndex(index: Int): Int {
        return when {
            index <= 0 -> 0
            index >= totalSize -> totalSize
            else -> index
        }
    }
}
