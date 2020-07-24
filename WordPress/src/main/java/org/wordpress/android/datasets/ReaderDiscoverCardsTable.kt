package org.wordpress.android.datasets

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SqlUtils

object ReaderDiscoverCardsTable {
    private const val DISCOVER_CARDS_TABLE = "tbl_discover_cards"
    private const val CARDS_JSON_COLUMN = "cards_json"
    fun createTable(db: SQLiteDatabase) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS $DISCOVER_CARDS_TABLE ("
                        + "  _id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + " $CARDS_JSON_COLUMN TEXT"
                        + ")"
        )
    }

    fun dropTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS tbl_discover_cards")
    }

    fun reset(db: SQLiteDatabase) {
        AppLog.i(AppLog.T.READER, "resetting ReaderDiscoverCardsTable")
        dropTables(db)
        createTable(db)
    }

    private fun getReadableDb(): SQLiteDatabase {
        return WordPress.wpDB.database
    }

    private fun getWritableDb(): SQLiteDatabase {
        return WordPress.wpDB.database
    }

    fun addCardsPage(cardsJson: String) {
        val values = ContentValues()
        values.put(CARDS_JSON_COLUMN, cardsJson)

        getWritableDb().insert(DISCOVER_CARDS_TABLE, null, values)
    }

    fun loadDiscoverCardsJsons(): List<String> {
        val c = getReadableDb()
                .rawQuery("SELECT * FROM $DISCOVER_CARDS_TABLE ORDER BY _id ASC", null)
        return try {
            val cardJsonList = arrayListOf<String>()
            if (c.moveToFirst()) {
                do {
                    val cardJson = c.getString(c.getColumnIndex(CARDS_JSON_COLUMN))
                    cardJsonList.add(cardJson)
                } while (c.moveToNext())
            }
            return cardJsonList
        } finally {
            SqlUtils.closeCursor(c)
        }
    }
}
