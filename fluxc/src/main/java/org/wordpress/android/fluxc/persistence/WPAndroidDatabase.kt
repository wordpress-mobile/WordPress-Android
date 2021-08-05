package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOffer
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferFeature
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferId

@Database(
        version = 2,
        entities = [
            BloggingReminders::class,
            PlanOffer::class,
            PlanOfferId::class,
            PlanOfferFeature::class
        ]
)
abstract class WPAndroidDatabase : RoomDatabase() {
    abstract fun bloggingRemindersDao(): BloggingRemindersDao

    abstract fun planOffersDao(): PlanOffersDao

    companion object {
        fun buildDb(applicationContext: Context) = Room.databaseBuilder(
                applicationContext,
                WPAndroidDatabase::class.java,
                "wp-android-database"
        )
                .fallbackToDestructiveMigration()
                .addMigrations(MIGRATION_1_2)
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
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
    }
}
