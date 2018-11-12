package org.wordpress.android.fluxc.model.list

import android.arch.paging.DataSource
import android.arch.paging.PositionalDataSource
import org.wordpress.android.fluxc.model.list.InternalItem.InternalEndListItem
import org.wordpress.android.fluxc.model.list.InternalItem.InternalLocalItem
import org.wordpress.android.fluxc.model.list.InternalItem.InternalRemoteItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.EndListIndicatorItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.LoadingItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem
import org.wordpress.android.fluxc.model.list.datastore.ListDataStoreInterface

/**
 * A helper internal item to make it easier to manage the list in [PagedListPositionalDataSource].
 */
private sealed class InternalItem<T> {
    class InternalLocalItem<T>(val localItem: T) : InternalItem<T>()
    class InternalRemoteItem<T>(val remoteItemId: Long) : InternalItem<T>()
    class InternalEndListItem<T> : InternalItem<T>()
}

/**
 * A [DataSource.Factory] instance for `ListStore` lists. It creates instances of [PagedListPositionalDataSource].
 *
 * All properties are passed to [PagedListPositionalDataSource] during instantiation.
 */
class PagedListFactory<T, R>(
    private val dataStore: ListDataStoreInterface<T>,
    private val listDescriptor: ListDescriptor,
    private val getList: (ListDescriptor) -> List<Long>,
    private val isListFullyFetched: (ListDescriptor) -> Boolean,
    private val transform: (T) -> R
) : DataSource.Factory<Int, PagedListItemType<R>>() {
    private var currentSource: PagedListPositionalDataSource<T, R>? = null

    override fun create(): DataSource<Int, PagedListItemType<R>> {
        val source = PagedListPositionalDataSource(
                listDescriptor = listDescriptor,
                dataStore = dataStore,
                getList = getList,
                isListFullyFetched = isListFullyFetched,
                transform = transform
        )
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

/**
 * A positional data source for [PagedListItemType].
 *
 * @param listDescriptor Which list this data source is for.
 * @param dataStore Describes how to take certain actions such as fetching list for the item type [T].
 * @param getList A function to get the list for the given [ListDescriptor]
 * @param isListFullyFetched A function to check whether the list is fully fetched. It's used to add an
 * [EndListIndicatorItem] at the end of the list.
 * @param transform A transform function from the actual item type [T], to the resulting item type [R]. In many
 * cases there are a lot of expensive calculations that needs to be made before an item can be used. This function
 * provides a way to do that during the pagination step in a background thread, so the clients won't need to
 * worry about these expensive operations.
 */
private class PagedListPositionalDataSource<T, R>(
    private val listDescriptor: ListDescriptor,
    private val dataStore: ListDataStoreInterface<T>,
    getList: (ListDescriptor) -> List<Long>,
    isListFullyFetched: (ListDescriptor) -> Boolean,
    private val transform: (T) -> R
) : PositionalDataSource<PagedListItemType<R>>() {
    // Create internal items to make it easier to manage it
    private val internalItems: List<InternalItem<T>> by lazy {
        val localItems = dataStore.localItems(listDescriptor).map { InternalLocalItem(it) }
        val remoteItemIdsToHide = dataStore.getItemIdsToHide(listDescriptor).mapNotNull { it.second }
        val remoteItems = getList(listDescriptor).asSequence().filter {
            !remoteItemIdsToHide.contains(it)
        }.map { InternalRemoteItem<T>(it) }.toList()
        val actualItems = localItems.plus(remoteItems)
        // We only want to show the end list indicator if the list is fully fetched and it's not empty
        if (isListFullyFetched(listDescriptor) && actualItems.isNotEmpty()) {
            actualItems.plus(InternalEndListItem<T>())
        } else {
            actualItems
        }
    }
    private val totalSize: Int by lazy { internalItems.size }

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<PagedListItemType<R>>) {
        val startPosition = computeInitialLoadPosition(params, totalSize)
        val loadSize = computeInitialLoadSize(params, startPosition, totalSize)
        val items = loadRangeInternal(startPosition, loadSize)
        if (params.placeholdersEnabled) {
            callback.onResult(items, startPosition, totalSize)
        } else {
            callback.onResult(items, startPosition)
        }
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<PagedListItemType<R>>) {
        val items = loadRangeInternal(params.startPosition, params.loadSize)
        callback.onResult(items)
    }

    private fun loadRangeInternal(startPosition: Int, loadSize: Int): List<PagedListItemType<R>> {
        val endPosition = startPosition + loadSize
        if (startPosition == endPosition) {
            return emptyList()
        }
        return (startPosition..(endPosition - 1)).map { index ->
            val internalItem = internalItems[index]
            when (internalItem) {
                is InternalEndListItem -> EndListIndicatorItem<R>()
                is InternalLocalItem -> ReadyItem(transform(internalItem.localItem))
                is InternalRemoteItem -> {
                    val remoteItemId = internalItem.remoteItemId
                    val item = dataStore.getItemByRemoteId(listDescriptor, remoteItemId)
                    if (item == null) {
                        dataStore.fetchItem(listDescriptor, remoteItemId)
                        LoadingItem<R>(remoteItemId)
                    } else {
                        ReadyItem(transform(item))
                    }
                }
            }
        }
    }
}
