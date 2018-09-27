package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.wellsql.generated.StatsBlockTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.StatsStore.InsightsTypes
import org.wordpress.android.fluxc.store.StatsStore.TimeStatsTypes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSqlUtils
@Inject constructor(val gson: Gson) {
    fun <T> insertInsight(site: SiteModel, type: InsightsTypes, item: T) = insert(site, item, type.name)

    fun <T> insertTimeStats(site: SiteModel, type: TimeStatsTypes, item: T) = insert(site, item, type.name)

    private fun <T> insert(site: SiteModel, item: T, type: String) {
        val json = gson.toJson(item)
        WellSql.delete(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.TYPE, type)
                .endWhere()
                .execute()
        WellSql.insert(StatsBlockBuilder(localSiteId = site.id, type = type, json = json))
                .execute()
    }

    fun <T> selectInsight(site: SiteModel, type: InsightsTypes, classOfT: Class<T>): T? =
            select(site, type.name, classOfT)

    fun <T> selectTimeStats(site: SiteModel, type: TimeStatsTypes, classOfT: Class<T>): T? =
            select(site, type.name, classOfT)

    private fun <T> select(
        site: SiteModel,
        type: String,
        classOfT: Class<T>
    ): T? {
        val model = WellSql.select(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.TYPE, type)
                .endWhere().asModel.firstOrNull()
        if (model != null) {
            return gson.fromJson(model.json, classOfT)
        }
        return null
    }

    @Table(name = "StatsBlock")
    data class StatsBlockBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var type: String,
        @Column var json: String
    ) : Identifiable {
        constructor() : this(-1, -1, "", "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
