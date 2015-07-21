package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.SqlUtils;

/**
 * stores likes for Reader posts and comments
 */
public class ReaderLikeTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_post_likes ("
                + " post_id        INTEGER DEFAULT 0,"
                + " blog_id        INTEGER DEFAULT 0,"
                + " user_id        INTEGER DEFAULT 0,"
                + " PRIMARY KEY (blog_id, post_id, user_id))");

        db.execSQL("CREATE TABLE tbl_comment_likes ("
                + " comment_id     INTEGER DEFAULT 0,"
                + " blog_id        INTEGER DEFAULT 0,"
                + " user_id        INTEGER DEFAULT 0,"
                + " PRIMARY KEY (blog_id, comment_id, user_id))");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_post_likes");
        db.execSQL("DROP TABLE IF EXISTS tbl_comment_likes");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    /*
     * purge likes attached to posts/comments that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        int numDeleted = db.delete("tbl_post_likes", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
        numDeleted += db.delete("tbl_comment_likes", "comment_id NOT IN (SELECT DISTINCT comment_id FROM tbl_comments)", null);
        return numDeleted;
    }

    /*
     * returns userIds of users who like the passed post
     */
    public static ReaderUserIdList getLikesForPost(ReaderPost post) {
        ReaderUserIdList userIds = new ReaderUserIdList();
        if (post == null) {
            return userIds;
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT user_id FROM tbl_post_likes WHERE blog_id=? AND post_id=?", args);
        try {
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
        if (post == null) {
            return 0;
        }
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT count(*) FROM tbl_post_likes WHERE blog_id=? AND post_id=?", args);
    }

    public static void setCurrentUserLikesPost(ReaderPost post, boolean isLiked) {
        if (post == null) {
            return;
        }
        long currentUserId = AccountHelper.getDefaultAccount().getUserId();
        if (isLiked) {
            ContentValues values = new ContentValues();
            values.put("blog_id", post.blogId);
            values.put("post_id", post.postId);
            values.put("user_id", currentUserId);
            ReaderDatabase.getWritableDb().insert("tbl_post_likes", null, values);
        } else {
            String args[] = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(currentUserId)};
            ReaderDatabase.getWritableDb().delete("tbl_post_likes", "blog_id=? AND post_id=? AND user_id=?", args);
        }
    }

    public static void setLikesForPost(ReaderPost post, ReaderUserIdList userIds) {
        if (post == null) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_post_likes (blog_id, post_id, user_id) VALUES (?1,?2,?3)");
        try {
            // first delete all likes for this post
            String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
            db.delete("tbl_post_likes", "blog_id=? AND post_id=?", args);

            // now insert the passed likes
            if (userIds != null) {
                stmt.bindLong(1, post.blogId);
                stmt.bindLong(2, post.postId);
                for (Long userId: userIds) {
                    stmt.bindLong(3, userId);
                    stmt.execute();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }


    /****
     * comment likes
     */

    public static ReaderUserIdList getLikesForComment(ReaderComment comment) {
        ReaderUserIdList userIds = new ReaderUserIdList();
        if (comment == null) {
            return userIds;
        }

        String[] args = {Long.toString(comment.blogId),
                         Long.toString(comment.commentId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(
                "SELECT user_id FROM tbl_comment_likes WHERE blog_id=? AND comment_id=?", args);
        try {
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

    public static int getNumLikesForComment(ReaderComment comment) {
        if (comment == null) {
            return 0;
        }
        String[] args = {Long.toString(comment.blogId),
                         Long.toString(comment.commentId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM tbl_comment_likes WHERE blog_id=? AND comment_id=?", args);
    }

    public static void setCurrentUserLikesComment(ReaderComment comment, boolean isLiked) {
        if (comment == null) {
            return;
        }

        long currentUserId = AccountHelper.getDefaultAccount().getUserId();
        if (isLiked) {
            ContentValues values = new ContentValues();
            values.put("blog_id", comment.blogId);
            values.put("comment_id", comment.commentId);
            values.put("user_id", currentUserId);
            ReaderDatabase.getWritableDb().insert("tbl_comment_likes", null, values);
        } else {
            String args[] = {Long.toString(comment.blogId),
                             Long.toString(comment.commentId),
                             Long.toString(currentUserId)};
            ReaderDatabase.getWritableDb().delete("tbl_comment_likes",
                    "blog_id=? AND comment_id=? AND user_id=?", args);
        }
    }

    public static void setLikesForComment(ReaderComment comment, ReaderUserIdList userIds) {
        if (comment == null) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO tbl_comment_likes (blog_id, comment_id, user_id) VALUES (?1,?2,?3)");
        try {
            String[] args = {Long.toString(comment.blogId),
                             Long.toString(comment.commentId)};
            db.delete("tbl_comment_likes", "blog_id=? AND comment_id=?", args);

            if (userIds != null) {
                stmt.bindLong(1, comment.blogId);
                stmt.bindLong(2, comment.commentId);
                for (Long userId: userIds) {
                    stmt.bindLong(3, userId);
                    stmt.execute();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }
}


