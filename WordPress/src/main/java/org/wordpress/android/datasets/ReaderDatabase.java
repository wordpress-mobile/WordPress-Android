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
 * database for all reader information
 */
public class ReaderDatabase extends SQLiteOpenHelper {
    protected static final String DB_NAME = "wpreader.db";
    private static final int DB_VERSION = 93;

    /*
     * version history
     *   67 - added tbl_blog_info to ReaderBlogTable
     *   68 - added author_blog_id to ReaderCommentTable
     *   69 - renamed tbl_blog_urls to tbl_followed_blogs in ReaderBlogTable
     *   70 - added author_id to ReaderCommentTable and ReaderPostTable
     *   71 - added blog_id to ReaderUserTable
     *   72 - removed tbl_followed_blogs from ReaderBlogTable
     *   73 - added tbl_recommended_blogs to ReaderBlogTable
     *   74 - added primary_tag to ReaderPostTable
     *   75 - added secondary_tag to ReaderPostTable
     *   76 - added feed_id to ReaderBlogTable
     *   77 - restructured tag tables (ReaderTagTable)
     *   78 - added tag_type to ReaderPostTable.tbl_post_tags
     *   79 - added is_likes_enabled and is_sharing_enabled to tbl_posts
     *   80 - added tbl_comment_likes in ReaderLikeTable, added num_likes to tbl_comments
     *   81 - added image_url to tbl_blog_info
     *   82 - added idx_posts_timestamp to tbl_posts
     *   83 - removed tag_list from tbl_posts
     *   84 - added tbl_attachments
     *   85 - removed tbl_attachments, added attachments_json to tbl_posts
     *   90 - added default values for all INTEGER columns that were missing them (hotfix 3.1.1)
     *   92 - added default values for all INTEGER columns that were missing them (3.2)
     *   93 - tbl_posts text is now truncated to a max length (3.3)
     */

    /*
	 *  database singleton
	 */
    private static ReaderDatabase mReaderDb;
    private final static Object mDbLock = new Object();
    public static ReaderDatabase getDatabase() {
        if (mReaderDb == null) {
            synchronized(mDbLock) {
                if (mReaderDb == null) {
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

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        //copyDatabase(db);
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
        AppLog.i(T.READER, "Upgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // IMPORTANT: do NOT call super() here - doing so throws a SQLiteException
        AppLog.w(T.READER, "Downgrading database from version " + oldVersion + " to version " + newVersion);
        reset(db);
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
        db.beginTransaction();
        try {
            dropAllTables(db);
            createAllTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
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
                AppLog.i(T.READER, String.format("%d total posts purged", numPostsDeleted));

                // purge unattached comments
                int numCommentsDeleted = ReaderCommentTable.purge(db);
                if (numCommentsDeleted > 0) {
                    AppLog.i(T.READER, String.format("%d comments purged", numCommentsDeleted));
                }

                // purge unattached likes
                int numLikesDeleted = ReaderLikeTable.purge(db);
                if (numLikesDeleted > 0) {
                    AppLog.i(T.READER, String.format("%d likes purged", numLikesDeleted));
                }

                // purge unattached thumbnails
                int numThumbsPurged = ReaderThumbnailTable.purge(db);
                if (numThumbsPurged > 0) {
                    AppLog.i(T.READER, String.format("%d thumbnails purged", numThumbsPurged));
                }

                // purge unattached tags
                int numTagsPurged = ReaderTagTable.purge(db);
                if (numTagsPurged > 0) {
                    AppLog.i(T.READER, String.format("%d tags purged", numTagsPurged));
                }
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

    /*
     * used during development to copy database to external storage so we can access it via DDMS
     */
    private void copyDatabase(SQLiteDatabase db) {
        String copyFrom = db.getPath();
        String copyTo = WordPress.getContext().getExternalFilesDir(null).getAbsolutePath() + "/" + DB_NAME;

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
            AppLog.e(T.DB, "failed to copy reader database", e);
        }
    }


}
