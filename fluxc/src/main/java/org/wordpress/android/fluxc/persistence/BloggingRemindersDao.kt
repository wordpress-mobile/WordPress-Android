package org.wordpress.android.fluxc.persistence

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
abstract class BloggingRemindersDao {
    @Query("SELECT * FROM BloggingReminders")
    abstract fun getAll(): Flow<List<BloggingReminders>>

    @Query("SELECT * FROM BloggingReminders WHERE localSiteId = :siteId")
    abstract fun liveGetBySiteId(siteId: Int): Flow<BloggingReminders?>

    @Query("SELECT * FROM BloggingReminders WHERE localSiteId = :siteId")
    abstract suspend fun getBySiteId(siteId: Int): List<BloggingReminders>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(type: BloggingReminders): Long

    @Entity(tableName = "BloggingReminders")
    data class BloggingReminders(
        @PrimaryKey
        var localSiteId: Int,
        var monday: Boolean = false,
        var tuesday: Boolean = false,
        var wednesday: Boolean = false,
        var thursday: Boolean = false,
        var friday: Boolean = false,
        var saturday: Boolean = false,
        var sunday: Boolean = false,
        var hour: Int = 10,
        var minute: Int = 0
    )
}
