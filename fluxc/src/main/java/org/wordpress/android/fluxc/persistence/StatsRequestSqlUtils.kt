package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.StatsBlockTable
import com.wellsql.generated.StatsRequestTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRequestSqlUtils @Inject constructor() {
    fun insert(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        requestedItems: Int? = null,
        date: String? = null,
        postId: Long? = null
    ) {
        var deleteStatement = WellSql.delete(StatsRequestBuilder::class.java)
                .where()
                .equals(StatsBlockTable.LOCAL_SITE_ID, site.id)
                .equals(StatsBlockTable.BLOCK_TYPE, blockType.name)
                .equals(StatsBlockTable.STATS_TYPE, statsType.name)
        if (date != null) {
            deleteStatement = deleteStatement.equals(StatsBlockTable.DATE, date)
        }
        deleteStatement.endWhere().execute()
        val timeStamp = System.currentTimeMillis()
        WellSql.insert(
                StatsRequestBuilder(
                        localSiteId = site.id,
                        blockType = blockType.name,
                        statsType = statsType.name,
                        date = date,
                        postId = postId,
                        timeStamp = timeStamp,
                        requestedItems = requestedItems
                )
        ).execute()
    }

    fun hasFreshRequest(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        requestedItems: Int? = null,
        after: Long = System.currentTimeMillis() - STALE_PERIOD,
        date: String? = null,
        postId: Long? = null
    ): Boolean {
        return createSelectStatement(
                site,
                blockType,
                statsType,
                date,
                postId,
                after,
                requestedItems
        ).asModel.firstOrNull<StatsRequestBuilder?>() != null
    }

    @Suppress("LongParameterList")
    private fun createSelectStatement(
        site: SiteModel,
        blockType: BlockType,
        statsType: StatsType,
        date: String?,
        postId: Long?,
        after: Long,
        requestedItems: Int? = null
    ): SelectQuery<StatsRequestBuilder> {
        var select = WellSql.select(StatsRequestBuilder::class.java)
                .where()
                .equals(StatsRequestTable.LOCAL_SITE_ID, site.id)
                .equals(StatsRequestTable.BLOCK_TYPE, blockType.name)
                .equals(StatsRequestTable.STATS_TYPE, statsType.name)
                .greaterThen(StatsRequestTable.TIME_STAMP, after)
        if (requestedItems != null) {
            select = select.greaterThenOrEqual(StatsRequestTable.REQUESTED_ITEMS, requestedItems)
        }
        if (date != null) {
            select = select.equals(StatsRequestTable.DATE, date)
        }
        if (postId != null) {
            select = select.equals(StatsRequestTable.POST_ID, postId)
        }
        return select.endWhere()
    }

    @Table(name = "StatsRequest")
    data class StatsRequestBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var blockType: String,
        @Column var statsType: String,
        @Column var date: String?,
        @Column var postId: Long?,
        @Column var timeStamp: Long,
        @Column var requestedItems: Int?
    ) : Identifiable {
        constructor() : this(-1, -1, "", "", null, null, 0, null)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    companion object {
        private const val STALE_PERIOD = 5 * 60 * 1000
    }
}
