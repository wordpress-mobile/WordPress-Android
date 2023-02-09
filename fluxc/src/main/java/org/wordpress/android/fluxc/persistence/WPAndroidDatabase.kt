package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOffer
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferFeature
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferId
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfig
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeStatusDao.BlazeStatus
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.persistence.coverters.StringListConverter
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao.CardEntity

@Database(
        version = 12,
        entities = [
            BloggingReminders::class,
            PlanOffer::class,
            PlanOfferId::class,
            PlanOfferFeature::class,
            CommentEntity::class,
            CardEntity::class,
            BloggingPromptEntity::class,
            FeatureFlag::class,
            RemoteConfig::class,
            BlazeStatus::class
        ],
        autoMigrations = [
            AutoMigration(from = 11, to = 12)
        ]
)
@TypeConverters(
    value = [
        StringListConverter::class
    ]
)
abstract class WPAndroidDatabase : RoomDatabase() {
    abstract fun bloggingRemindersDao(): BloggingRemindersDao

    abstract fun planOffersDao(): PlanOffersDao

    abstract fun commentsDao(): CommentsDao

    abstract fun dashboardCardsDao(): CardsDao

    abstract fun bloggingPromptsDao(): BloggingPromptsDao

    abstract fun featureFlagConfigDao(): FeatureFlagConfigDao

    abstract fun remoteConfigDao(): RemoteConfigDao

    abstract fun blazeStatusDao(): BlazeStatusDao

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val WP_DB_NAME = "wp-android-database"

        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
            applicationContext,
            WPAndroidDatabase::class.java,
            WP_DB_NAME
        )
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .addMigrations(MIGRATION_3_4)
                .addMigrations(MIGRATION_5_6)
                .addMigrations(MIGRATION_7_8)
                .build()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOffers` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "`name` TEXT, " +
                            "`shortName` TEXT, " +
                            "`tagline` TEXT, " +
                            "`description` TEXT, " +
                            "`icon` TEXT" +
                            ")"
                    )
                    execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_PlanOffers_internalPlanId` " +
                            "ON `PlanOffers` (`internalPlanId`)"
                    )
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOfferIds` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`productId` INTEGER NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`internalPlanId`) REFERENCES `PlanOffers`(`internalPlanId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
                    )
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `PlanOfferFeatures` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`internalPlanId` INTEGER NOT NULL, " +
                            "`stringId` TEXT, " +
                            "`name` TEXT, " +
                            "`description` TEXT, " +
                            "FOREIGN KEY(`internalPlanId`) REFERENCES `PlanOffers`(`internalPlanId`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE" +
                            ")"
                    )
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `Comments` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`remoteCommentId` INTEGER NOT NULL, " +
                            "`remotePostId` INTEGER NOT NULL, " +
                            "`remoteParentCommentId` INTEGER NOT NULL, " +
                            "`localSiteId` INTEGER NOT NULL, " +
                            "`remoteSiteId` INTEGER NOT NULL, " +
                            "`authorUrl` TEXT, " +
                            "`authorName` TEXT, " +
                            "`authorEmail` TEXT, " +
                            "`authorProfileImageUrl` TEXT, " +
                            "`postTitle` TEXT, " +
                            "`status` TEXT, " +
                            "`datePublished` TEXT, " +
                            "`publishedTimestamp` INTEGER NOT NULL, " +
                            "`content` TEXT, " +
                            "`url` TEXT, " +
                            "`hasParent` INTEGER NOT NULL, " +
                            "`parentId` INTEGER NOT NULL, " +
                            "`iLike` INTEGER NOT NULL)"
                    )
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("ALTER TABLE BloggingReminders ADD COLUMN hour INTEGER DEFAULT 10 NOT NULL")
                    execSQL("ALTER TABLE BloggingReminders ADD COLUMN minute INTEGER DEFAULT 0 NOT NULL")
                }
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE Comments")
                    execSQL(
                        "CREATE TABLE `Comments` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`remoteCommentId` INTEGER NOT NULL, " +
                            "`remotePostId` INTEGER NOT NULL, " +
                            "`localSiteId` INTEGER NOT NULL, " +
                            "`remoteSiteId` INTEGER NOT NULL, " +
                            "`authorUrl` TEXT, " +
                            "`authorName` TEXT, " +
                            "`authorEmail` TEXT, " +
                            "`authorProfileImageUrl` TEXT, " +
                            "`authorId` INTEGER NOT NULL , " +
                            "`postTitle` TEXT, " +
                            "`status` TEXT, " +
                            "`datePublished` TEXT, " +
                            "`publishedTimestamp` INTEGER NOT NULL, " +
                            "`content` TEXT, " +
                            "`url` TEXT, " +
                            "`hasParent` INTEGER NOT NULL, " +
                            "`parentId` INTEGER NOT NULL, " +
                            "`iLike` INTEGER NOT NULL)"
                    )
                }
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "ALTER TABLE BloggingReminders ADD COLUMN isPromptRemindersOptedIn" +
                            " INTEGER DEFAULT 0 NOT NULL"
                    )
                }
            }
        }
    }
}
