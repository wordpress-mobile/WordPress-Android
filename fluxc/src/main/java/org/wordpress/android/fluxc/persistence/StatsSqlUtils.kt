package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.wellsql.generated.StatsBlockTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsSqlUtils
@Inject constructor(private val gson: Gson) {
    fun <T> insert(site: SiteModel, type: Key, item: T) {
        val json = gson.toJson(item)
        WellSql.delete(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.TYPE, type.name)
                .endWhere()
                .execute()
        WellSql.insert(StatsBlockBuilder(localSiteId = site.id, type = type.name, json = json))
                .execute()
    }

    fun <T> select(site: SiteModel, type: Key, classOfT: Class<T>): T? {
        val model = WellSql.select(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.TYPE, type.name)
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

    enum class BlockType {
        ALL_TIME_INSIGHTS,
        MOST_POPULAR_INSIGHTS,
        LATEST_POST_DETAIL_INSIGHTS,
        LATEST_POST_STATS_INSIGHTS,
        TODAYS_INSIGHTS,
        WP_COM_FOLLOWERS,
        EMAIL_FOLLOWERS,
        COMMENTS_INSIGHTS,
        TAGS_AND_CATEGORIES_INSIGHTS
    }
}
