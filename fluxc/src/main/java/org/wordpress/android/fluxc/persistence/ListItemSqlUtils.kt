package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ListItemModelTable
import com.wellsql.generated.ListModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.list.ListItemModel
import org.wordpress.android.fluxc.persistence.WellSqlConfig.Companion.SQLITE_MAX_VARIABLE_NUMBER
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
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
     * It catches exceptions that occur during the database query and handles them.
     */
    @Suppress("TooGenericExceptionCaught")
    fun getListItems(listId: Int): List<ListItemModel> {
        return try {
            // Attempt to execute the query and map the result to models
            getListItemsQuery(listId).asModel
        } catch (e: Exception) {
            // Handle exceptions that might occur
            AppLog.e(T.DB, "Error fetching items for listId: $listId", e)
            emptyList()
        }
    }

    /**
     * This function returns the number of records a list has for the given [listId].
     */
    fun getListItemsCount(listId: Int): Long = getListItemsQuery(listId).count()

    /**
     * A helper function that returns the select query for a list of [ListItemModel] records for the given [listId].
     */
    private fun getListItemsQuery(listId: Int): SelectQuery<ListItemModel> =
            WellSql.select(ListItemModel::class.java)
                    .where()
                    .equals(ListItemModelTable.LIST_ID, listId)
                    .endWhere()
                    .orderBy(ListModelTable.ID, SelectQuery.ORDER_ASCENDING)

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
        // Prevent a crash caused by either of these lists being empty
        if (listIds.isEmpty() || remoteItemIds.isEmpty()) {
            return
        }

        val batches = listIds.chunked(SQLITE_MAX_VARIABLE_NUMBER - remoteItemIds.count())
        batches.forEach {
            WellSql.delete(ListItemModel::class.java)
                .where()
                .isIn(ListItemModelTable.LIST_ID, it)
                .isIn(ListItemModelTable.REMOTE_ITEM_ID, remoteItemIds)
                .endWhere()
                .execute()
        }
    }
}
