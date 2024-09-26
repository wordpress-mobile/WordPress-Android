package org.wordpress.android.fluxc.persistence

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfigValueSource.REMOTE

@Dao
abstract class RemoteConfigDao {
    @Transaction
    @Query("SELECT * from RemoteConfigurations")
    abstract fun getRemoteConfigList(): List<RemoteConfig>

    @Transaction
    @Query("SELECT * from RemoteConfigurations WHERE `key` = :key")
    abstract fun getRemoteConfig(key: String): List<RemoteConfig>

    @Transaction
    @Suppress("SpreadOperator")
    open fun insert(remoteFlags: Map<String, String>) {
        remoteFlags.forEach {
            insert(
                    RemoteConfig(
                            key = it.key,
                            value = it.value,
                            createdAt = System.currentTimeMillis(),
                            modifiedAt = System.currentTimeMillis(),
                            source = REMOTE
                    )
            )
        }
    }

    @Transaction
    @Query("DELETE FROM RemoteConfigurations")
    abstract fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(offer: RemoteConfig)

    @Entity(
            tableName = "RemoteConfigurations",
            primaryKeys = ["key"]
    )
    data class RemoteConfig(
        val key: String,
        val value: String,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "modified_at") val modifiedAt: Long,
        @ColumnInfo(name = "source") val source: RemoteConfigValueSource
    )

    enum class RemoteConfigValueSource(val value: Int) {
        BUILD_CONFIG(0),
        REMOTE(1),
    }

    class RemoteConfigValueConverter {
        @TypeConverter
        fun toRemoteConfigValueSource(value: Int): RemoteConfigValueSource =
                enumValues<RemoteConfigValueSource>()[value]

        @TypeConverter
        fun fromRemoteConfigValueSource(value: RemoteConfigValueSource): Int = value.ordinal
    }
}
