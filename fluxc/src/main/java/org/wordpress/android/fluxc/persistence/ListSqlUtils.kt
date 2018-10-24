package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.ListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
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
            listModel.descriptorUniqueIdentifierDbValue = listDescriptor.uniqueIdentifier.value
            listModel.descriptorTypeIdentifierDbValue = listDescriptor.typeIdentifier.value
            WellSql.insert(listModel).execute()
        }
    }

    /**
     * This function returns the [ListModel] record for the given [listDescriptor] if there is one.
     */
    fun getList(listDescriptor: ListDescriptor): ListModel? {
        return WellSql.select(ListModel::class.java)
                .where()
                .equals(ListModelTable.DESCRIPTOR_UNIQUE_IDENTIFIER_DB_VALUE, listDescriptor.uniqueIdentifier.value)
                // Checking the type identifier shouldn't be necessary since we have a unique value, but if we don't
                // implement perfect hash for the unique value, we can use this approach to even lower the chance
                // of collisions for ListDescriptor values.
                .equals(ListModelTable.DESCRIPTOR_TYPE_IDENTIFIER_DB_VALUE, listDescriptor.typeIdentifier.value)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    /**
     * This function returns all [ListModel] records that matches the given [ListDescriptorTypeIdentifier].
     */
    fun getListsWithTypeIdentifier(descriptorTypeIdentifier: ListDescriptorTypeIdentifier): List<ListModel> {
        return WellSql.select(ListModel::class.java)
                .where()
                .equals(ListModelTable.DESCRIPTOR_TYPE_IDENTIFIER_DB_VALUE, descriptorTypeIdentifier.value)
                .endWhere()
                .asModel
    }

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

    /**
     * This function deletes [ListModel] records that hasn't been updated for the given [expirationDuration].
     */
    fun deleteExpiredLists(expirationDuration: Long) {
        val allLists = WellSql.select(ListModel::class.java).asModel
        val cutOffDate = Date(System.currentTimeMillis() - expirationDuration)
        // Find the ids of lists that are expired
        val listIdsToDelete = allLists.asSequence().filter {
            DateTimeUtils.dateFromIso8601(it.lastModified).before(cutOffDate)
        }.map { it.id }.toList()
        if (listIdsToDelete.isNotEmpty()) {
            WellSql.delete(ListModel::class.java)
                    .where().isIn(ListModelTable.ID, listIdsToDelete).endWhere()
                    .execute()
        }
    }

    /**
     * This function deletes all [ListModel] records from the DB.
     */
    fun deleteAllLists() {
        WellSql.delete(ListModel::class.java).execute()
    }
}
