package org.wordpress.android.datasets;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.repository.ReaderRepositoryEvent.ReaderPostTableActionEnded;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * database for all reader information
 */
public class ReaderDatabase extends SQLiteOpenHelper {
    protected static final String DB_NAME = "wpreader.db";
    private static final int DB_VERSION = 138;
    private static final int DB_LAST_VERSION_WITHOUT_MIGRATION_SCRIPT = 136; // do not change this value

    /*
     * version history
     * 67 - added tbl_blog_info to ReaderBlogTable
     * 68 - added author_blog_id to ReaderCommentTable
     * 69 - renamed tbl_blog_urls to tbl_followed_blogs in ReaderBlogTable
     * 70 - added author_id to ReaderCommentTable and ReaderPostTable
     * 71 - added blog_id to ReaderUserTable
     * 72 - removed tbl_followed_blogs from ReaderBlogTable
     * 73 - added tbl_recommended_blogs to ReaderBlogTable
     * 74 - added primary_tag to ReaderPostTable
     * 75 - added secondary_tag to ReaderPostTable
     * 76 - added feed_id to ReaderBlogTable
     * 77 - restructured tag tables (ReaderTagTable)
     * 78 - added tag_type to ReaderPostTable.tbl_post_tags
     * 79 - added is_likes_enabled and is_sharing_enabled to tbl_posts
     * 80 - added tbl_comment_likes in ReaderLikeTable, added num_likes to tbl_comments
     * 81 - added image_url to tbl_blog_info
     * 82 - added idx_posts_timestamp to tbl_posts
     * 83 - removed tag_list from tbl_posts
     * 84 - added tbl_attachments
     * 85 - removed tbl_attachments, added attachments_json to tbl_posts
     * 90 - added default values for all INTEGER columns that were missing them (hotfix 3.1.1)
     * 92 - added default values for all INTEGER columns that were missing them (3.2)
     * 93 - tbl_posts text is now truncated to a max length (3.3)
     * 94 - added is_jetpack to tbl_posts (3.4)
     * 95 - added page_number to tbl_comments (3.4)
     * 96 - removed tbl_tag_updates, added date_updated to tbl_tags (3.4)
     * 97 - added short_url to tbl_posts
     * 98 - added feed_id to tbl_posts
     * 99 - added feed_url to tbl_blog_info
     * 100 - changed primary key on tbl_blog_info
     * 101 - dropped is_reblogged from ReaderPostTable
     * 102 - changed primary key of tbl_blog_info from blog_id+feed_id to just blog_id
     * 103 - added discover_json to ReaderPostTable
     * 104 - added word_count to ReaderPostTable
     * 105 - added date_updated to ReaderBlogTable
     * 106 - dropped is_likes_enabled and is_sharing_enabled from tbl_posts
     * 107 - "Blogs I Follow" renamed to "Followed Sites"
     * 108 - added "has_gap_marker" to tbl_post_tags
     * 109 - added "feed_item_id" to tbl_posts
     * 110 - added xpost_post_id and xpost_blog_id to tbl_posts
     * 111 - added author_first_name to tbl_posts
     * 112 - no structural change, just reset db
     * 113 - added tag_title to tag tables
     * 114 - renamed tag_name to tag_slug in tag tables
     * 115 - added ReaderSearchTable
     * 116 - added tag_display_name to tag tables
     * 117 - changed tbl_posts.timestamp from INTEGER to REAL
     * 118 - renamed tbl_search_history to tbl_search_suggestions
     * 119 - renamed tbl_posts.timestamp to sort_index
     * 120 - added "format" to tbl_posts
     * 121 - removed word_count from tbl_posts
     * 122 - changed tbl_posts primary key to pseudo_id
     * 123 - changed tbl_posts.published to tbl_posts.date
     * 124 - returned tbl_posts.published
     * 125 - added tbl_posts.railcar_json
     * 126 - separate fields in tbl_posts for date_liked, date_tagged, date_published
     * 127 - changed tbl_posts.sort_index to tbl_posts.score
     * 128 - added indexes on tbl_posts.date_published and tbl_posts.date_tagged
     * 129 - denormalized post storage, dropped tbl_post_tags
     * 130 - added tbl_posts.blog_image_url
     * 131 - added tbl_posts.card_type
     * 132 - no schema changes, simply clearing to accommodate gallery card_type
     * 133 - no schema changes, simply clearing to accommodate video card_type
     * 134 - added tbl_posts.use_excerpt
     * 135 - added tbl_blog_info.is_notifications_enabled in ReaderBlogTable
     * 136 - added tbl_posts.is_bookmarked
     * 137 - added support for migration scripts
     * 138 - added tbl_posts.is_private_atomic
     */

    /*
     * database singleton
     */
    private static ReaderDatabase mReaderDb;
    private static final Object DB_LOCK = new Object();

    public static ReaderDatabase getDatabase() {
        if (mReaderDb == null) {
            synchronized (DB_LOCK) {
                if (mReaderDb == null) {
                    mReaderDb = new ReaderDatabase(WordPress.getContext());
                    // this ensures that onOpen() is called with a writable database
                    // (open will fail if app calls getReadableDb() first)
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
        // copyDatabase(db);
        // getDatabase().reset(db);
    }

    /*
     * resets (clears) the reader database
     */
    public static void reset(boolean retainBookmarkedPosts) {
        // note that we must call getWritableDb() before getDatabase() in case the database
        // object hasn't been created yet
        SQLiteDatabase db = getWritableDb();

        if (retainBookmarkedPosts && ReaderPostTable.hasBookmarkedPosts()) {
            ReaderTagList tags = ReaderTagTable.getBookmarkTags();
            if (!tags.isEmpty()) {
                ReaderPostList bookmarkedPosts = ReaderPostTable.getPostsWithTag(tags.get(0), 0, false);
                db.beginTransaction();
                try {
                    getDatabase().reset(db);
                    ReaderPostTable.addOrUpdatePosts(tags.get(0), bookmarkedPosts);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                return;
            }
        }

        getDatabase().reset(db);
    }

    public ReaderDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createAllTables(db);
    }

    @SuppressWarnings({"FallThrough"})
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        AppLog.i(T.READER,
                "Upgrading database from version " + oldVersion + " to version " + newVersion + " IN PROGRESS");
        int currentVersion = oldVersion;
        if (currentVersion <= DB_LAST_VERSION_WITHOUT_MIGRATION_SCRIPT) {
            // versions 0 - 136 didn't support migration scripts, so we can safely drop and recreate all tables
            reset(db);
            currentVersion = newVersion;
        }

        switch (currentVersion) {
            case 136:
                // no-op
                currentVersion++;
            case 137:
                db.execSQL("ALTER TABLE tbl_posts ADD is_private_atomic BOOLEAN;");
                currentVersion++;
        }
        if (currentVersion != newVersion) {
            throw new RuntimeException(
                    "Migration from version " + oldVersion + " to version " + newVersion + " FAILED. ");
        }
        AppLog.i(T.READER,
                "Upgrading database from version " + oldVersion + " to version " + newVersion + " SUCCEEDED");
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
        ReaderSearchTable.createTables(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        ReaderCommentTable.dropTables(db);
        ReaderLikeTable.dropTables(db);
        ReaderPostTable.dropTables(db);
        ReaderTagTable.dropTables(db);
        ReaderUserTable.dropTables(db);
        ReaderThumbnailTable.dropTables(db);
        ReaderBlogTable.dropTables(db);
        ReaderSearchTable.dropTables(db);
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
                AppLog.i(T.READER, String.format(Locale.ENGLISH, "%d total posts purged", numPostsDeleted));

                // purge unattached comments
                int numCommentsDeleted = ReaderCommentTable.purge(db);
                if (numCommentsDeleted > 0) {
                    AppLog.i(T.READER, String.format(Locale.ENGLISH, "%d comments purged", numCommentsDeleted));
                }

                // purge unattached likes
                int numLikesDeleted = ReaderLikeTable.purge(db);
                if (numLikesDeleted > 0) {
                    AppLog.i(T.READER, String.format(Locale.ENGLISH, "%d likes purged", numLikesDeleted));
                }

                // purge unattached thumbnails
                int numThumbsPurged = ReaderThumbnailTable.purge(db);
                if (numThumbsPurged > 0) {
                    AppLog.i(T.READER, String.format(Locale.ENGLISH, "%d thumbnails purged", numThumbsPurged));
                }
            }
            db.setTransactionSuccessful();
            if (numPostsDeleted > 0) {
                EventBus.getDefault().post(ReaderPostTableActionEnded.INSTANCE);
            }
        } finally {
            db.endTransaction();
        }
    }

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
