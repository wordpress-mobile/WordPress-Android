package org.wordpress.android.util.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * database for all reader information
 */
public class LogDatabase extends SQLiteOpenHelper {
    protected static final String DB_NAME = "log.db";
    private static final int DB_VERSION = 1;

    /*
     * version history
     * 1 - creation, added LogTable
     */

    /*
     * database singleton
     */
    private static LogDatabase mLogDb;
    private static final Object DB_LOCK = new Object();

    public static LogDatabase getDatabase(Context context) {
        if (mLogDb == null) {
            synchronized (DB_LOCK) {
                if (mLogDb == null) {
                    mLogDb = new LogDatabase(context);
                    // this ensures that onOpen() is called with a writable database
                    // (open will fail if app calls getReadableDb() first)
                    mLogDb.getWritableDatabase();
                }
            }
        }
        return mLogDb;
    }

    public static SQLiteDatabase getReadableDb(Context context) {
        return getDatabase(context).getReadableDatabase();
    }

    public static SQLiteDatabase getWritableDb(Context context) {
        return getDatabase(context).getWritableDatabase();
    }

    public LogDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @SuppressWarnings({"FallThrough"})
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int currentVersion = oldVersion;

        switch (currentVersion) {
            case 1:
                // no-op
                ++currentVersion;
        }
        if (currentVersion != newVersion) {
            throw new RuntimeException(
                    "Migration from version " + oldVersion + " to version " + newVersion + " FAILED. ");
        }
    }

    private void createAllTables(SQLiteDatabase db) {
        LogTable.createTables(db);
    }
}
