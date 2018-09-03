package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload

class ListManager<T>(
    private val dispatcher: Dispatcher,
    private val listDescriptor: ListDescriptor,
    private val items: List<ListItemModel>,
    private val dataSource: ListItemDataSource<T>,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean
) {
    val size: Int = items.size

    fun getRemoteItem(position: Int): T? {
        if (position == size - 1) {
            loadMore()
        }
        val listItemModel = items[position]
        val remoteItemId = listItemModel.remoteItemId
        val item = dataSource.getItem(remoteItemId)
        if (item == null) {
            dataSource.fetchItem(remoteItemId)
        }
        return item
    }

    fun indexOfItem(remoteItemId: Long): Int? {
        val index = items.indexOfFirst { it.remoteItemId == remoteItemId }
        return if (index != -1) index else null
    }

    fun refresh() {
        dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor)))
    }

    private fun loadMore() {
        dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(listDescriptor, true)))
    }

    fun hasDataChanged(otherListManager: ListManager<T>): Boolean {
        if (listDescriptor != otherListManager.listDescriptor || items.size != otherListManager.items.size)
            return true
        return !items.zip(otherListManager.items).fold(true) { result, pair ->
            result && pair.first.remoteItemId == pair.second.remoteItemId
        }
    }
}
