package org.wordpress.android.fluxc.persistence.blaze

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.blaze.BlazeStatusModel

@Dao
abstract class BlazeStatusDao {
    @Transaction
    @Query("SELECT * from BlazeStatus WHERE `siteId` = :siteId")
    abstract fun getBlazeStatus(siteId: Long): Flow<List<BlazeStatus>>

    @Transaction
    @Suppress("SpreadOperator")
    open fun insert(siteId: Long, isEligible: Boolean) {
        insert(
            BlazeStatus(
                siteId = siteId,
                isEligible = isEligible
            )
        )
    }

    @Transaction
    @Query("DELETE FROM BlazeStatus")
    abstract fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(blazeStatus: BlazeStatus)

    @Entity(
        tableName = "BlazeStatus",
        primaryKeys = ["siteId"]
    )
    data class BlazeStatus(
        val siteId: Long,
        val isEligible: Boolean
    ) {
        fun toBlazeModel() = BlazeStatusModel(
            siteId = siteId,
            isEligible = isEligible
        )
    }
}
