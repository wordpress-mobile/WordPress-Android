package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.model.list.ListManager
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.ListType
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

const val LIST_STATE_TIMEOUT = 60 * 1000 // 1 minute

@Singleton
class ListStore @Inject constructor(
    private val listSqlUtils: ListSqlUtils,
    private val listItemSqlUtils: ListItemSqlUtils,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? ListAction ?: return

        when (actionType) {
            ListAction.FETCH_LIST -> fetchList(action.payload as FetchListPayload)
            ListAction.UPDATE_LIST -> updateList(action.payload as UpdateListPayload)
            ListAction.DELETE_LIST_ITEMS -> listItemsDeleted(action.payload as DeleteListItemsPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    fun <T> getListManager(listDescriptor: ListDescriptor, dataSource: ListItemDataSource<T>): ListManager<T> {
        val listModel = getListModel(listDescriptor)
        val listItems = if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id)
        } else emptyList()
        return ListManager(
                mDispatcher,
                listDescriptor,
                listItems,
                dataSource,
                listModel?.isFetchingFirstPage() ?: false,
                listModel?.isLoadingMore() ?: false
        )
    }

    private fun fetchList(payload: FetchListPayload) {
        val listModel = getListModel(payload.listDescriptor)
        if (!shouldFetch(listModel, payload.loadMore)) return

        val newState = if (payload.loadMore) ListState.LOADING_MORE else ListState.FETCHING_FIRST_PAGE
        listSqlUtils.insertOrUpdateList(payload.listDescriptor, newState)
        emitChange(OnListChanged(payload.listDescriptor, null))

        when (payload.listDescriptor.type) {
            ListType.POST -> TODO()
            ListType.WOO_ORDER -> TODO()
        }
    }

    private fun updateList(payload: UpdateListPayload) {
        if (!payload.isError) {
            if (!payload.loadedMore) {
                deleteListItems(payload.listDescriptor)
            }
            val state = if (payload.canLoadMore) ListState.CAN_LOAD_MORE else ListState.FETCHED
            listSqlUtils.insertOrUpdateList(payload.listDescriptor, state)
            val listModel = getListModel(payload.listDescriptor)
            if (listModel != null) { // Sanity check
                // Ensure the listId is set correctly for ListItemModels
                listItemSqlUtils.insertItemList(payload.remoteItemIds.map { remoteItemId ->
                    val listItemModel = ListItemModel()
                    listItemModel.listId = listModel.id
                    listItemModel.remoteItemId = remoteItemId
                    return@map listItemModel
                })
            }
        } else {
            listSqlUtils.insertOrUpdateList(payload.listDescriptor, ListState.ERROR)
        }
        emitChange(OnListChanged(payload.listDescriptor, payload.error))
    }

    private fun listItemsDeleted(payload: DeleteListItemsPayload) {
        val lists = payload.listDescriptors.mapNotNull { listSqlUtils.getList(it) }
        listItemSqlUtils.deleteItemsFromLists(lists.map { it.id }, payload.remoteItemIds)
    }

    private fun getListModel(listDescriptor: ListDescriptor): ListModel? =
            listSqlUtils.getList(listDescriptor)

    private fun deleteListItems(listDescriptor: ListDescriptor) {
        getListModel(listDescriptor)?.let {
            listItemSqlUtils.deleteItems(it.id)
        }
    }

    private fun shouldFetch(listModel: ListModel?, loadMore: Boolean): Boolean =
            (loadMore && shouldLoadMore(listModel)) || (!loadMore && shouldFetchFirstPage(listModel))

    private fun shouldFetchFirstPage(listModel: ListModel?): Boolean =
            listModel == null || isListStateOutdated(listModel) || !listModel.isFetchingFirstPage()

    private fun shouldLoadMore(listModel: ListModel?): Boolean = listModel?.canLoadMore() ?: false

    private fun isListStateOutdated(listModel: ListModel): Boolean {
        /**
         * If certain amount of time passed since the state last updated, we should always fetch the first page.
         * For example, consider the case where we are fetching the first page of a list and the user closes the app.
         * Since we keep the state in the DB, it'll preserve it until the next session even though there is not
         * actually any request going on. This kind of check prevents such cases and also makes sure that we have
         * proper timeout.
         */
        val lastModified = DateTimeUtils.dateUTCFromIso8601(listModel.lastModified)
        val timePassed = (Date().time - lastModified.time)
        return timePassed > LIST_STATE_TIMEOUT
    }

    class OnListChanged(
        val listDescriptor: ListDescriptor,
        error: UpdateListError?
    ) : Store.OnChanged<UpdateListError>() {
        init {
            this.error = error
        }
    }

    class DeleteListItemsPayload(
        val listDescriptors: List<ListDescriptor>,
        val remoteItemIds: List<Long>
    ) : Payload<BaseNetworkError>()

    class FetchListPayload(
        val listDescriptor: ListDescriptor,
        val loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class UpdateListPayload(
        val listDescriptor: ListDescriptor,
        val remoteItemIds: List<Long>,
        val loadedMore: Boolean,
        val canLoadMore: Boolean,
        error: UpdateListError?
    ) : Payload<UpdateListError>() {
        init {
            this.error = error
        }
    }

    class UpdateListError(val type: UpdateListErrorType, val message: String? = null) : Store.OnChangedError

    enum class UpdateListErrorType {
        GENERIC_ERROR
    }
}
