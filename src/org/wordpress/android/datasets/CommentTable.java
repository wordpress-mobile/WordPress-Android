package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Comment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Created by nbradbury on 1/30/14.
 */
public class CommentTable {
    private static final String COMMENTS_TABLE = "comments";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + COMMENTS_TABLE + " ("
                 + "   blogID text,"
                 + "   postID text,"
                 + "   iCommentID integer,"
                 + "   author text,"
                 + "   comment text,"
                 + "   commentDate text,"
                 + "   commentDateFormatted text,"
                 + "   status text,"
                 + "   url text,"
                 + "   email text,"
                 + "   postTitle text,"
                 + "   PRIMARY KEY (blogID, postID, iCommentID)"
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
    public static void addComment(int localBlogId, Comment comment) {
        if (comment == null)
            return;

        // first delete existing comment (necessary since there's no primary key or indexes
        // on this table, which means we can't rely on using CONFLICT_REPLACE below)
        deleteComment(localBlogId, comment.commentID);

        ContentValues values = new ContentValues();
        values.put("blogID", localBlogId);
        values.put("postID", StringUtils.notNullStr(comment.postID));
        values.put("iCommentID", comment.commentID);
        values.put("author", StringUtils.notNullStr(comment.name));
        values.put("url", StringUtils.notNullStr(comment.authorURL));
        values.put("comment", StringUtils.notNullStr(comment.comment));
        values.put("status", StringUtils.notNullStr(comment.getStatus()));
        values.put("email", StringUtils.notNullStr(comment.authorEmail));
        values.put("postTitle", StringUtils.notNullStr(comment.postTitle));
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
        String[] cols = {"author",
                "comment",
                "commentDateFormatted",
                "status",
                "url",
                "email",
                "postTitle",
                "postID"};
        String[] args = {Integer.toString(localBlogId),
                Integer.toString(commentId)};
        Cursor c = getReadableDb().query(COMMENTS_TABLE,
                cols,
                "blogID=? AND iCommentID=?",
                args,
                null, null, null);

        if (!c.moveToFirst())
            return null;

        String authorName = c.getString(0);
        String content = c.getString(1);
        String dateCreatedFormatted = c.getString(2);
        String status = c.getString(3);
        String authorUrl = c.getString(4);
        String authorEmail = c.getString(5);
        String postTitle = c.getString(6);
        String postId = c.getString(7);

        return new Comment(postId,
                           commentId,
                           0,
                           authorName,
                           dateCreatedFormatted,
                           content,
                           status,
                           postTitle,
                           authorUrl,
                           authorEmail,
                           null);
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
        int count = getWritableDb().delete(COMMENTS_TABLE, "blogID=? AND iCommentID=?", args);
        return (count > 0);
    }

    public static List<Map<String, Object>> loadComments(int blogID) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c = getReadableDb().query(COMMENTS_TABLE,
                new String[]{"blogID", "postID", "iCommentID", "author",
                             "comment", "commentDate", "commentDateFormatted",
                             "status", "url", "email", "postTitle"}, "blogID="
                + blogID, null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; i++) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("blogID", c.getString(0));
                returnHash.put("postID", c.getInt(1));
                returnHash.put("commentID", c.getInt(2));
                returnHash.put("author", c.getString(3));
                returnHash.put("comment", c.getString(4));
                returnHash.put("commentDate", c.getString(5));
                returnHash.put("commentDateFormatted", c.getString(6));
                returnHash.put("status", c.getString(7));
                returnHash.put("url", c.getString(8));
                returnHash.put("email", c.getString(9));
                returnHash.put("postTitle", c.getString(10));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public static boolean saveComments(List<?> commentValues) {
        SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            try {
                for (int i = 0; i < commentValues.size(); i++) {
                    ContentValues values = new ContentValues();
                    Map<?, ?> thisHash = (Map<?, ?>) commentValues.get(i);
                    values.put("blogID", thisHash.get("blogID").toString());
                    values.put("postID", thisHash.get("postID").toString());
                    values.put("iCommentID", thisHash.get("commentID").toString());
                    values.put("author", thisHash.get("author").toString());
                    values.put("comment", thisHash.get("comment").toString());
                    values.put("commentDate", thisHash.get("commentDate").toString());
                    values.put("commentDateFormatted",
                            thisHash.get("commentDateFormatted").toString());
                    values.put("status", thisHash.get("status").toString());
                    values.put("url", thisHash.get("url").toString());
                    values.put("email", thisHash.get("email").toString());
                    values.put("postTitle", thisHash.get("postTitle").toString());

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

    public static void updateComment(int blogID, int id, Map<?, ?> commentHash) {
        ContentValues values = new ContentValues();
        values.put("author", commentHash.get("author").toString());
        values.put("comment", commentHash.get("comment").toString());
        values.put("status", commentHash.get("status").toString());
        values.put("url", commentHash.get("url").toString());
        values.put("email", commentHash.get("email").toString());

        getWritableDb().update(COMMENTS_TABLE, values, "blogID=" + blogID
                + " AND iCommentID=" + id, null);

    }

    public static void updateCommentStatus(int blogID, int id, String newStatus) {
        ContentValues values = new ContentValues();
        values.put("status", newStatus);
        getWritableDb().update(COMMENTS_TABLE, values, "blogID=" + blogID
                + " AND iCommentID=" + id, null);

    }

    public static int getUnmoderatedCommentCount(int blogID) {
        String sql = "SELECT COUNT(*) FROM " + COMMENTS_TABLE + " WHERE blogID=? AND status=?";
        String[] args = {Integer.toString(blogID), "hold"};
        return SqlUtils.intForQuery(getReadableDb(), sql, args);
    }

}
