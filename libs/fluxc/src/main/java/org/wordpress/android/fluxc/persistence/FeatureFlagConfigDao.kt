package org.wordpress.android.fluxc.persistence

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSource.REMOTE

@Dao
abstract class FeatureFlagConfigDao {
    @Transaction
    @Query("SELECT * from FeatureFlagConfigurations")
    abstract fun getFeatureFlagList(): List<FeatureFlag>

    @Transaction
    @Query("SELECT * from FeatureFlagConfigurations WHERE `key` = :key")
    abstract fun getFeatureFlag(key: String): List<FeatureFlag>

    @Transaction
    @Suppress("SpreadOperator")
    open fun insert(featureFlags: Map<String, Boolean>) {
        featureFlags.forEach {
            insert(
                    FeatureFlag(
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
    @Query("DELETE FROM FeatureFlagConfigurations")
    abstract fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(offer: FeatureFlag)

    @Entity(
            tableName = "FeatureFlagConfigurations",
            primaryKeys = ["key"]
    )
    data class FeatureFlag(
        val key: String,
        val value: Boolean,
        @ColumnInfo(name = "created_at") val createdAt: Long,
        @ColumnInfo(name = "modified_at") val modifiedAt: Long,
        @ColumnInfo(name = "source") val source: FeatureFlagValueSource
    )

    enum class FeatureFlagValueSource(value: Int) {
        BUILD_CONFIG(0),
        REMOTE(1),
    }

    class FeatureFlagValueSourceConverter {
        @TypeConverter
        fun toFeatureFlagValueSource(value: Int): FeatureFlagValueSource =
                enumValues<FeatureFlagValueSource>()[value]

        @TypeConverter
        fun fromFeatureFlagValueSource(value: FeatureFlagValueSource): Int = value.ordinal
    }
}
