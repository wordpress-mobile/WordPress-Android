package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.StockMediaPageTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.store.StockMediaItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMediaSqlUtils
@Inject constructor() {
    fun insert(
        page: Int,
        nextPage: Int?,
        items: List<StockMediaItem>
    ) {
        val writableDb = WellSql.giveMeWritableDb()
        writableDb.beginTransaction()
        try {
            WellSql.insert(StockMediaPageBuilder(page = page, nextPage = nextPage)).execute()
            WellSql.insert(
                    items.map {
                        StockMediaBuilder(
                                itemId = it.id,
                                name = it.name,
                                title = it.title,
                                url = it.url,
                                date = it.date,
                                thumbnail = it.thumbnail
                        )
                    }
            ).execute()
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    fun selectAll(): List<StockMediaItem> {
        return WellSql.select(StockMediaBuilder::class.java).asModel.map {
            StockMediaItem(
                    it.itemId,
                    it.name,
                    it.title,
                    it.url,
                    it.date,
                    it.thumbnail
            )
        }
    }

    fun getNextPage(): Int? {
        return WellSql.select(StockMediaPageBuilder::class.java)
                .orderBy(StockMediaPageTable.PAGE, SelectQuery.ORDER_DESCENDING).asModel.firstOrNull()?.nextPage
    }

    fun clear() {
        val writableDb = WellSql.giveMeWritableDb()
        writableDb.beginTransaction()
        try {
            WellSql.delete(StockMediaBuilder::class.java).execute()
            WellSql.delete(StockMediaPageBuilder::class.java).execute()
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    @Table(name = "StockMediaPage")
    data class StockMediaPageBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var page: Int,
        @Column var nextPage: Int?
    ) : Identifiable {
        constructor() : this(-1, -1, null)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }

    @Table(name = "StockMedia")
    data class StockMediaBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var itemId: String?,
        @Column var name: String?,
        @Column var title: String?,
        @Column var url: String?,
        @Column var date: String?,
        @Column var thumbnail: String?
    ) : Identifiable {
        constructor() : this(-1, null, null, null, null, null, null)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
