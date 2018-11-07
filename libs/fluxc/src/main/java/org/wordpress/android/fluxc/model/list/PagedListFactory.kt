package org.wordpress.android.fluxc.model.list

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.paging.DataSource
import android.arch.paging.PositionalDataSource
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.list.PagedListItemType.LoadingItem
import org.wordpress.android.fluxc.model.list.PagedListItemType.ReadyItem
import org.wordpress.android.fluxc.model.list.datastore.ListDataStoreInterface
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListItemsChanged

class PagedListFactory<T, R>(
    private val dispatcher: Dispatcher,
    private val dataStore: ListDataStoreInterface<T>,
    private val listDescriptor: ListDescriptor,
    private val lifecycle: Lifecycle,
    private val getList: (ListDescriptor) -> List<Long>,
    private val transform: (T) -> R
) : DataSource.Factory<Int, PagedListItemType<R>>() {
    private var currentSource: PagedListPositionalDataSource<T, R>? = null

    override fun create(): DataSource<Int, PagedListItemType<R>> {
        // Cleanup the previous source!
        currentSource?.onDestroy()
        val source = PagedListPositionalDataSource(dispatcher, dataStore, listDescriptor, lifecycle, getList, transform)
        currentSource = source
        return source
    }

    fun invalidate() {
        currentSource?.invalidate()
    }
}

private class PagedListPositionalDataSource<T, R>(
    private val dispatcher: Dispatcher,
    private val dataStore: ListDataStoreInterface<T>,
    private val listDescriptor: ListDescriptor,
    private val lifecycle: Lifecycle,
    getList: (ListDescriptor) -> List<Long>,
    private val transform: (T) -> R
) : PositionalDataSource<PagedListItemType<R>>(), LifecycleObserver {
    private val localItems = dataStore.localItems(listDescriptor)
    private val remoteItemIds: List<Long> = getList(listDescriptor)
    private val totalSize: Int = localItems.size + remoteItemIds.size
    private var isRegistered = true

    init {
        dispatcher.register(this)
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        if (isRegistered) {
            lifecycle.removeObserver(this)
            dispatcher.unregister(this)
            isRegistered = false
        }
    }

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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListChanged(event: OnListChanged) {
        if (!event.listDescriptors.contains(listDescriptor)) {
            return
        }
        invalidate()
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @Suppress("unused")
    fun onListItemsChanged(event: OnListItemsChanged) {
        if (listDescriptor.typeIdentifier != event.type) {
            return
        }
        invalidate()
    }
}
