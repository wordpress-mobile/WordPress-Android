package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.DynamicCardTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.fluxc.persistence.DynamicCardSqlUtils.DynamicCardState.PINNED
import org.wordpress.android.fluxc.persistence.DynamicCardSqlUtils.DynamicCardState.REMOVED
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicCardSqlUtils
@Inject constructor() {
    fun pin(
        siteId: Int,
        cardType: DynamicCardType
    ) {
        WellSql.insert(
                DynamicCardBuilder(
                        siteId = siteId,
                        dynamicCardType = cardType.toString(),
                        state = PINNED.toString()
                )
        ).execute()
    }

    fun unpin(
        siteId: Int
    ) {
        WellSql.delete(DynamicCardBuilder::class.java)
                .where()
                .equals(DynamicCardTable.SITE_ID, siteId)
                .equals(DynamicCardTable.STATE, PINNED.toString())
                .endWhere()
                .execute()
    }

    fun remove(
        siteId: Int,
        cardType: DynamicCardType
    ) {
        WellSql.insert(
                DynamicCardBuilder(
                        siteId = siteId,
                        dynamicCardType = cardType.toString(),
                        state = REMOVED.toString()
                )
        ).execute()
    }

    fun selectPinned(siteId: Int): DynamicCardType? {
        return WellSql.select(DynamicCardBuilder::class.java)
                .where()
                .equals(DynamicCardTable.SITE_ID, siteId)
                .equals(DynamicCardTable.STATE, PINNED.toString())
                .endWhere()
                .asModel.mapNotNull { builder ->
                    builder.dynamicCardType?.let { DynamicCardType.valueOf(it) }
                }.firstOrNull()
    }

    fun selectRemoved(siteId: Int): List<DynamicCardType> {
        return WellSql.select(DynamicCardBuilder::class.java)
                .where()
                .equals(DynamicCardTable.SITE_ID, siteId)
                .equals(DynamicCardTable.STATE, REMOVED.toString())
                .endWhere()
                .asModel.mapNotNull { builder ->
                    builder.dynamicCardType?.let { DynamicCardType.valueOf(it) }
                }
    }

    fun clear() {
        WellSql.delete(DynamicCardBuilder::class.java).execute()
    }

    private enum class DynamicCardState {
        PINNED, REMOVED
    }

    @Table(name = "DynamicCard")
    data class DynamicCardBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var siteId: Int?,
        @Column var dynamicCardType: String?,
        @Column var state: String?
    ) : Identifiable {
        constructor() : this(-1, null, null, null)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId
    }
}
