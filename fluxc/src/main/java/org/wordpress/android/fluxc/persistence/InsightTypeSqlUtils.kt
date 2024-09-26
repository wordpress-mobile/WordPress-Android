package org.wordpress.android.fluxc.persistence

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
import org.wordpress.android.fluxc.store.StatsStore.InsightType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightTypeSqlUtils
@Inject constructor() {
    fun selectAddedItemsOrderedByStatus(site: SiteModel): List<InsightType> {
        return selectItemsOrderedByStatus(site, ADDED)
    }

    fun selectRemovedItemsOrderedByStatus(site: SiteModel): List<InsightType> {
        return selectItemsOrderedByStatus(site, REMOVED)
    }

    private fun selectItemsOrderedByStatus(site: SiteModel, status: Status): List<InsightType> {
        return WellSql.select(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.STATUS, status.name)
                .endWhere()
                .orderBy(InsightTypesTable.POSITION, ORDER_ASCENDING)
                .asModel
                .map { InsightType.valueOf(it.insightType) }
    }

    fun insertOrReplaceAddedItems(site: SiteModel, insightTypes: List<InsightType>) {
        insertOrReplaceList(site, insightTypes, ADDED)
    }

    fun insertOrReplaceRemovedItems(site: SiteModel, insightTypes: List<InsightType>) {
        insertOrReplaceList(site, insightTypes, REMOVED)
    }

    private fun insertOrReplaceList(site: SiteModel, insightTypes: List<InsightType>, status: Status) {
        WellSql.delete(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.STATUS, status.name)
                .endWhere().execute()
        WellSql.insert(insightTypes.mapIndexed { index, type ->
            type.toBuilder(
                    site,
                    status,
                    index
            )
        }).execute()
    }

    private fun InsightType.toBuilder(site: SiteModel, status: Status, position: Int): InsightTypesBuilder {
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
                    InsightType.valueOf(insightType),
                    Status.valueOf(status),
                    if (position >= 0) position else null
            )
        }
    }
}
