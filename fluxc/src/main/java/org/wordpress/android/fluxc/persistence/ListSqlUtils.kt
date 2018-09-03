package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.ListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListModel
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListSqlUtils @Inject constructor() {
    /**
     * This function either creates a new [ListModel] for the [listDescriptor] or updates the existing record.
     *
     * If there is an existing record, only the [ListModel.lastModified] and [ListModel.stateDbValue] will be updated.
     * If there is no existing record, a new [ListModel] will be created for [listDescriptor].
     *
     * The current [Date] will be assigned to both [ListModel.dateCreated] and [ListModel.lastModified].
     */
    fun insertOrUpdateList(
        listDescriptor: ListDescriptor,
        listState: ListState = ListState.CAN_LOAD_MORE
    ) {
        val now = DateTimeUtils.iso8601FromDate(Date())
        val listModel = ListModel()
        listModel.lastModified = now
        listModel.stateDbValue = listState.value

        val existing = getList(listDescriptor)
        if (existing != null) {
            WellSql.update<ListModel>(ListModel::class.java)
                    .whereId(existing.id)
                    .put(listModel) { item ->
                        val cv = ContentValues()
                        cv.put(ListModelTable.LAST_MODIFIED, item.lastModified)
                        cv.put(ListModelTable.STATE_DB_VALUE, item.stateDbValue)
                        cv
                    }.execute()
        } else {
            listModel.setListDescriptor(listDescriptor)
            listModel.dateCreated = now
            WellSql.insert(listModel).execute()
        }
    }

    /**
     * This function returns the [ListModel] record for the given [listDescriptor] if there is one.
     */
    fun getList(listDescriptor: ListDescriptor): ListModel? =
            // TODO: these filters need to use the `IS NULL` or check value: `localSiteId`, `filter`, `order`
            WellSql.select(ListModel::class.java)
                    .where()
                    .equals(ListModelTable.LOCAL_SITE_ID_DB_VALUE, listDescriptor.localSiteId)
                    .equals(ListModelTable.TYPE_DB_VALUE, listDescriptor.type.value)
//                    .equals(ListModelTable.FILTER_DB_VALUE, listDescriptor.filter?.value)
//                    .equals(ListModelTable.ORDER_DB_VALUE, listDescriptor.order?.value)
                    .endWhere()
                    .asModel
                    .firstOrNull()

    /**
     * This function deletes the [ListModel] record for the given [listDescriptor] if there is one.
     *
     * To ensure that we have the same `where` queries for both `select` and `delete` queries, [getList] is utilized.
     */
    fun deleteList(listDescriptor: ListDescriptor) {
        val existing = getList(listDescriptor)
        existing?.let {
            WellSql.delete(ListModel::class.java).whereId(it.id)
        }
    }
}
