package org.wordpress.android.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.ReaderLog;

/**
 * Created by nbradbury on 6/22/13.
 * database for all reader information
 */
public class ReaderDatabase extends SQLiteOpenHelper {
    protected static final String DB_NAME = "wpreader.db";
    private static final int DB_VERSION = 56;

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
                    // purge older data
                    mReaderDb.purgeAsync();
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
     * called when database is created - uses external storage when debug const is set (so database can be accessed via ddms)
     */
    private static String getDatabaseName(Context context) {
        /*if (ReaderConst.DEBUG_EXTERNAL_STORAGE_DB) {
            File dir = context.getExternalFilesDir(null);
            if (dir!=null)
                return dir.getAbsolutePath() + "/" + DB_NAME;
        }*/
        return DB_NAME;
    }

    public ReaderDatabase(Context context) {
        super(context, getDatabaseName(context), null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // for now just reset the db when upgrading, future versions will want to avoid this
        // and modify table structures, etc., on upgrade while preserving data
        ReaderLog.i("Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        /*if (ReaderConst.DEBUG_RESET_DB_AT_START) {
            reset(db);
        } else if (ReaderConst.DEBUG_RESET_POSTS_AT_START) {
            ReaderTopicTable.resetTopicUpdates(db);
            ReaderPostTable.reset(db);
            ReaderCommentTable.reset(db);
            ReaderLikeTable.reset(db);
        }*/
    }

    private void createAllTables(SQLiteDatabase db) {
        ReaderCommentTable.createTables(db);
        ReaderLikeTable.createTables(db);
        ReaderPostTable.createTables(db);
        ReaderTopicTable.createTables(db);
        ReaderUserTable.createTables(db);
        ReaderThumbnailTable.createTables(db);
        ReaderBlogTable.createTables(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        ReaderCommentTable.dropTables(db);
        ReaderLikeTable.dropTables(db);
        ReaderPostTable.dropTables(db);
        ReaderTopicTable.dropTables(db);
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
     * purge older/unattached data
     */
    public void purge() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            int numPostsDeleted = ReaderPostTable.purge(db);

            // don't bother purging other data unless posts were purged
            if (numPostsDeleted > 0) {
                ReaderLog.i(numPostsDeleted + " posts purged");

                int numCommentsDeleted = ReaderCommentTable.purge(db);
                if (numCommentsDeleted > 0)
                    ReaderLog.i(numCommentsDeleted + " comments purged");

                int numLikesDeleted = ReaderLikeTable.purge(db);
                if (numLikesDeleted > 0)
                    ReaderLog.i(numLikesDeleted + " likes purged");

                int numThumbsPurged = ReaderThumbnailTable.purge(db);
                if (numThumbsPurged > 0)
                    ReaderLog.i(numThumbsPurged + " thumbnails purged");

                int numTopicsPurged = ReaderTopicTable.purge(db);
                if (numTopicsPurged > 0)
                    ReaderLog.i(numTopicsPurged + " topics purged");
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /*
     * async purge
     */
    public void purgeAsync() {
        new Thread() {
            @Override
            public void run() {
                purge();
            }
        }.start();
    }


}
