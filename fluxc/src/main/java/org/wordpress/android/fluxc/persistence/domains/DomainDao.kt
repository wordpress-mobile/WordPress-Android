package org.wordpress.android.fluxc.persistence.domains

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.DomainModel
import org.wordpress.android.fluxc.persistence.domains.DomainDao.DomainEntity

/**
 * DAO for [DomainEntity] access
 */
@Dao
abstract class DomainDao {
    @Transaction
    @Query("SELECT * from Domains WHERE `siteLocalId` = :siteLocalId")
    abstract fun getDomains(siteLocalId: Int): Flow<List<DomainEntity>>

    suspend fun insert(siteLocalId: Int, domains: List<DomainModel>) {
        insert(domains.map {
            DomainEntity(
                    siteLocalId = siteLocalId,
                    domain = it.domain,
                    primaryDomain = it.primaryDomain,
                    wpcomDomain = it.wpcomDomain
            )
        })
    }

    @Transaction
    @Query("DELETE FROM Domains")
    abstract fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(domainEntity: List<DomainEntity>)

    @Entity(
            tableName = "Domains",
            primaryKeys = ["domain"]
    )
    data class DomainEntity(
        val siteLocalId: Int,
        val domain: String,
        val primaryDomain: Boolean,
        val wpcomDomain: Boolean
    ) {
        fun toDomainModel() = DomainModel(
                domain = domain,
                primaryDomain = primaryDomain,
                wpcomDomain = wpcomDomain
        )
    }
}
