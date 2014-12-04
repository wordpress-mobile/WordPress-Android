package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

public class AutoSaveTextTable {
    public static final int MAX_ENTRIES = 100;
    private static final String AUTOSAVETEXT_TABLE = "autosavetext";

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.DB, "resetting " + AUTOSAVETEXT_TABLE + " table");
        dropTables(db);
        createTables(db);
    }

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + AUTOSAVETEXT_TABLE + " ("
                + "_id	     INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "key       TEXT UNIQUE,"
                + "value     TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + AUTOSAVETEXT_TABLE);
    }

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }

    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void put(String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        getWritableDb().insertWithOnConflict(AUTOSAVETEXT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static String get(String key, String defaultResult) {
        Cursor c = getReadableDb().query(AUTOSAVETEXT_TABLE, new String[]{"value"}, "key=?", new String[]{key}, null,
                null, null);
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
        } finally {
            SqlUtils.closeCursor(c);
        }
        return defaultResult;
    }

    public static void remove(String key) {
        getWritableDb().delete(AUTOSAVETEXT_TABLE, "key=?", new String[]{key});
        // purge old entries
        purge(MAX_ENTRIES);
    }

    private static void purge(int limit) {
        getWritableDb().delete(AUTOSAVETEXT_TABLE, "_id NOT IN (SELECT _id FROM " + AUTOSAVETEXT_TABLE +
                " ORDER BY _id DESC LIMIT " + limit + ")", null);
    }
}
