package org.wordpress.android.fluxc.persistence.blaze

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignsModel
import org.wordpress.android.fluxc.persistence.coverters.BlazeCampaignsDateConverter
import java.util.Date

@Dao
abstract class BlazeCampaignsDao {
    @Transaction
    open suspend fun getCachedCampaigns(siteId: Long): List<BlazeCampaignModel> {
        val campaigns = getCampaigns(siteId)
        return campaigns.map { it.toDomainModel() }
    }

    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY CAST(campaignId AS int) DESC")
    abstract fun getCampaigns(siteId: Long): List<BlazeCampaignEntity>

    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY CAST(campaignId AS int) DESC")
    abstract fun observeCampaigns(siteId: Long): Flow<List<BlazeCampaignEntity>>

    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY CAST(campaignId AS int) DESC LIMIT 1")
    abstract fun getMostRecentCampaignForSite(siteId: Long): BlazeCampaignEntity?

    @Query("SELECT * from BlazeCampaigns WHERE `siteId` = :siteId ORDER BY CAST(campaignId AS int) DESC LIMIT 1")
    abstract fun observeMostRecentCampaignForSite(siteId: Long): Flow<BlazeCampaignEntity?>

    @Transaction
    open suspend fun insertCampaigns(siteId: Long, domainModel: BlazeCampaignsModel) {
        if (domainModel.skipped == 0) {
            clearBlazeCampaigns(siteId)
        }
        insert(domainModel.campaigns.map { BlazeCampaignEntity.fromDomainModel(siteId, it) })
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(campaigns: List<BlazeCampaignEntity>)

    @Query("DELETE FROM BlazeCampaigns where siteId = :siteId")
    abstract fun clearBlazeCampaigns(siteId: Long)

    @Entity(
        tableName = "BlazeCampaigns",
        indices = [Index(value = ["siteId"], unique = false)],
        primaryKeys = ["siteId", "campaignId"]
    )
    @TypeConverters(BlazeCampaignsDateConverter::class)
    data class BlazeCampaignEntity(
        val siteId: Long,
        val campaignId: String,
        val title: String,
        val imageUrl: String?,
        val startTime: Date,
        val durationInDays: Int,
        val uiStatus: String,
        val impressions: Long,
        val clicks: Long,
        val targetUrn: String?,
        val totalBudget: Double,
        val spentBudget: Double,
        @ColumnInfo(defaultValue = "0")
        val isEndlessCampaign: Boolean
    ) {
        fun toDomainModel() = BlazeCampaignModel(
            campaignId = campaignId,
            title = title,
            imageUrl = imageUrl,
            startTime = startTime,
            durationInDays = durationInDays,
            uiStatus = uiStatus,
            impressions = impressions,
            clicks = clicks,
            targetUrn = targetUrn,
            totalBudget = totalBudget,
            spentBudget = spentBudget,
            isEndlessCampaign = isEndlessCampaign
        )

        companion object {
            fun fromDomainModel(
                siteId: Long,
                campaign: BlazeCampaignModel
            ) = BlazeCampaignEntity(
                siteId = siteId,
                campaignId = campaign.campaignId,
                title = campaign.title,
                imageUrl = campaign.imageUrl,
                startTime = campaign.startTime,
                durationInDays = campaign.durationInDays,
                uiStatus = campaign.uiStatus,
                impressions = campaign.impressions,
                clicks = campaign.clicks,
                targetUrn = campaign.targetUrn,
                totalBudget = campaign.totalBudget,
                spentBudget = campaign.spentBudget,
                isEndlessCampaign = campaign.isEndlessCampaign
            )
        }
    }
}
