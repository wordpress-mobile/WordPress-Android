package org.wordpress.android.ui.stats.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wordpress.android.util.AppLog;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * database for all tracks information
 */
public class StatsDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "stats.db";
    private static final int DB_VERSION = 1;

    /*
	 *  database singleton
	 */
    private static StatsDatabaseHelper mDatabaseHelper;
    private final static Object mDbLock = new Object();
    private final Context mContext;

    public static StatsDatabaseHelper getDatabase(Context ctx) {
        if (mDatabaseHelper == null) {
            synchronized(mDbLock) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new StatsDatabaseHelper(ctx);
                    // this ensures that onOpen() is called with a writable database (open will fail if app calls getReadableDb() first)
                    mDatabaseHelper.getWritableDatabase();
                }
            }
        }
        return mDatabaseHelper;
    }

    private StatsDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }


    public static SQLiteDatabase getReadableDb(Context ctx) {
        return getDatabase(ctx).getReadableDatabase();
    }
    public static SQLiteDatabase getWritableDb(Context ctx) {
        return getDatabase(ctx).getWritableDatabase();
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Used during development to copy database to external storage and read its content.
        // copyDatabase(db);
    }

    /*
     * drop & recreate all tables (essentially clears the db of all data)
     */
    public void reset() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            dropAllTables(db);
            createAllTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just reset the db when upgrading, future versions may want to avoid this
        // and modify table structures, etc., on upgrade while preserving data
        AppLog.i(AppLog.T.STATS, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset();
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // IMPORTANT: do NOT call super() here - doing so throws a SQLiteException
        AppLog.w(AppLog.T.STATS, "Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset();
    }

    private void createAllTables(SQLiteDatabase db) {
        StatsTable.createTables(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        StatsTable.dropTables(db);
    }

    /*
     * used during development to copy database to external storage so we can access it via DDMS
    */
    @SuppressWarnings("unused")
    private void copyDatabase(SQLiteDatabase db) {
        String copyFrom = db.getPath();
        String copyTo = mContext.getExternalFilesDir(null).getAbsolutePath() + "/" + DB_NAME;

        try {
            InputStream input = new FileInputStream(copyFrom);
            OutputStream output = new FileOutputStream(copyTo);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            AppLog.e(AppLog.T.STATS, "failed to copy stats database", e);
        }
    }
}
