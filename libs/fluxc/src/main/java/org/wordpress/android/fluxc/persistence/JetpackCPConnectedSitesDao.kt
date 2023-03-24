package org.wordpress.android.fluxc.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.SiteModel

@Dao
interface JetpackCPConnectedSitesDao {
    @Query("SELECT COUNT(*) FROM JetpackCPConnectedSites")
    suspend fun getCount(): Int

    @Query("SELECT * FROM JetpackCPConnectedSites")
    suspend fun getAll(): List<JetpackCPConnectedSiteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sites: List<JetpackCPConnectedSiteEntity>)

    @Query("DELETE FROM JetpackCPConnectedSites")
    suspend fun deleteAll()

    @Entity(
        tableName = "JetpackCPConnectedSites",
        primaryKeys = ["remoteSiteId"]
    )
    data class JetpackCPConnectedSiteEntity(
        val remoteSiteId: Long? = null,
        val localSiteId: Int,
        val url: String,
        val name: String,
        val description: String,
        val activeJetpackConnectionPlugins: String,
    ) {
        fun toJetpackCPConnectedSite(): JetpackCPConnectedSiteModel = JetpackCPConnectedSiteModel(
            remoteSiteId = remoteSiteId,
            localSiteId = localSiteId,
            url = url,
            name = name,
            description = description,
            activeJetpackConnectionPlugins = activeJetpackConnectionPlugins.split(","),
        )

        companion object {
            fun from(
                jetpackConnectedSite: JetpackCPConnectedSiteModel
            ): JetpackCPConnectedSiteEntity = jetpackConnectedSite.run {
                JetpackCPConnectedSiteEntity(
                    remoteSiteId = remoteSiteId,
                    localSiteId = localSiteId,
                    url = url,
                    name = name,
                    description = description,
                    activeJetpackConnectionPlugins = activeJetpackConnectionPlugins
                        .joinToString(","),
                )
            }

            fun from(
                siteModel: SiteModel
            ): JetpackCPConnectedSiteEntity? = siteModel
                .takeIf { it.isJetpackCPConnected }
                ?.run {
                    JetpackCPConnectedSiteEntity(
                        remoteSiteId = remoteId().value,
                        localSiteId = localId().value,
                        url = url,
                        name = name.orEmpty(),
                        description = description.orEmpty(),
                        activeJetpackConnectionPlugins = activeJetpackConnectionPlugins.orEmpty()
                    )
                }
        }
    }
}

data class JetpackCPConnectedSiteModel(
    val remoteSiteId: Long? = null,
    val localSiteId: Int,
    val url: String,
    val name: String,
    val description: String,
    val activeJetpackConnectionPlugins: List<String>,
)
