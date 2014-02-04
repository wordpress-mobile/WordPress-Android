package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.Map;

/**
 * Created by nbradbury on 1/30/14.
 */
public class CommentTable {
    private static final String COMMENTS_TABLE = "comments";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + COMMENTS_TABLE + " ("
                 + "    blog_id      INTEGER DEFAULT 0,"
                 + "    post_id      INTEGER DEFAULT 0,"
                 + "    comment_id   INTEGER DEFAULT 0,"
                 + "    comment      TEXT,"
                 + "    published    TEXT,"
                 + "    status       TEXT,"
                 + "    author_name  TEXT,"
                 + "    author_url   TEXT,"
                 + "    author_email TEXT,"
                 + "    post_title   TEXT,"
                 + "    PRIMARY KEY (blog_id, post_id, comment_id)"
                 + " );");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + COMMENTS_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.COMMENTS, "resetting comment table");
        dropTables(db);
        createTables(db);
    }

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }
    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    /*
     * purge comments attached to blogs that no longer exist, and remove older comments
     * TODO: call after hiding or deleting blogs
     */
    private static final int MAX_COMMENTS = 1000;
    public static int purge(SQLiteDatabase db) {
        int numDeleted = 0;

        // get rid of comments on blogs that don't exist or are hidden
        String sql = " blog_id NOT IN (SELECT DISTINCT id FROM " + WordPressDB.SETTINGS_TABLE
                   + " WHERE " + WordPressDB.COLNAME_IS_HIDDEN + " = 0)";
        numDeleted += db.delete(COMMENTS_TABLE, sql, null);

        // get rid of older comments if we've reached the max
        int numExisting = (int)SqlUtils.getRowCount(db, COMMENTS_TABLE);
        if (numExisting > MAX_COMMENTS) {
            int numToPurge = numExisting - MAX_COMMENTS;
            sql = " comment_id IN (SELECT DISTINCT comment_id FROM " + COMMENTS_TABLE
                + " ORDER BY published LIMIT " + Integer.toString(numToPurge) + ")";
            numDeleted += db.delete(COMMENTS_TABLE, sql, null);
        }

        return numDeleted;
    }

    /**
     * nbradbury 11/15/13 - add a single comment
     * @param localBlogId - unique id in account table for the blog the comment is from
     * @param comment - comment object to store
     */
    public static void addComment(int localBlogId, final Comment comment) {
        if (comment == null)
            return;

        ContentValues values = new ContentValues();
        values.put("blog_id",       localBlogId);
        values.put("post_id",       comment.postID);
        values.put("comment_id",    comment.commentID);
        values.put("author_name",   comment.getAuthorName());
        values.put("author_url",    comment.getAuthorUrl());
        values.put("comment",       comment.getCommentText());
        values.put("status",        comment.getStatus());
        values.put("author_email",  comment.getAuthorEmail());
        values.put("post_title",    comment.getPostTitle());
        values.put("published",     comment.getPublished());

        getWritableDb().insertWithOnConflict(COMMENTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * nbradbury 11/11/13 - retrieve a single comment
     * @param localBlogId - unique id in account table for the blog the comment is from
     * @param commentId - commentId of the actual comment
     * @return Comment if found, null otherwise
     */
    public static Comment getComment(int localBlogId, int commentId) {
        String[] args = {Integer.toString(localBlogId), Integer.toString(commentId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + COMMENTS_TABLE + " WHERE blog_id=? AND comment_id=?", args);
        try {
            if (!c.moveToFirst())
                return null;
            return getCommentFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /**
     * nbradbury - get all comments for a blog
     * @param localBlogId - unique id in account table for this blog
     * @return list of comments for this blog
     */
    public static CommentList getCommentsForBlog(int localBlogId) {
        CommentList comments = new CommentList();

        String[] args = {Integer.toString(localBlogId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + COMMENTS_TABLE + " WHERE blog_id=? ORDER BY published DESC", args);

        try {
            if (c.moveToFirst()) {
                do {
                    Comment comment = getCommentFromCursor(c);
                    comments.add(comment);
                } while (c.moveToNext());
            }

            return comments;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /**
    * nbradbury - delete all comments for a blog
    * @param localBlogId - unique id in account table for this blog
    * @return number of comments deleted
     */
    public static int deleteCommentsForBlog(int localBlogId) {
        return getWritableDb().delete(COMMENTS_TABLE, "blog_id=?", new String[]{Integer.toString(localBlogId)});
    }

    /**
     * nbradbury - saves comments for passed blog to local db, overwriting existing ones if necessary
     * @param localBlogId - unique id in account table for this blog
     * @param comments - list of comments to save
     * @return true if saved, false on failure
     */
    public static boolean saveComments(int localBlogId, final CommentList comments) {
        if (comments == null || comments.size() == 0)
            return false;

        final String sql = " INSERT OR REPLACE INTO " + COMMENTS_TABLE + "("
                         + " blog_id,"       // 1
                         + " post_id,"       // 2
                         + " comment_id,"    // 3
                         + " comment,"       // 4
                         + " published,"     // 5
                         + " status,"        // 6
                         + " author_name,"   // 7
                         + " author_url,"    // 8
                         + " author_email,"  // 9
                         + " post_title"     // 10
                         + " ) VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10)";

        SQLiteDatabase db = getWritableDb();
        SQLiteStatement stmt = db.compileStatement(sql);
        db.beginTransaction();
        try {
            try {
                for (Comment comment: comments) {
                    stmt.bindLong  (1, localBlogId);
                    stmt.bindLong  (2, comment.postID);
                    stmt.bindLong  (3, comment.commentID);
                    stmt.bindString(4, comment.getCommentText());
                    stmt.bindString(5, comment.getPublished());
                    stmt.bindString(6, comment.getStatus());
                    stmt.bindString(7, comment.getAuthorName());
                    stmt.bindString(8, comment.getAuthorUrl());
                    stmt.bindString(9, comment.getAuthorEmail());
                    stmt.bindString(10, comment.getPostTitle());
                    stmt.execute();
                }

                db.setTransactionSuccessful();
                return true;
            } catch (SQLiteException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            }
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    /**
     * nbradbury - updates the passed comment
     * @param localBlogId - unique id in account table for this blog
     * @param commentId - id of comment (returned by api)
     * @param commentHash - values to store
     */
    // TODO: replace commentHash with Comment object
    public static void updateComment(int localBlogId, int commentId, Map<?, ?> commentHash) {
        ContentValues values = new ContentValues();

        values.put("author_name", commentHash.get("author").toString());
        values.put("comment", commentHash.get("comment").toString());
        values.put("status", commentHash.get("status").toString());
        values.put("author_url", commentHash.get("url").toString());
        values.put("author_email", commentHash.get("email").toString());

        String[] args = {Integer.toString(localBlogId), Integer.toString(commentId)};
        getWritableDb().update(COMMENTS_TABLE, values, "blog_id=? AND comment_id=?", args);

    }

    /**
     * nbradbury - updates the status for the passed comment
     * @param localBlogId - unique id in account table for this blog
     * @param commentId - id of comment (returned by api)
     * @param newStatus - status to change to
     */
    public static void updateCommentStatus(int localBlogId, int commentId, String newStatus) {
        ContentValues values = new ContentValues();
        values.put("status", newStatus);
        String[] args = {Integer.toString(localBlogId),
                         Integer.toString(commentId)};
        getWritableDb().update(COMMENTS_TABLE,
                               values,
                               "blog_id=? AND comment_id=?",
                               args);

    }

    /**
     * nbradbury - updates the status for the passed list of comments
     * @param localBlogId - unique id in account table for this blog
     * @param comments - list of comments to update
     * @param newStatus - status to change to
     */
    public static void updateCommentsStatus(int localBlogId, final CommentList comments, String newStatus) {
        if (comments == null || comments.size() == 0)
            return;
        getWritableDb().beginTransaction();
        try {
            for (Comment comment: comments) {
                updateCommentStatus(localBlogId, comment.commentID, newStatus);
            }
            getWritableDb().setTransactionSuccessful();
        } finally {
            getWritableDb().endTransaction();
        }
    }

    /**
     * nbradbury 11/12/13 - delete a single comment
     * @param localBlogId - unique id in account table for this blog
     * @param commentId - commentId of the actual comment
     * @return true if comment deleted, false otherwise
     */
    public static boolean deleteComment(int localBlogId, int commentId) {
        String[] args = {Integer.toString(localBlogId),
                         Integer.toString(commentId)};
        int count = getWritableDb().delete(COMMENTS_TABLE, "blog_id=? AND comment_id=?", args);
        return (count > 0);
    }

    /**
     * nbradbury - delete a list of comments
     * @param localBlogId - unique id in account table for this blog
     * @param comments - list of comments to delete
     */
    public static void deleteComments(int localBlogId, final CommentList comments) {
        if (comments == null || comments.size() == 0)
            return;
        getWritableDb().beginTransaction();
        try {
            for (Comment comment: comments) {
                deleteComment(localBlogId, comment.commentID);
            }
            getWritableDb().setTransactionSuccessful();
        } finally {
            getWritableDb().endTransaction();
        }
    }

    /**
     * nbradbury - returns the number of unmoderated comments for a specific blog
     * @param localBlogId - unique id in account table for this blog
     */
    public static int getUnmoderatedCommentCount(int localBlogId) {
        String sql = "SELECT COUNT(*) FROM " + COMMENTS_TABLE + " WHERE blog_id=? AND status=?";
        String[] args = {Integer.toString(localBlogId), "hold"};
        return SqlUtils.intForQuery(getReadableDb(), sql, args);
    }

    private static Comment getCommentFromCursor(Cursor c) {
        final String authorName = c.getString(c.getColumnIndex("author_name"));
        final String content = c.getString(c.getColumnIndex("comment"));
        final String published = c.getString(c.getColumnIndex("published"));
        final String status = c.getString(c.getColumnIndex("status"));
        final String authorUrl = c.getString(c.getColumnIndex("author_url"));
        final String authorEmail = c.getString(c.getColumnIndex("author_email"));
        final String postTitle = c.getString(c.getColumnIndex("post_title"));

        int postId = c.getInt(c.getColumnIndex("post_id"));
        int commentId = c.getInt(c.getColumnIndex("comment_id"));

        // TODO: store localBlogId with comment
        //int localBlogId = c.getInt(c.getColumnIndex("blog_id"));

        return new Comment(
                postId,
                commentId,
                authorName,
                published,
                content,
                status,
                postTitle,
                authorUrl,
                authorEmail,
                null);
    }
}
