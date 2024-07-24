package org.wordpress.android.fluxc.persistence

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction

@Dao
abstract class PlanOffersDao {
    @Transaction
    @Suppress("SpreadOperator")
    open fun insertPlanOfferWithDetails(vararg planOfferWithDetails: PlanOfferWithDetails) {
        planOfferWithDetails.forEach {
            this.insertPlanOffer(it.planOffer)
            this.insertPlanOfferIds(*it.planIds.toTypedArray())
            this.insertPlanOfferFeatures(*it.planFeatures.toTypedArray())
        }
    }

    @Transaction
    @Query("SELECT * from PlanOffers")
    abstract fun getPlanOfferWithDetails(): List<PlanOfferWithDetails>

    @Transaction
    @Query("DELETE FROM PlanOffers")
    abstract fun clearPlanOffers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertPlanOffer(offer: PlanOffer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertPlanOfferIds(vararg planOfferIds: PlanOfferId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertPlanOfferFeatures(vararg features: PlanOfferFeature)

    @Entity(
            tableName = "PlanOffers",
            indices = [Index(
                    value = ["internalPlanId"],
                    unique = true
            )]
    )
    data class PlanOffer(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val internalPlanId: Int = 0,
        val name: String? = null,
        val shortName: String? = null,
        val tagline: String? = null,
        val description: String? = null,
        val icon: String? = null
    )

    @Entity(
            tableName = "PlanOfferIds",
            foreignKeys = [ForeignKey(
                    entity = PlanOffer::class,
                    parentColumns = ["internalPlanId"],
                    childColumns = ["internalPlanId"],
                    onDelete = CASCADE
            )]
    )
    data class PlanOfferId(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val productId: Int = 0,
        val internalPlanId: Int = 0
    )

    @Entity(
            tableName = "PlanOfferFeatures",
            foreignKeys = [ForeignKey(
                    entity = PlanOffer::class,
                    parentColumns = ["internalPlanId"],
                    childColumns = ["internalPlanId"],
                    onDelete = CASCADE
            )]
    )
    data class PlanOfferFeature(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        val internalPlanId: Int = 0,
        val stringId: String? = null,
        val name: String? = null,
        val description: String? = null
    )

    data class PlanOfferWithDetails(
        @Embedded
        val planOffer: PlanOffer,

        @Relation(parentColumn = "internalPlanId", entityColumn = "internalPlanId")
        val planIds: List<PlanOfferId> = emptyList(),

        @Relation(parentColumn = "internalPlanId", entityColumn = "internalPlanId")
        val planFeatures: List<PlanOfferFeature> = emptyList()
    )
}
