package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.ListItemModel
import org.wordpress.android.fluxc.model.ListModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.ListItemSqlUtils
import org.wordpress.android.fluxc.persistence.ListSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

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
            ListAction.UPDATE_LIST -> updateList(action.payload as UpdateListPayload)
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, ListStore::class.java.simpleName + " onRegister")
    }

    fun getListItems(site: SiteModel, listType: ListType): List<ListItemModel> {
        val listModel = listSqlUtils.getList(site.id, listType)
        return if (listModel != null) {
            listItemSqlUtils.getListItems(listModel.id)
        } else emptyList()
    }

    private fun updateList(payload: UpdateListPayload) {
        // TODO: handle can load more by probably saving it to the DB for ListModel
        if (!payload.isError) {
            if (!payload.loadedMore) {
                deleteList(payload.localSiteId, payload.listType)
            }
            listSqlUtils.insertOrUpdateList(payload.localSiteId, payload.listType)
            val listModel = getList(payload.localSiteId, payload.listType)
            if (listModel != null) { // Sanity check
                for (listItem in payload.listItems) {
                    listItem.listId = listModel.id
                }
                // Ensure the listId is set correctly for ListItemModels
                listItemSqlUtils.insertItemList(payload.listItems.map {
                    it.listId = listModel.id
                    return@map it
                })
            }
        }
        emitChange(OnListChanged(payload.localSiteId, payload.listType, payload.error))
    }

    private fun getList(localSiteId: Int, listType: ListType): ListModel? = listSqlUtils.getList(localSiteId, listType)

    private fun deleteList(localSiteId: Int, listType: ListType) {
        listSqlUtils.deleteList(localSiteId, listType)
    }

    class OnListChanged(
        val localSiteId: Int,
        val listType: ListType,
        error: UpdateListError?
    ) : Store.OnChanged<UpdateListError>() {
        init {
            this.error = error
        }
    }

    class UpdateListPayload(
        val localSiteId: Int,
        val listType: ListType,
        val listItems: List<ListItemModel>,
        val loadedMore: Boolean,
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
