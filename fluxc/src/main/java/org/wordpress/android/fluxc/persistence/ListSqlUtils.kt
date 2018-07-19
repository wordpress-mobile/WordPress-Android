package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.ListModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.ListModel
import org.wordpress.android.fluxc.model.ListModel.ListType
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListSqlUtils @Inject constructor() {
    /**
     * This function either creates a new [ListModel] for the [siteModel] and [listType] or updates the existing record.
     *
     * If there is an existing record, only the [ListModel.lastModified] will be updated with the current [Date].
     * If there is no existing record, a new [ListModel] will be created for [siteModel] and [listType]. The current
     * [Date] will be assigned to both [ListModel.dateCreated] and [ListModel.lastModified].
     */
    fun insertOrUpdateList(siteModel: SiteModel, listType: ListType) {
        val now = DateTimeUtils.iso8601FromDate(Date())
        val listModel = ListModel()
        listModel.lastModified = now

        val existing = getList(siteModel, listType)
        if (existing != null) {
            WellSql.update<ListModel>(ListModel::class.java)
                    .whereId(existing.id)
                    .put(listModel) { item ->
                        val cv = ContentValues()
                        cv.put(ListModelTable.LAST_MODIFIED, item.lastModified)
                        cv
                    }.execute()
        } else {
            listModel.type = listType.value
            listModel.localSiteId = siteModel.id
            listModel.dateCreated = now
            WellSql.insert(listModel).execute()
        }
    }

    /**
     * This function returns the [ListModel] record for the given [siteModel] and [type] if there is one.
     *
     * Since there shouldn't be more than one record for a [siteModel] and [type] combination, [firstOrNull] is used.
     */
    fun getList(siteModel: SiteModel, type: ListType): ListModel? {
        return WellSql.select(ListModel::class.java)
                .where()
                .equals(ListModelTable.LOCAL_SITE_ID, siteModel.id)
                .equals(ListModelTable.TYPE, type.value)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    /**
     * This function deletes the [ListModel] record for the given [siteModel] and [type] if there is one.
     *
     * To ensure that we have the same `where` queries for both `select` and `delete` queries, [getList] is utilized.
     */
    fun deleteList(siteModel: SiteModel, type: ListType) {
        val existing = getList(siteModel, type)
        existing?.let {
            WellSql.delete(ListModel::class.java).whereId(it.id)
        }
    }
}
