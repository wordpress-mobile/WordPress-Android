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
    fun <T> insert(site: SiteModel, blockType: BlockType, statsType: StatsType, item: T, date: String = "") {
        val json = gson.toJson(item)
        WellSql.delete(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.BLOCK_TYPE, blockType.name)
                .equals(StatsBlockTable.STATS_TYPE, statsType.name)
                .equals(StatsBlockTable.DATE, date)
                .endWhere()
                .execute()
        WellSql.insert(
                StatsBlockBuilder(
                        localSiteId = site.id,
                        blockType = blockType.name,
                        statsType = statsType.name,
                        date = date,
                        json = json
                )
        )
                .execute()
    }

    fun <T> select(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String = ""
    ): T? {
        val model = WellSql.select(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.BLOCK_TYPE, blockType.name)
                .equals(StatsBlockTable.STATS_TYPE, statsType.name)
                .equals(StatsBlockTable.DATE, date)
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
        @Column var blockType: String,
        @Column var statsType: String,
        @Column var date: String,
        @Column var json: String
    ) : Identifiable {
        constructor() : this(-1, -1, "", "", "", "")

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    enum class StatsType {
        INSIGHTS,
        DAY,
        WEEK,
        MONTH,
        YEAR
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
        TAGS_AND_CATEGORIES_INSIGHTS,
        POSTS_AND_PAGES_VIEWS,
        REFERRERS,
        CLICKS,
        VISITS_AND_VIEWS,
        COUNTRY_VIEWS,
        AUTHORS,
        PUBLICIZE_INSIGHTS
    }
}
