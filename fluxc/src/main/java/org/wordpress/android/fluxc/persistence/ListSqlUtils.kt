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
     * This function either creates a new [ListModel] for the [localSiteId] and [listDescriptor] or updates
     * the existing record.
     *
     * If there is an existing record, only the [ListModel.lastModified] and [ListModel.stateDbValue] will be updated
     * with the current [Date]. If there is no existing record, a new [ListModel] will be created for [localSiteId]
     * and [listDescriptor].
     *
     * The current [Date] will be assigned to both [ListModel.dateCreated] and [ListModel.lastModified].
     */
    fun insertOrUpdateList(
        localSiteId: Int,
        listDescriptor: ListDescriptor,
        listState: ListState = ListState.CAN_LOAD_MORE
    ) {
        val now = DateTimeUtils.iso8601FromDate(Date())
        val listModel = ListModel()
        listModel.lastModified = now
        listModel.stateDbValue = listState.value

        val existing = getList(localSiteId, listDescriptor)
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
            listModel.type = listDescriptor.type.value
            listModel.filterDbValue = listDescriptor.filter?.value
            listModel.orderDbValue = listDescriptor.order?.value
            listModel.localSiteId = localSiteId
            listModel.dateCreated = now
            WellSql.insert(listModel).execute()
        }
    }

    /**
     * This function returns the [ListModel] record for the given [localSiteId] and [listDescriptor] if there is one.
     *
     * Since there shouldn't be more than one record for a [localSiteId] and [listDescriptor] combination,
     * [firstOrNull] is used.
     */
    fun getList(localSiteId: Int, listDescriptor: ListDescriptor): ListModel? {
        return WellSql.select(ListModel::class.java)
                .where()
                .equals(ListModelTable.LOCAL_SITE_ID, localSiteId)
                .equals(ListModelTable.TYPE, listDescriptor.type.value)
                .equals(ListModelTable.FILTER_DB_VALUE, listDescriptor.filter?.value)
                .equals(ListModelTable.ORDER_DB_VALUE, listDescriptor.order?.value)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    /**
     * This function deletes the [ListModel] record for the given [localSiteId] and [listDescriptor] if there is one.
     *
     * To ensure that we have the same `where` queries for both `select` and `delete` queries, [getList] is utilized.
     */
    fun deleteList(localSiteId: Int, listDescriptor: ListDescriptor) {
        val existing = getList(localSiteId, listDescriptor)
        existing?.let {
            WellSql.delete(ListModel::class.java).whereId(it.id)
        }
    }
}
