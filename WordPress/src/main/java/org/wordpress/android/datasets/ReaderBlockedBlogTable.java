package org.wordpress.android.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.SqlUtils;

public class ReaderBlockedBlogTable {
    protected static final String BLOCKED_BLOGS_TABLE = "tbl_blocked_blogs";
    private static final String BLOG_ID = "blog_id";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + BLOCKED_BLOGS_TABLE + " ("
                   + BLOG_ID + " INTEGER DEFAULT 0,"
                   + " PRIMARY KEY (" + BLOG_ID + ")"
                   + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + BLOCKED_BLOGS_TABLE);
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    public static void addBlockedBlog(long blogId) {
        SQLiteStatement stmt = null;
        try {
            stmt = ReaderDatabase.getWritableDb().compileStatement(
                    "INSERT OR REPLACE INTO " + BLOCKED_BLOGS_TABLE + " (" + BLOG_ID + ") VALUES (?1)");

            stmt.bindString(1, Long.toString(blogId));
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static boolean isBlockedBlog(long blogId) {
        if (blogId == 0) {
            return false;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM " + BLOCKED_BLOGS_TABLE + " WHERE " + BLOG_ID + "=?",
                new String[]{Long.toString(blogId)}) > 0;
    }

    public static void removeBlockedBlog(long blogId) {
        if (blogId == 0) {
            return;
        }
        String[] args = new String[]{Long.toString(blogId)};
        ReaderDatabase.getWritableDb().delete(BLOCKED_BLOGS_TABLE, BLOG_ID + "=?", args);
    }

    /*
     * the server drops a blocked site from the followed sites list, and unblocking it on the web re-subscribes
     * to it, so a followed site can't be blocked - this clears the local block left behind by such an unblock,
     * which would otherwise keep dropping the site's posts until sign-out
     */
    public static void removeFollowedBlogs() {
        ReaderDatabase.getWritableDb().execSQL("DELETE FROM " + BLOCKED_BLOGS_TABLE + " WHERE " + BLOG_ID
                                               + " IN (SELECT blog_id FROM tbl_blog_info WHERE is_following=1)");
    }

    public static void blacklistBlogLocally(long blogId) {
        addBlockedBlog(blogId);
    }

    public static void whitelistBlogLocally(long blogId) {
        removeBlockedBlog(blogId);
    }

    public static boolean isBlockedBlog(ReaderPost post) {
        return isBlockedBlog(post.blogId);
    }
}
