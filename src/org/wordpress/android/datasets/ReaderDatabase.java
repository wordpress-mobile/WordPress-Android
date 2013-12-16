package org.wordpress.android.datasets;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.ReaderLog;

/**
 * Created by nbradbury on 6/22/13.
 * database for all reader information
 */
public class ReaderDatabase extends SQLiteOpenHelper {
    protected static final String DB_NAME = "wpreader.db";
    private static final int DB_VERSION = 66;

    /*
	 *  database singleton
	 */
    private static ReaderDatabase mReaderDb;
    private final static Object mDbLock = new Object();
    public static ReaderDatabase getDatabase() {
        if (mReaderDb==null) {
            synchronized(mDbLock) {
                if (mReaderDb==null) {
                    mReaderDb = new ReaderDatabase(WordPress.getContext());
                    // this ensures that onOpen() is called with a writable database (open will fail if app calls getReadableDb() first)
                    mReaderDb.getWritableDatabase();
                }
            }
        }
        return mReaderDb;
    }

    public static SQLiteDatabase getReadableDb() {
        return getDatabase().getReadableDatabase();
    }
    public static SQLiteDatabase getWritableDb() {
        return getDatabase().getWritableDatabase();
    }

    /*
     * resets (clears) the reader database
     */
    public static void reset() {
        // note that we must call getWritableDb() before getDatabase() in case the database
        // object hasn't been created yet
        SQLiteDatabase db = getWritableDb();
        getDatabase().reset(db);
    }

    public ReaderDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just reset the db when upgrading, future versions may want to avoid this
        // and modify table structures, etc., on upgrade while preserving data
        ReaderLog.i("Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // IMPORTANT: do NOT call super() here - doing so throws a SQLiteException
        ReaderLog.w("Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

    private void createAllTables(SQLiteDatabase db) {
        ReaderCommentTable.createTables(db);
        ReaderLikeTable.createTables(db);
        ReaderPostTable.createTables(db);
        ReaderTagTable.createTables(db);
        ReaderUserTable.createTables(db);
        ReaderThumbnailTable.createTables(db);
        ReaderBlogTable.createTables(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        ReaderCommentTable.dropTables(db);
        ReaderLikeTable.dropTables(db);
        ReaderPostTable.dropTables(db);
        ReaderTagTable.dropTables(db);
        ReaderUserTable.dropTables(db);
        ReaderThumbnailTable.dropTables(db);
        ReaderBlogTable.dropTables(db);
    }

    /*
     * drop & recreate all tables (essentially clears the db of all data)
     */
    private void reset(SQLiteDatabase db) {
        dropAllTables(db);
        createAllTables(db);
    }

    /*
     * purge older/unattached data - use purgeAsync() to do this in the background
     */
    private static void purge() {
        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            int numPostsDeleted = ReaderPostTable.purge(db);

            // don't bother purging other data unless posts were purged
            if (numPostsDeleted > 0) {
                ReaderLog.i(String.format("%d total posts purged", numPostsDeleted));

                // purge unattached comments
                int numCommentsDeleted = ReaderCommentTable.purge(db);
                if (numCommentsDeleted > 0)
                    ReaderLog.i(String.format("%d comments purged", numCommentsDeleted));

                // purge unattached likes
                int numLikesDeleted = ReaderLikeTable.purge(db);
                if (numLikesDeleted > 0)
                    ReaderLog.i(String.format("%d likes purged", numLikesDeleted));

                // purge unattached thumbnails
                int numThumbsPurged = ReaderThumbnailTable.purge(db);
                if (numThumbsPurged > 0)
                    ReaderLog.i(String.format("%d thumbnails purged", numThumbsPurged));

                // purge unattached tags
                int numTagsPurged = ReaderTagTable.purge(db);
                if (numTagsPurged > 0)
                    ReaderLog.i(String.format("%d tags purged", numTagsPurged));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /*
     * async purge
     */
    public static void purgeAsync() {
        new Thread() {
            @Override
            public void run() {
                purge();
            }
        }.start();
    }


}
