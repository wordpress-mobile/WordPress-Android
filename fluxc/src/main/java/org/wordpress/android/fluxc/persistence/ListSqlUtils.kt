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

    fun getList(siteModel: SiteModel, type: ListType?): ListModel? {
        if (type == null) return null
        return WellSql.select(ListModel::class.java)
                .where()
                .equals(ListModelTable.LOCAL_SITE_ID, siteModel.id)
                .equals(ListModelTable.TYPE, type.value)
                .endWhere()
                .asModel
                .firstOrNull()
    }
}
