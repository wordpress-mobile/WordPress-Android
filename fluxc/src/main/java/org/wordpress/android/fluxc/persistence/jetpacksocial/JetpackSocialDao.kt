package org.wordpress.android.fluxc.persistence.jetpacksocial

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JetpackSocialDao {
    @Query("SELECT * FROM JetpackSocial WHERE siteLocalId = :siteLocalId")
    suspend fun getJetpackSocial(siteLocalId: Int): JetpackSocialEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(jetpackSocialEntity: JetpackSocialEntity)

    @Query("DELETE FROM JetpackSocial")
    suspend fun clear()

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
