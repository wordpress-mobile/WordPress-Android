package org.wordpress.android.fluxc.persistence.dashboard

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils

@Dao
abstract class CardsDao {
    @Query("SELECT * FROM DashboardCards WHERE siteLocalId = :siteLocalId")
    abstract fun get(siteLocalId: Int): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(card: List<CardEntity>)

    suspend fun insertWithDate(siteLocalId: Int, cards: List<CardModel>) {
        val insertDate = CardsUtils.getInsertDate()
        insert(cards.map { CardEntity.from(siteLocalId, it, insertDate) })
    }

    @Query("DELETE FROM DashboardCards")
    abstract fun clear()

    @Entity(
            tableName = "DashboardCards",
            primaryKeys = ["siteLocalId", "type"]
    )
    data class CardEntity(
        val siteLocalId: Int,
        val type: String,
        val date: String,
        val json: String
    ) {
        fun toCard() = CardsUtils.GSON.fromJson(json, CardModel.Type.valueOf(type).classOf) as CardModel

        companion object {
            fun from(
                siteLocalId: Int,
                card: CardModel,
                insertDate: String
            ) = CardEntity(
                    siteLocalId = siteLocalId,
                    type = card.type.name,
                    date = insertDate,
                    json = CardsUtils.GSON.toJson(card)
            )
        }
    }
}
