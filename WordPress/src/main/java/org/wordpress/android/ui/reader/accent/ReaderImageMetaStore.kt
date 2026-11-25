package org.wordpress.android.ui.reader.accent

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What sampling a featured image found: a dark accent color taken from it, and its aspect ratio (width / height).
 * [accent] is null when sampling failed, and [failedAt] says when, so it can be retried later.
 */
data class ReaderImageMeta(
    val accent: Int?,
    val ratio: Float?,
    val failedAt: Long? = null,
)

/**
 * Keeps each featured image's [ReaderImageMeta], keyed by the image's URL so it survives post refreshes and the
 * Reader database being reset. It lives in its own small database rather than the Reader's, so it needs no Reader
 * database migration. Everything is held in memory once loaded, as [meta], which the cards observe.
 */
@Singleton
class ReaderImageMetaStore @Inject constructor(@ApplicationContext context: Context) {
    private val helper = Helper(context)
    private val _meta = MutableStateFlow<Map<String, ReaderImageMeta>>(emptyMap())
    private var isLoaded = false

    val meta: StateFlow<Map<String, ReaderImageMeta>>
        get() {
            ensureLoaded()
            return _meta.asStateFlow()
        }

    fun get(url: String): ReaderImageMeta? {
        ensureLoaded()
        return _meta.value[url]
    }

    fun putAll(entries: Map<String, ReaderImageMeta>) {
        if (entries.isEmpty()) return
        ensureLoaded()
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            entries.forEach { (url, meta) ->
                val values = ContentValues().apply {
                    put(COLUMN_URL, url)
                    put(COLUMN_ACCENT, meta.accent)
                    put(COLUMN_RATIO, meta.ratio)
                    put(COLUMN_FAILED_AT, meta.failedAt)
                    put(COLUMN_SAMPLED_AT, System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _meta.update { it + entries }
    }

    /*
     * read once, on the first use - the table is small, and pruned of old rows as it's read
     */
    @Synchronized
    private fun ensureLoaded() {
        if (isLoaded) return
        isLoaded = true
        val db = helper.writableDatabase
        db.delete(TABLE, "$COLUMN_SAMPLED_AT < ?", arrayOf((System.currentTimeMillis() - MAX_AGE_MS).toString()))
        val loaded = mutableMapOf<String, ReaderImageMeta>()
        db.query(TABLE, null, null, null, null, null, null).use { cursor ->
            val url = cursor.getColumnIndexOrThrow(COLUMN_URL)
            val accent = cursor.getColumnIndexOrThrow(COLUMN_ACCENT)
            val ratio = cursor.getColumnIndexOrThrow(COLUMN_RATIO)
            val failedAt = cursor.getColumnIndexOrThrow(COLUMN_FAILED_AT)
            while (cursor.moveToNext()) {
                loaded[cursor.getString(url)] = ReaderImageMeta(
                    accent = if (cursor.isNull(accent)) null else cursor.getInt(accent),
                    ratio = if (cursor.isNull(ratio)) null else cursor.getFloat(ratio),
                    failedAt = if (cursor.isNull(failedAt)) null else cursor.getLong(failedAt),
                )
            }
        }
        _meta.update { loaded + it }
    }

    private class Helper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE (" +
                    "$COLUMN_URL TEXT PRIMARY KEY, " +
                    "$COLUMN_ACCENT INTEGER, " +
                    "$COLUMN_RATIO REAL, " +
                    "$COLUMN_FAILED_AT INTEGER, " +
                    "$COLUMN_SAMPLED_AT INTEGER NOT NULL)"
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE")
            onCreate(db)
        }
    }

    companion object {
        private const val DB_NAME = "reader_image_meta.db"
        private const val DB_VERSION = 1
        private const val TABLE = "image_meta"
        private const val COLUMN_URL = "url"
        private const val COLUMN_ACCENT = "accent"
        private const val COLUMN_RATIO = "ratio"
        private const val COLUMN_FAILED_AT = "failed_at"
        private const val COLUMN_SAMPLED_AT = "sampled_at"
        private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000
    }
}
