package org.wordpress.android.fluxc.persistence

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BloggingRemindersDao {
    @Query("SELECT * FROM BloggingReminders")
    fun getAll(): Flow<List<BloggingReminders>>

    @Query("SELECT * FROM BloggingReminders WHERE localSiteId = :siteId")
    fun liveGetBySiteId(siteId: Int): Flow<BloggingReminders?>

    @Query("SELECT * FROM BloggingReminders WHERE localSiteId = :siteId")
    suspend fun getBySiteId(siteId: Int): List<BloggingReminders>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: BloggingReminders): Long

    @Entity(tableName = "BloggingReminders")
    data class BloggingReminders(
        @PrimaryKey
        val localSiteId: Int,
        val monday: Boolean = false,
        val tuesday: Boolean = false,
        val wednesday: Boolean = false,
        val thursday: Boolean = false,
        val friday: Boolean = false,
        val saturday: Boolean = false,
        val sunday: Boolean = false,
        val hour: Int = 10,
        val minute: Int = 0,
        val isPromptRemindersOptedIn: Boolean = false,
        @ColumnInfo(defaultValue = "1") val isPromptsCardOptedIn: Boolean = true,
    )
}
