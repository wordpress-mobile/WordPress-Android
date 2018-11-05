package org.wordpress.android.ui.posts

import android.arch.paging.DataSource
import android.arch.paging.PositionalDataSource
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.launch
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.ui.posts.ListItemType.LoadingItem
import org.wordpress.android.ui.posts.ListItemType.ReadyItem

sealed class ListItemType<T> {
    class EndListIndicatorItem<T> : ListItemType<T>()
    class LoadingItem<T>(val remoteItemId: Long) : ListItemType<T>()
    class ReadyItem<T>(val item: T) : ListItemType<T>()
}

class PostFactory(
    private val pagedListDataSource: PagedListDataSource<PostModel>,
    private val listStore: ListStore,
    private val listDescriptor: PostListDescriptor,
    private val transform: (PostModel) -> PostAdapterItem
) : DataSource.Factory<Int, ListItemType<PostAdapterItem>>() {
    private var currentSource: PostPositionalDataSource? = null

    override fun create(): DataSource<Int, ListItemType<PostAdapterItem>> {
        val source = PostPositionalDataSource(pagedListDataSource, listStore, listDescriptor, transform)
        currentSource = source
        return source
    }
}

class PostPositionalDataSource(
    private val pagedListDataSource: PagedListDataSource<PostModel>,
    listStore: ListStore,
    private val listDescriptor: PostListDescriptor,
    private val transform: (PostModel) -> PostAdapterItem
) : PositionalDataSource<ListItemType<PostAdapterItem>>() {
    private val localItems = pagedListDataSource.localItems(listDescriptor)
    private val remoteItemIds: List<Long> = listStore.getList(listDescriptor)
    private val totalSize: Int = localItems.size + remoteItemIds.size

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<ListItemType<PostAdapterItem>>) {
        val startPosition = if (params.requestedStartPosition < remoteItemIds.size) params.requestedStartPosition else 0
        val items = getItems(startPosition, params.requestedLoadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, remoteItemIds.size + localItems.size)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<ListItemType<PostAdapterItem>>) {
        CoroutineScope(Dispatchers.Default).launch {
            val items = getItems(params.startPosition, params.loadSize)
            callback.onResult(items)
        }
    }

    private fun getItems(startPosition: Int, loadSize: Int): List<ListItemType<PostAdapterItem>> {
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
            val item = pagedListDataSource.getItemByRemoteId(listDescriptor, remoteItemId)
            if (item == null) {
                pagedListDataSource.fetchItem(listDescriptor, remoteItemId)
                LoadingItem<PostAdapterItem>(remoteItemId)
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
