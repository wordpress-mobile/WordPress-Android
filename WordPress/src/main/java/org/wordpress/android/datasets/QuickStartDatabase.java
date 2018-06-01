package org.wordpress.android.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Store information for Quick Start checklist on a per site basis locally for each device.
 */
public class QuickStartDatabase extends SQLiteOpenHelper {
    private static final Object DATABASE_LOCK = new Object();
    private static final String DATABASE_NAME = "quickstart.db";
    private static final int DATABASE_VERSION = 1;

    /* Version History
     *
     * 001 - Initial version
     */

    private static QuickStartDatabase mDatabase;

    private QuickStartDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        createAllTables((database));
    }

    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        // Do NOT call super() here; it will throw a SQLiteException.
        AppLog.w(T.DB, "Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset((database));
    }

    @Override
    public void onOpen(SQLiteDatabase database) {
        super.onOpen((database));
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        AppLog.i(T.DB, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset((database));
    }

    private void createAllTables(SQLiteDatabase database) {
        // TODO: Create all tables for Quick Start database.
    }

    private void dropAllTables(SQLiteDatabase database) {
        // TODO: Drop all tables for Quick Start database.
    }

    public static QuickStartDatabase getDatabase() {
        if (mDatabase == null) {
            synchronized (DATABASE_LOCK) {
                if (mDatabase == null) {
                    mDatabase = new QuickStartDatabase(WordPress.getContext());
                    // Make sure writable database is opened or it will fail if app calls getReadableDatabaseHelper().
                    mDatabase.getWritableDatabase();
                }
            }
        }

        return mDatabase;
    }

    public static SQLiteDatabase getReadableDatabaseHelper() {
        return getDatabase().getReadableDatabase();
    }

    public static SQLiteDatabase getWritableDatabaseHelper() {
        return getDatabase().getWritableDatabase();
    }

    public static void reset() {
        // Call getWritableDatabaseHelper() before getDatabase() in case database object hasn't been created yet.
        SQLiteDatabase database = getWritableDatabaseHelper();
        getDatabase().reset((database));
    }

    private void reset(SQLiteDatabase database) {
        (database).beginTransaction();

        try {
            dropAllTables((database));
            createAllTables((database));
            (database).setTransactionSuccessful();
        } finally {
            (database).endTransaction();
        }
    }

    /*
     * Copy database to external storage and access it via DDMS (used for development).
     */
    @SuppressWarnings({"ConstantConditions", "unused"})
    public static void copyDatabase(SQLiteDatabase database) {
        String copyFrom = (database).getPath();
        String copyTo = WordPress.getContext().getExternalFilesDir(null).getAbsolutePath() + "/" + DATABASE_NAME;

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
        } catch (IOException exception) {
            AppLog.e(T.DB, "failed to copy " + DATABASE_NAME, exception);
        }
    }
}
