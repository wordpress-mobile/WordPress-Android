package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.wordpress.android.util.SqlUtils;

/**
 * Created by nbradbury on 7/18/13.
 * stores likes for Reader posts
 */
public class ReaderLikeTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_post_likes ("
                + " post_id        INTEGER,"
                + " blog_id        INTEGER,"
                + " user_id        INTEGER,"
                + " PRIMARY KEY (blog_id, post_id, user_id))");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_post_likes");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    /*
     * purge likes attached to posts that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_post_likes", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
    }

    /*
     * returns userIds of users who like the passed post
     */
    public static ReaderUserIdList getLikesForPost(ReaderPost post) {
        if (post==null)
            return new ReaderUserIdList();

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT user_id FROM tbl_post_likes WHERE blog_id=? AND post_id=?", args);
        try {
            ReaderUserIdList userIds = new ReaderUserIdList();
            if (c.moveToFirst()) {
                do {
                    userIds.add(c.getLong(0));
                } while (c.moveToNext());
            }

            return userIds;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static int getNumLikesForPost(ReaderPost post) {
        if (post==null)
            return 0;
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT count(*) FROM tbl_post_likes WHERE blog_id=? AND post_id=?", args);
    }

    /*
     * returns true if the passed user likes the passed post
     */
    private static boolean isLikedByUser(ReaderPost post, long userId) {
        if (post==null)
            return false;
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(userId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_post_likes WHERE blog_id=? AND post_id=? AND user_id=?", args);
    }

    public static void setCurrentUserLikesPost(ReaderPost post, boolean isLiked) {
        if (post==null)
            return;
        long userId = ReaderPrefs.getCurrentUserId();
        if (isLiked) {
            ContentValues values = new ContentValues();
            values.put("blog_id", post.blogId);
            values.put("post_id", post.postId);
            values.put("user_id", userId);
            ReaderDatabase.getWritableDb().insert("tbl_post_likes", null, values);
        } else {
            String args[] = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(userId)};
            ReaderDatabase.getWritableDb().delete("tbl_post_likes", "blog_id=? AND post_id=? AND user_id=?", args);
        }
    }

    public static void setLikesForPost(ReaderPost post, ReaderUserIdList userIds) {
        if (post==null)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_post_likes (blog_id, post_id, user_id) VALUES (?1,?2,?3)");
        try {
            // first delete all likes for this post
            String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
            db.delete("tbl_post_likes", "blog_id=? AND post_id=?", args);

            // now insert the passed likes
            if (userIds!=null) {
                for (Long userId: userIds) {
                    stmt.bindLong(1, post.blogId);
                    stmt.bindLong(2, post.postId);
                    stmt.bindLong(3, userId);
                    stmt.execute();
                    stmt.clearBindings();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

}


