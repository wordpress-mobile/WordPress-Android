package org.wordpress.android.fluxc.persistence

import android.content.ContentValues
import com.wellsql.generated.InsightTypesTable
import com.yarolegovich.wellsql.SelectQuery.ORDER_ASCENDING
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightTypeDataModel
import org.wordpress.android.fluxc.model.stats.InsightTypeDataModel.Status
import org.wordpress.android.fluxc.model.stats.InsightTypeDataModel.Status.ADDED
import org.wordpress.android.fluxc.model.stats.InsightTypeDataModel.Status.REMOVED
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightTypesSqlUtils
@Inject constructor() {
    fun selectAddedItemsOrderedByStatus(site: SiteModel): List<InsightsTypes> {
        return selectItemsOrderedByStatus(site, ADDED)
    }

    fun selectRemovedItemsOrderedByStatus(site: SiteModel): List<InsightsTypes> {
        return selectItemsOrderedByStatus(site, REMOVED)
    }

    private fun selectItemsOrderedByStatus(site: SiteModel, status: InsightTypeDataModel.Status): List<InsightsTypes> {
        return WellSql.select(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.STATUS, status.name)
                .endWhere()
                .orderBy(InsightTypesTable.POSITION, ORDER_ASCENDING)
                .asModel
                .map { InsightsTypes.valueOf(it.insightType) }
    }

    fun insertOrReplaceAddedItems(site: SiteModel, insightsTypes: List<InsightsTypes>) {
        insertOrReplaceList(site, insightsTypes, ADDED)
    }

    fun insertOrReplaceRemovedItems(site: SiteModel, insightsTypes: List<InsightsTypes>) {
        insertOrReplaceList(site, insightsTypes, REMOVED)
    }

    private fun insertOrReplaceList(site: SiteModel, insightsTypes: List<InsightsTypes>, status: Status) {
        WellSql.delete(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.STATUS, status.name)
                .endWhere().execute()
        WellSql.insert(insightsTypes.mapIndexed { index, type ->
            type.toBuilder(
                    site,
                    status,
                    index
            )
        }).execute()
    }

    fun updateStatus(site: SiteModel, type: InsightsTypes, status: Status = REMOVED, position: Int = -1) {
        WellSql.update(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.INSIGHT_TYPE, type.name)
                .endWhere()
                .put(status.name) {
                    val cv = ContentValues()
                    cv.put(InsightTypesTable.STATUS, it)
                    cv.put(InsightTypesTable.POSITION, position)
                    cv
                }
    }

    private fun InsightsTypes.toBuilder(site: SiteModel, status: Status, position: Int): InsightTypesBuilder {
        return InsightTypesBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                insightType = this.name,
                position = if (status == ADDED) position else -1,
                status = status.name
        )
    }

    @Table(name = "InsightTypes")
    data class InsightTypesBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var insightType: String,
        @Column var position: Int,
        @Column var status: String
    ) : Identifiable {
        constructor() : this(-1, 0, 0, "", -1, "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun build(): InsightTypeDataModel {
            return InsightTypeDataModel(
                    InsightsTypes.valueOf(insightType),
                    Status.valueOf(status),
                    if (position >= 0) position else null
            )
        }
    }
}
