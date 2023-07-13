package org.wordpress.android.fluxc.persistence.jetpacksocial

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class JetpackSocialDao {
    @Query("SELECT * FROM JetpackSocial WHERE siteLocalId = :siteLocalId")
    abstract fun getJetpackSocial(siteLocalId: Int): Flow<JetpackSocialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(jetpackSocialEntity: JetpackSocialEntity)

    @Query("DELETE FROM JetpackSocial")
    abstract fun clear()

    @Entity(
        tableName = "JetpackSocial",
        primaryKeys = ["siteLocalId"]
    )
    data class JetpackSocialEntity(
        val siteLocalId: Int,
        val isShareLimitEnabled: Boolean,
        val toBePublicizedCount: Int,
        val shareLimit: Int,
        val publicizedCount: Int,
        val sharedPostsCount: Int,
        val sharesRemaining: Int,
        val isEnhancedPublishingEnabled: Boolean,
        val isSocialImageGeneratorEnabled: Boolean,
    )
}
