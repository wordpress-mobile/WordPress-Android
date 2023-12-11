package org.wordpress.android.editor.savedinstance

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Parcelable
import org.wordpress.android.editor.savedinstance.SavedParcelTable.createTable
import org.wordpress.android.editor.savedinstance.SavedParcelTable.dropTable

/**
 * Database for the saved instance state data
 */
class SavedInstanceDatabase(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
    }

    override fun onCreate(db: SQLiteDatabase) {
        createAllTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        reset(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        reset(db)
    }

    private fun createAllTables(db: SQLiteDatabase) {
        createTable(db)
    }

    private fun dropAllTables(db: SQLiteDatabase) {
        dropTable(db)
    }

    fun reset(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            dropAllTables(db)
            createAllTables(db)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addParcel(parcelId: String, parcel: Parcelable?) {
        parcel?.let {
            SavedParcelTable.addParcel(writableDatabase, parcelId, it)
        }
    }

    fun <T> getParcel(parcelId: String, creator: Parcelable.Creator<T>): T? {
        return SavedParcelTable.getParcel(readableDatabase, parcelId, creator)
    }

    fun hasParcel(parcelId: String): Boolean {
        return SavedParcelTable.hasParcel(readableDatabase, parcelId)
    }

    companion object {
        private const val DB_NAME = "wpsavedinstance.db"
        private const val DB_VERSION = 1

        private var mSavedInstanceDb: SavedInstanceDatabase? = null
        private val DB_LOCK = Any()

        fun getDatabase(context: Context): SavedInstanceDatabase? {
            if (mSavedInstanceDb == null) {
                synchronized(DB_LOCK) {
                    if (mSavedInstanceDb == null) {
                        mSavedInstanceDb = SavedInstanceDatabase(context.applicationContext)
                        // this ensures that onOpen() is called with a writable database
                        // (open will fail if app calls getReadableDb() first)
                        mSavedInstanceDb?.writableDatabase
                    }
                }
            }
            return mSavedInstanceDb
        }
    }
}
