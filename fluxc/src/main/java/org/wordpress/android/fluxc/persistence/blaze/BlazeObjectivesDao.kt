package org.wordpress.android.fluxc.persistence.blaze

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignObjective

@Dao
interface BlazeObjectivesDao {
    @Query("SELECT * FROM BlazeCampaignObjectives WHERE locale = :locale")
    fun observeObjectives(locale: String): Flow<List<BlazeCampaignObjectiveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectives(topics: List<BlazeCampaignObjectiveEntity>)

    @Query("DELETE FROM BlazeCampaignObjectives")
    suspend fun deleteObjectives()

    @Transaction
    suspend fun replaceObjectives(objectives: List<BlazeCampaignObjectiveEntity>) {
        deleteObjectives()
        insertObjectives(objectives)
    }

    @Entity(tableName = "BlazeCampaignObjectives")
    data class BlazeCampaignObjectiveEntity(
        @PrimaryKey val id: String,
        val title: String,
        val description: String,
        val suitableForDescription: String,
        val locale: String
    ) {
        fun toDomainModel() = BlazeCampaignObjective(id, title, description, suitableForDescription)
    }
}
