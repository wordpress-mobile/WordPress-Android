package org.wordpress.android.datasets

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.wordpress.android.ui.reader.discover.DiscoverSortingType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.SqlUtils

object ReaderDiscoverCardsTable {
    private const val DISCOVER_CARDS_TABLE = "tbl_discover_cards"
    private const val CARDS_JSON_COLUMN = "cards_json"
    private const val CARDS_SORTING_TYPE_COLUMN = "cards_sorting_type"

    fun createTable(db: SQLiteDatabase) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS $DISCOVER_CARDS_TABLE (" +
                        "  _id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $CARDS_JSON_COLUMN TEXT," +
                        " $CARDS_SORTING_TYPE_COLUMN TEXT DEFAULT ${DiscoverSortingType.NONE.sortedBy}" +
                        ")"
        )
    }

    fun addSortingTypeColumnToTable(db: SQLiteDatabase) {
        db.execSQL(
                "ALTER TABLE $DISCOVER_CARDS_TABLE ADD " +
                        "$CARDS_SORTING_TYPE_COLUMN TEXT DEFAULT ${DiscoverSortingType.NONE.sortedBy};"
        )
    }

    fun dropTables(db: SQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS tbl_discover_cards")
    }

    fun clear(sortingType: DiscoverSortingType) {
        val args = arrayOf(sortingType.sortedBy)
        val whereClause = "$CARDS_SORTING_TYPE_COLUMN = ?"
        AppLog.i(AppLog.T.READER, "clearing ReaderDiscoverCardsTable")
        getWritableDb().delete(DISCOVER_CARDS_TABLE, whereClause, args)
    }

    private fun getReadableDb(): SQLiteDatabase {
        return ReaderDatabase.getReadableDb()
    }

    private fun getWritableDb(): SQLiteDatabase {
        return ReaderDatabase.getWritableDb()
    }

    fun addCardsPage(cardsJson: String, DiscoverSortingType: DiscoverSortingType) {
        val values = ContentValues()
        values.put(CARDS_JSON_COLUMN, cardsJson)
        values.put(CARDS_SORTING_TYPE_COLUMN, DiscoverSortingType.sortedBy)

        getWritableDb().insert(DISCOVER_CARDS_TABLE, null, values)
    }

    fun loadDiscoverCardsJsons(discoverSortingType: DiscoverSortingType): List<String> {
        val args = arrayOf(discoverSortingType.sortedBy)
        val c = getReadableDb()
                .rawQuery("SELECT * FROM $DISCOVER_CARDS_TABLE " +
                        "WHERE $CARDS_SORTING_TYPE_COLUMN = ?" +
                        " ORDER BY _id ASC", args)
        val cardJsonList = arrayListOf<String>()
        try {
            if (c.moveToFirst()) {
                do {
                    val cardJson = c.getString(c.getColumnIndex(CARDS_JSON_COLUMN))
                    cardJsonList.add(cardJson)
                } while (c.moveToNext())
            }
        } finally {
            SqlUtils.closeCursor(c)
        }
        return cardJsonList
    }
}
