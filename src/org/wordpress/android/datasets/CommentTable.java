package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;

import java.util.Map;

/**
 * Created by nbradbury on 1/30/14.
 */
public class CommentTable {
    private static final String COMMENTS_TABLE = "comments";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + COMMENTS_TABLE + " ("
                 + "   blog_id      INTEGER DEFAULT 0," // <- local blog id
                 + "   post_id      INTEGER DEFAULT 0,"
                 + "   comment_id   INTEGER DEFAULT 0,"
                 + "   comment      TEXT,"
                 + "   commentDate  TEXT,"
                 + "   commentDateFormatted TEXT,"
                 + "   status       TEXT,"
                 + "   author_name  TEXT,"
                 + "   author_url   TEXT,"
                 + "   author_email TEXT,"
                 + "   post_title   TEXT,"
                 + " PRIMARY KEY (blog_id, post_id, comment_id)"
                 + " );");
    }

    protected static void dropTables(SQLiteDatabase db) {
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

    /**
     * nbradbury 11/15/13 - add a single comment
     * @param localBlogId - unique id in account table for the blog the comment is from
     * @param comment - comment object to store
     */
    public static void addComment(int localBlogId, final Comment comment) {
        if (comment == null)
            return;

        ContentValues values = new ContentValues();
        values.put("blog_id", localBlogId);
        values.put("post_id", comment.postID);
        values.put("comment_id", comment.commentID);
        values.put("author_name", StringUtils.notNullStr(comment.authorName));
        values.put("author_url", StringUtils.notNullStr(comment.authorURL));
        values.put("comment", StringUtils.notNullStr(comment.comment));
        values.put("status", StringUtils.notNullStr(comment.getStatus()));
        values.put("author_email", StringUtils.notNullStr(comment.authorEmail));
        values.put("post_title", StringUtils.notNullStr(comment.postTitle));
        values.put("commentDateFormatted", StringUtils.notNullStr(comment.dateCreatedFormatted));
        // TODO: store actual date here
        values.put("commentDate", StringUtils.notNullStr(comment.dateCreatedFormatted));

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

    public static CommentList loadComments(int localBlogId) {
        CommentList comments = new CommentList();

        String[] args = {Integer.toString(localBlogId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + COMMENTS_TABLE + " WHERE blog_id=?", args);

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

    public static void deleteCommentsForBlog(int localBlogId) {
        getWritableDb().delete(COMMENTS_TABLE, "blog_id=?", new String[]{Integer.toString(localBlogId)});
    }

    /*public static boolean saveComments(List<?> commentValues) {
        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            try {
                for (int i = 0; i < commentValues.size(); i++) {
                    ContentValues values = new ContentValues();
                    Map<?, ?> thisHash = (Map<?, ?>) commentValues.get(i);
                    values.put("blog_id", thisHash.get("blog_id").toString());
                    values.put("post_id", thisHash.get("post_id").toString());
                    values.put("comment_id", thisHash.get("comment_id").toString());
                    values.put("author_name", thisHash.get("author_name").toString());
                    values.put("comment", thisHash.get("comment").toString());
                    values.put("commentDate", thisHash.get("commentDate").toString());
                    values.put("commentDateFormatted",
                            thisHash.get("commentDateFormatted").toString());
                    values.put("status", thisHash.get("status").toString());
                    values.put("author_url", thisHash.get("author_url").toString());
                    values.put("author_email", thisHash.get("author_email").toString());
                    values.put("post_title", thisHash.get("post_title").toString());

                    db.insertWithOnConflict(COMMENTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }

                db.setTransactionSuccessful();
                return true;
            } catch (SQLiteException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            }
        } finally {
            db.endTransaction();
        }
    }*/

    public static boolean saveComments(int localBlogId, CommentList comments) {
        if (comments == null || comments.size() == 0)
            return false;

        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            try {
                for (Comment comment: comments) {
                    ContentValues values = new ContentValues();

                    values.put("blog_id", localBlogId);
                    values.put("post_id", comment.postID);
                    values.put("comment_id", comment.commentID);
                    values.put("comment", comment.comment);
                    values.put("commentDateFormatted", comment.dateCreatedFormatted);
                    values.put("status", comment.getStatus());
                    values.put("author_name", comment.authorName);
                    values.put("author_url", comment.authorURL);
                    values.put("author_email", comment.authorEmail);
                    values.put("post_title", comment.postTitle);

                    db.insertWithOnConflict(COMMENTS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }

                db.setTransactionSuccessful();
                return true;
            } catch (SQLiteException e) {
                AppLog.e(AppLog.T.COMMENTS, e);
                return false;
            }
        } finally {
            db.endTransaction();
        }
    }

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

    public static int getUnmoderatedCommentCount(int localBlogId) {
        String sql = "SELECT COUNT(*) FROM " + COMMENTS_TABLE + " WHERE blog_id=? AND status=?";
        String[] args = {Integer.toString(localBlogId), "hold"};
        return SqlUtils.intForQuery(getReadableDb(), sql, args);
    }

    private static Comment getCommentFromCursor(Cursor c) {
        String authorName = c.getString(c.getColumnIndex("author_name"));
        String content = c.getString(c.getColumnIndex("comment"));
        String dateCreatedFormatted = c.getString(c.getColumnIndex("commentDateFormatted"));
        String status = c.getString(c.getColumnIndex("status"));
        String authorUrl = c.getString(c.getColumnIndex("author_url"));
        String authorEmail = c.getString(c.getColumnIndex("author_email"));
        String postTitle = c.getString(c.getColumnIndex("post_title"));
        int postId = c.getInt(c.getColumnIndex("post_id"));
        int commentId = c.getInt(c.getColumnIndex("comment_id"));
        int localBlogId = c.getInt(c.getColumnIndex("blog_id"));

        return new Comment(
                postId,
                commentId,
                authorName,
                dateCreatedFormatted,
                content,
                status,
                postTitle,
                authorUrl,
                authorEmail,
                null);
    }
}
