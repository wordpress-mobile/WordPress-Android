package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ListItemModelTable
import com.wellsql.generated.ListModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.list.ListItemModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListItemSqlUtils @Inject constructor() {
    /**
     * This function inserts the [itemList] in the [ListItemModelTable].
     *
     * Unique constraint in [ListItemModel] will ignore duplicate records which is what we want. That'll ensure that
     * the order of the items will not be altered while the user is browsing the list. The order will fix itself
     * once the list data is refreshed.
     */
    fun insertItemList(itemList: List<ListItemModel>) {
        WellSql.insert(itemList).asSingleTransaction(true).execute()
    }

    /**
     * This function returns a list of [ListItemModel] records for the given [listId].
     */
    fun getListItems(listId: Int): List<ListItemModel> =
            WellSql.select(ListItemModel::class.java)
                    .where()
                    .equals(ListItemModelTable.LIST_ID, listId)
                    .endWhere()
                    .orderBy(ListModelTable.ID, SelectQuery.ORDER_ASCENDING)
                    .asModel

    /**
     * This function deletes [ListItemModel] records for the [listIds].
     */
    fun deleteItem(listIds: List<Int>, remoteItemId: Long) {
        WellSql.delete(ListItemModel::class.java)
                .where()
                .isIn(ListItemModelTable.LIST_ID, listIds)
                .equals(ListItemModelTable.REMOTE_ITEM_ID, remoteItemId)
                .endWhere()
                .execute()
    }

    /**
     * This function deletes all [ListItemModel]s for a specific [listId].
     */
    fun deleteItems(listId: Int) {
        WellSql.delete(ListItemModel::class.java)
                .where()
                .equals(ListItemModelTable.LIST_ID, listId)
                .endWhere()
                .execute()
    }

    /**
     * This function deletes [ListItemModel]s for [remoteItemIds] in every lists with [listIds]
     */
    fun deleteItemsFromLists(listIds: List<Int>, remoteItemIds: List<Long>) {

        /// This check fixes a bug caused by WellSQL. In `ConditionClauseBuilder` (the location where `isIn()` is
        /// declared), the code iterates through the items in the provided list argument, appending "?," to each.
        /// It then removes the very last character from the created query. The issue is, if there are no items
        /// to iterate through, removing the last character removes the opening "(" in the query. This results in a
        //  query that looks like:
        //
        //  DELETE FROM ListItemModel WHERE LIST_ID IN ) AND REMOTE_ITEM_ID IN (?)
        //
        //  When SQLite tries to run this query, it crashes attempting to compile it with an SQLiteException.
        
        if(listIds.isEmpty() || remoteItemIds.isEmpty()) {
            return
        }

        WellSql.delete(ListItemModel::class.java)
                .where()
                .isIn(ListItemModelTable.LIST_ID, listIds)
                .isIn(ListItemModelTable.REMOTE_ITEM_ID, remoteItemIds)
                .endWhere()
                .execute()
    }
}
