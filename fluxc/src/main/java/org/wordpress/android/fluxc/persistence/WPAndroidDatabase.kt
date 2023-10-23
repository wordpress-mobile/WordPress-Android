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
import org.wordpress.android.fluxc.persistence.JetpackCPConnectedSitesDao.JetpackCPConnectedSiteEntity
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOffer
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferFeature
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferId
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfig
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignEntity
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao.BlazeCampaignsPaginationEntity
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import org.wordpress.android.fluxc.persistence.comments.CommentsDao
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.persistence.coverters.StringListConverter
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao
import org.wordpress.android.fluxc.persistence.dashboard.CardsDao.CardEntity
import org.wordpress.android.fluxc.persistence.domains.DomainDao
import org.wordpress.android.fluxc.persistence.domains.DomainDao.DomainEntity
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao
import org.wordpress.android.fluxc.persistence.jetpacksocial.JetpackSocialDao.JetpackSocialEntity

@Database(
        version = 21,
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
            JetpackCPConnectedSiteEntity::class,
            DomainEntity::class,
            BlazeCampaignEntity::class,
            BlazeCampaignsPaginationEntity::class,
            JetpackSocialEntity::class,
        ],
        autoMigrations = [
            AutoMigration(from = 11, to = 12),
            AutoMigration(from = 12, to = 13),
            AutoMigration(from = 13, to = 14),
            AutoMigration(from = 16, to = 17),
            AutoMigration(from = 17, to = 18),
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

    abstract fun domainDao(): DomainDao

    abstract fun jetpackCPConnectedSitesDao(): JetpackCPConnectedSitesDao

    abstract fun blazeCampaignsDao(): BlazeCampaignsDao

    abstract fun jetpackSocialDao(): JetpackSocialDao

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
                .addMigrations(MIGRATION_14_15)
                .addMigrations(MIGRATION_15_16)
                .addMigrations(MIGRATION_18_19)
                .addMigrations(MIGRATION_19_20)
                .addMigrations(MIGRATION_20_21)
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

        val MIGRATION_14_15 = object : Migration(14,15){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "DROP TABLE IF EXISTS `BlazeStatus`"
                    )
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15,16){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL(
                        "DROP TABLE IF EXISTS `BlazeStatus`"
                    )
                }
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaigns`")
                    execSQL("DELETE FROM `BlazeCampaignsPagination`")
                    execSQL("CREATE TABLE `BlazeCampaigns` (" +
                        "`siteId` INTEGER NOT NULL, " +
                        "`campaignId` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`imageUrl` TEXT, " +
                        "`startDate` TEXT NOT NULL, " +
                        "`endDate` TEXT, " +
                        "`uiStatus` TEXT NOT NULL, " +
                        "`budgetCents` INTEGER NOT NULL, " +
                        "`impressions` INTEGER NOT NULL, " +
                        "`clicks` INTEGER NOT NULL, " +
                        "PRIMARY KEY (`siteId`, `campaignId`)" +
                        ")"
                    )
                    execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_BlazeCampaigns_siteId` " +
                            "ON `BlazeCampaigns` (`siteId`)"
                    )
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.apply {
                    execSQL("DROP TABLE IF EXISTS `BlazeCampaigns`")
                    execSQL("DELETE FROM `BlazeCampaignsPagination`")
                    execSQL("CREATE TABLE `BlazeCampaigns` (" +
                            "`siteId` INTEGER NOT NULL, " +
                            "`campaignId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`imageUrl` TEXT, " +
                            "`createdAt` TEXT NOT NULL, " +
                            "`endDate` TEXT, " +
                            "`uiStatus` TEXT NOT NULL, " +
                            "`budgetCents` INTEGER NOT NULL, " +
                            "`impressions` INTEGER NOT NULL, " +
                            "`clicks` INTEGER NOT NULL, " +
                            "PRIMARY KEY (`siteId`, `campaignId`)" +
                            ")"
                    )
                    execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_BlazeCampaigns_siteId` " +
                                "ON `BlazeCampaigns` (`siteId`)"
                    )
                }
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // migrate old data to new table schema
                database.apply {
                    execSQL(
                        "CREATE TABLE IF NOT EXISTS `BloggingPrompts_new` (" +
                            "`id` INTEGER NOT NULL," +
                            "`siteLocalId` INTEGER NOT NULL," +
                            "`text` TEXT NOT NULL," +
                            "`date` TEXT NOT NULL," +
                            "`isAnswered` INTEGER NOT NULL," +
                            "`respondentsCount` INTEGER NOT NULL," +
                            "`attribution` TEXT NOT NULL," +
                            "`respondentsAvatars` TEXT NOT NULL," +
                            "`answeredLink` TEXT NOT NULL," +
                            "PRIMARY KEY(`date`)" +
                            ")"
                    )

                    val tagPrefix = "https://wordpress.com/tag/dailyprompt-"
                    execSQL(
                        "INSERT INTO BloggingPrompts_new (" +
                            "id, siteLocalId, text, date, isAnswered, " +
                            "respondentsCount, attribution, respondentsAvatars, answeredLink) " +
                            "SELECT id, siteLocalId, text, date, isAnswered, " +
                            "respondentsCount, attribution, respondentsAvatars, " +
                            "'$tagPrefix' || id FROM BloggingPrompts"
                    )

                    execSQL("DROP TABLE `BloggingPrompts`")
                    execSQL("ALTER TABLE `BloggingPrompts_new` RENAME TO `BloggingPrompts`")
                }
            }
        }
    }
}
