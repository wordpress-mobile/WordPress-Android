package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
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

    fun updateList(site: SiteModel, listType: ListType, listItems: List<ListItemModel>, loadedMore: Boolean) {
        if (!loadedMore) {
            deleteList(site, listType)
        }
        listSqlUtils.insertOrUpdateList(site.id, listType)
        val listModel = getList(site, listType)
        if (listModel != null) { // Sanity check
            for (listItem in listItems) {
                listItem.listId = listModel.id
            }
            // Ensure the listId is set correctly for ListItemModels
            listItemSqlUtils.insertItemList(listItems.map {
                it.listId = listModel.id
                return@map it
            })
        }
    }

    private fun getList(site: SiteModel, listType: ListType): ListModel? = listSqlUtils.getList(site.id, listType)

    private fun deleteList(site: SiteModel, listType: ListType) {
        listSqlUtils.deleteList(site.id, listType)
    }
}
