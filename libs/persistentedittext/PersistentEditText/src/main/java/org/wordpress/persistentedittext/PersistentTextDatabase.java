package org.wordpress.persistentedittext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PersistentTextDatabase extends SQLiteOpenHelper {
    public static final String TAG = "PersistentEditText";
    private static final int MAX_ENTRIES = 100;
    private static final String DATABASE_NAME = "persistentedittext.db";
    private static final int DATABASE_VERSION = 1;
    private static final String PERSISTENTEDITTEXT_TABLE = "persistentedittext";

    public PersistentTextDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        reset(db);
    }

    public void reset(SQLiteDatabase db) {
        Log.i(TAG, "resetting " + PERSISTENTEDITTEXT_TABLE + " table");
        dropTables(db);
        createTables(db);
    }

    protected void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + PERSISTENTEDITTEXT_TABLE + " ("
                + "_id	     INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "key       TEXT UNIQUE,"
                + "value     TEXT)");
    }

    protected void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + PERSISTENTEDITTEXT_TABLE);
    }

    public void put(String key, String value) {
        ContentValues values = new ContentValues();
        values.put("key", key);
        values.put("value", value);
        getWritableDatabase().insertWithOnConflict(PERSISTENTEDITTEXT_TABLE, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
    }

    public String get(String key, String defaultResult) {
        Cursor c = getReadableDatabase().query(PERSISTENTEDITTEXT_TABLE, new String[]{"value"}, "key=?", new String[]{key}, null,
                null, null);
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }
        return defaultResult;
    }

    public void remove(String key) {
        getWritableDatabase().delete(PERSISTENTEDITTEXT_TABLE, "key=?", new String[]{key});
        // purge old entries
        purge(MAX_ENTRIES);
    }

    private void purge(int limit) {
        getWritableDatabase().delete(PERSISTENTEDITTEXT_TABLE, "_id NOT IN (SELECT _id FROM " + PERSISTENTEDITTEXT_TABLE +
                " ORDER BY _id DESC LIMIT " + limit + ")", null);
    }
}
