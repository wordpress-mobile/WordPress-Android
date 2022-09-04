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
    abstract fun getRemoteConfigs(): List<RemoteConfig>

    @Transaction
    @Query("SELECT * from RemoteConfigurations WHERE `key` = :key")
    abstract fun getRemoteConfig(key: String): List<RemoteConfig>

    @Transaction
    @Suppress("SpreadOperator")
    open fun insertRemoteConfig(remoteConfigs: Map<String, Boolean>) {
        remoteConfigs.forEach {
            insertRemoteConfig(
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
    abstract fun clearRemoteConfig()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertRemoteConfig(offer: RemoteConfig)

    @Entity(
            tableName = "RemoteConfigurations",
            primaryKeys = ["key"]
    )
    data class RemoteConfig(
        val key: String,
        val value: Boolean,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "modified_at") val modifiedAt: Long,
        @ColumnInfo(name = "source") val source: RemoteConfigValueSource
    )

    enum class RemoteConfigValueSource(value: Int) {
        BUILD_CONFIG(0),
        REMOTE(1),
    }

    class RemoteConfigValueSourceConverter {
        @TypeConverter
        fun toRemoteConfigValueSource(value: Int): RemoteConfigValueSource =
                enumValues<RemoteConfigValueSource>()[value]

        @TypeConverter
        fun fromRemoteConfigValueSource(value: RemoteConfigValueSource): Int = value.ordinal
    }
}
