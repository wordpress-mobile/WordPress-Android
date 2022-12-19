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
