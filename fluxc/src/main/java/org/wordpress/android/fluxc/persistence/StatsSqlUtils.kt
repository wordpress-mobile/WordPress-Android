package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.wellsql.generated.StatsBlockTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import javax.inject.Singleton

const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"

@Singleton
class StatsSqlUtils
@Inject constructor() {
    private val gson: Gson by lazy {
        val builder = GsonBuilder()
        builder.setDateFormat(DATE_FORMAT)
        builder.create()
    }

    fun <T> insert(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        item: T,
        replaceExistingData: Boolean,
        date: String? = null,
        postId: Long? = null
    ) {
        val json = gson.toJson(item)
        if (replaceExistingData) {
            var deleteStatement = WellSql.delete(StatsBlockBuilder::class.java)
                    .where()
                    .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                    .equals(StatsBlockTable.BLOCK_TYPE, blockType.name)
                    .equals(StatsBlockTable.STATS_TYPE, statsType.name)
            if (date != null) {
                deleteStatement = deleteStatement.equals(StatsBlockTable.DATE, date)
            }
            if (postId != null) {
                deleteStatement = deleteStatement.equals(StatsBlockTable.POST_ID, postId)
            }
            deleteStatement.endWhere().execute()
        }
        WellSql.insert(
                StatsBlockBuilder(
                        localSiteId = site.id,
                        blockType = blockType.name,
                        statsType = statsType.name,
                        date = date,
                        postId = postId,
                        json = json
                )
        ).execute()
    }

    fun <T> selectAll(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): List<T> {
        val models = createSelectStatement(site, blockType, statsType, date, postId).asModel
        return models.map { gson.fromJson(it.json, classOfT) }
    }

    fun <T> select(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        classOfT: Class<T>,
        date: String? = null,
        postId: Long? = null
    ): T? {
        val model = createSelectStatement(site, blockType, statsType, date, postId).asModel.firstOrNull()
        if (model != null) {
            return gson.fromJson(model.json, classOfT)
        }
        return null
    }

    fun deleteAllStats(): Int {
        return WellSql.delete(StatsBlockBuilder::class.java).execute()
    }

    fun deleteSiteStats(site: SiteModel): Int {
        return WellSql.delete(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    private fun createSelectStatement(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        date: String?,
        postId: Long?
    ): SelectQuery<StatsBlockBuilder> {
        var select = WellSql.select(StatsBlockBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.BLOCK_TYPE, blockType.name)
                .equals(StatsBlockTable.STATS_TYPE, statsType.name)
        if (date != null) {
            select = select.equals(StatsBlockTable.DATE, date)
        }
        if (postId != null) {
            select = select.equals(StatsBlockTable.POST_ID, postId)
        }
        return select.endWhere()
    }

    @Table(name = "StatsBlock")
    data class StatsBlockBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var blockType: String,
        @Column var statsType: String,
        @Column var date: String?,
        @Column var postId: Long?,
        @Column var json: String
    ) : Identifiable {
        constructor() : this(-1, -1, "", "", null, null, "")

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
        DETAILED_POST_STATS,
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
        SEARCH_TERMS,
        VIDEO_PLAYS,
        PUBLICIZE_INSIGHTS,
        POSTING_ACTIVITY,
        FILE_DOWNLOADS
    }
}
