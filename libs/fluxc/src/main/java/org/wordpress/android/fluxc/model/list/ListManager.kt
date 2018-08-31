package org.wordpress.android.fluxc.model.list

import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload

class ListManager<T>(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val listDescriptor: ListDescriptor,
    listState: ListState?,
    private val items: List<ListItemModel>,
    private val dataSource: ListItemDataSource<T>
) {
    private val canLoadMore: Boolean = listState?.canLoadMore() ?: false
    val isFetchingFirstPage: Boolean = listState?.isFetchingFirstPage() ?: false
    val isLoadingMore: Boolean = listState?.isLoadingMore() ?: false
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
        if (!isFetchingFirstPage) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(site, listDescriptor)))
        }
    }

    private fun loadMore() {
        if (canLoadMore) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(site, listDescriptor, true)))
        }
    }

    fun hasDataChanged(otherListManager: ListManager<T>): Boolean {
        if (site.id != otherListManager.site.id
                || listDescriptor != otherListManager.listDescriptor
                || items.size != otherListManager.items.size)
            return true
        return !items.zip(otherListManager.items).fold(true) { result, pair ->
            result && pair.first.remoteItemId == pair.second.remoteItemId
        }
    }
}
