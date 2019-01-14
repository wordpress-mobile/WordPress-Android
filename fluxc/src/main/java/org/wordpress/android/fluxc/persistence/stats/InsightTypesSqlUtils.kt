package org.wordpress.android.fluxc.persistence.stats

import com.wellsql.generated.InsightTypesTable
import com.yarolegovich.wellsql.SelectQuery.ORDER_ASCENDING
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightTypeModel
import org.wordpress.android.fluxc.model.stats.InsightTypeModel.Status
import org.wordpress.android.fluxc.persistence.UpdateAllExceptId
import org.wordpress.android.fluxc.store.stats.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightTypesSqlUtils
@Inject constructor(private val formattableContentMapper: FormattableContentMapper) {

    fun getOrderedInsightTypesByStatus(site: SiteModel, status: InsightTypeModel.Status): List<InsightTypeModel> {
        return WellSql.select(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .equals(InsightTypesTable.STATUS, status.name)
                .endWhere()
                .orderBy(InsightTypesTable.POSITION, ORDER_ASCENDING)
                .asModel
                .map { it.build() }
    }

    fun getInsightTypes(site: SiteModel): List<InsightTypeModel> {
        return WellSql.select(InsightTypesBuilder::class.java)
                .where()
                .equals(InsightTypesTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
                .map { it.build() }
    }

    fun insertInsightTypes(site: SiteModel, insightsTypes: List<InsightTypeModel>) {
        WellSql.giveMeWritableDb().beginTransaction()
        WellSql.delete(InsightTypesBuilder::class.java).execute()
        val insertQuery = WellSql.insert(insightsTypes.map { it.toBuilder(site) })
        insertQuery.execute()
        WellSql.giveMeWritableDb().endTransaction()
    }

    private fun InsightTypeModel.toBuilder(site: SiteModel): InsightTypesBuilder {
        return InsightTypesBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                insightType = this.status.name,
                position = this.position ?: -1,
                status = this.status.name
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

        fun build(): InsightTypeModel {
            return InsightTypeModel(
                    InsightsTypes.valueOf(insightType),
                    Status.valueOf(status),
                    if (position >= 0) position else null
            )
        }
    }
}
