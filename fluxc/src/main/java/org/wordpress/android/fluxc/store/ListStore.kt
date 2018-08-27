package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.ListItemModel
import org.wordpress.android.fluxc.model.ListModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.fluxc.store.ListStore.FetchListPayload
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

class ListData(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val listType: ListType,
    private val canLoadMore: Boolean,
    val items: List<ListItemModel>,
    val isFetchingFirstPage: Boolean,
    val isLoadingMore: Boolean
) {
    fun refresh() {
        if (!isFetchingFirstPage) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(site, listType)))
        }
    }

    fun loadMore() {
        if (canLoadMore) {
            dispatcher.dispatch(ListActionBuilder.newFetchListAction(FetchListPayload(site, listType, true)))
        }
    }
}

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
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    fun getList(site: SiteModel, listType: ListType): ListData {
        val listModel = getListModel(site.id, listType)
        val listItems = if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id)
        } else emptyList()
        return ListData(mDispatcher,
                site,
                listType,
                listModel?.getState()?.canLoadMore() ?: true,
                listItems,
                listModel?.getState()?.isFetchingFirstPage() ?: false,
                listModel?.getState()?.isLoadingMore() ?: false)
    }

    private fun getListSize(site: SiteModel, listType: ListType): Int {
        val listModel = getListModel(site.id, listType)
        return if (listModel != null) {
            listItemSqlUtils.getListSize(listModel.id)
        } else 0
    }

    private fun fetchList(payload: FetchListPayload) {
        val listModel = getListModel(payload.site.id, payload.listType)
        val state = listModel?.getState()
        if (payload.loadMore && state?.canLoadMore() != true) {
            // We can't load more right now, ignore
            return
        } else if (!payload.loadMore && state?.isFetchingFirstPage() == true) {
            // If we are already fetching the first page, ignore
            return
        }
        val newState = if (payload.loadMore) ListModel.State.LOADING_MORE else ListModel.State.FETCHING_FIRST_PAGE
        listSqlUtils.insertOrUpdateList(payload.site.id, payload.listType, newState)
        emitChange(OnListChanged(payload.site.id, payload.listType, null, false))

        when(payload.listType) {
            ListModel.ListType.POSTS_ALL -> {
                val offset = if (payload.loadMore) getListSize(payload.site, payload.listType) else 0
                val fetchPostsPayload = FetchPostsPayload(payload.site, payload.listType, offset)
                mDispatcher.dispatch(PostActionBuilder.newFetchPostsAction(fetchPostsPayload))
            }
            ListModel.ListType.POSTS_SCHEDULED -> TODO()
        }
    }

    private fun updateList(payload: UpdateListPayload) {
        if (!payload.isError) {
            if (!payload.loadedMore) {
                deleteList(payload.localSiteId, payload.listType)
            }
            val state = if (payload.canLoadMore) ListModel.State.CAN_LOAD_MORE else ListModel.State.FETCHED
            listSqlUtils.insertOrUpdateList(payload.localSiteId, payload.listType, state)
            val listModel = getListModel(payload.localSiteId, payload.listType)
            if (listModel != null) { // Sanity check
                // Ensure the listId is set correctly for ListItemModels
                listItemSqlUtils.insertItemList(payload.listItems.map {
                    it.listId = listModel.id
                    return@map it
                })
            }
        } else {
            listSqlUtils.insertOrUpdateList(payload.localSiteId, payload.listType, ListModel.State.ERROR)
        }
        emitChange(OnListChanged(payload.localSiteId, payload.listType, payload.error, !payload.isError))
    }

    private fun getListModel(localSiteId: Int, listType: ListType): ListModel? =
            listSqlUtils.getList(localSiteId, listType)

    private fun deleteList(localSiteId: Int, listType: ListType) {
        listSqlUtils.deleteList(localSiteId, listType)
    }

    class OnListChanged(
        val localSiteId: Int,
        val listType: ListType,
        error: UpdateListError?,
        val dataChanged: Boolean = false // should be false if the state is the only change
    ) : Store.OnChanged<UpdateListError>() {
        init {
            this.error = error
        }
    }

    class FetchListPayload(
        val site: SiteModel,
        val listType: ListType,
        val loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class UpdateListPayload(
        val localSiteId: Int,
        val listType: ListType,
        val listItems: List<ListItemModel>,
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
