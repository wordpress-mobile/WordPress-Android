package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.SqlUtils;

/**
 * Created by nbradbury on 7/8/13.
 * stores comments on reader posts
 */
public class ReaderCommentTable {

    private static final String COLUMN_NAMES =
            "blog_id, post_id, comment_id, parent_id, author_name, author_avatar, author_url, published, timestamp, status, text";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_comments ("
                + " blog_id             INTEGER DEFAULT 0,"
                + " post_id             INTEGER DEFAULT 0,"
                + "	comment_id		    INTEGER DEFAULT 0,"
                + " parent_id           INTEGER DEFAULT 0,"
                + "	author_name	        TEXT,"
                + " author_avatar       TEXT,"
                + "	author_url	        TEXT,"
                + " published           TEXT,"
                + " timestamp           INTEGER DEFAULT 0,"
                + " status              TEXT,"
                + " text                TEXT,"
                + " PRIMARY KEY (blog_id, post_id, comment_id))");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_comments");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    public static boolean isEmpty() {
        return (getNumComments()==0);
    }

    public static int getNumComments() {
        long count = SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_comments");
        return (int)count;
    }

    /*
     * returns the #comments stored locally for this post, which may differ from ReaderPostTable.getNumCommentsOnPost
     * (which is the #comments the server says exist for this post)
     */
    public static int getNumCommentsForPost(ReaderPost post) {
        if (post==null)
            return 0;
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT count(*) FROM tbl_comments WHERE blog_id=? AND post_id=?", args);
    }

    public static ReaderCommentList getCommentsForPost(ReaderPost post) {
        if (post==null)
            return new ReaderCommentList();

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_comments WHERE blog_id=? AND post_id=? ORDER BY timestamp", args);
        try {
            ReaderCommentList comments = new ReaderCommentList();
            if (c.moveToFirst()) {
                resetColumnIndexes(c);
                do {
                    comments.add(getCommentFromCursor(c));
                } while (c.moveToNext());
            }
            return comments;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void addOrUpdateComment(ReaderComment comment) {
        if (comment==null)
            return;
        ReaderCommentList comments = new ReaderCommentList();
        comments.add(comment);
        addOrUpdateComments(comments);
    }

    public static void addOrUpdateComments(ReaderCommentList comments) {
        if (comments==null || comments.size()==0)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_comments ("
                                                  + COLUMN_NAMES
                                                  + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11)");
        try {
            for (ReaderComment comment: comments) {
                stmt.bindLong  (1, comment.blogId);
                stmt.bindLong  (2, comment.postId);
                stmt.bindLong  (3, comment.commentId);
                stmt.bindLong  (4, comment.parentId);
                stmt.bindString(5, comment.getAuthorName());
                stmt.bindString(6, comment.getAuthorAvatar());
                stmt.bindString(7, comment.getAuthorUrl());
                stmt.bindString(8, comment.getPublished());
                stmt.bindLong  (9, comment.timestamp);
                stmt.bindString(10, comment.getStatus());
                stmt.bindString(11, comment.getText());

                stmt.execute();
                stmt.clearBindings();
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns the # of parents a comments has, used to determine its indentation when displayed
     */
    public static int getNumParentsForComment(ReaderPost post, long commentId) {
        if (post==null)
            return 0;

        int indentLevel = -1;
        do {
            indentLevel++;
            String[] args = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(commentId)};
            commentId = SqlUtils.longForQuery(ReaderDatabase.getReadableDb(), "SELECT parent_id FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id=?", args);
        } while (commentId!=0);

        return indentLevel;
    }

    /*
     * purge comments attached to posts that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_comments", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
    }

    public static void deleteComment(ReaderPost post, long commentId) {
        if (post==null)
            return;
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(commentId)};
        ReaderDatabase.getWritableDb().delete("tbl_comments", "blog_id=? AND post_id=? AND comment_id=?", args);
    }

    private static int COL_COMMENT_ID;
    private static int COL_BLOG_ID;
    private static int COL_POST_ID;
    private static int COL_PARENT_ID;
    private static int COL_STATUS;
    private static int COL_PUBLISHED;
    private static int COL_TIMESTAMP;
    private static int COL_AUTHOR_AVATAR;
    private static int COL_AUTHOR_NAME;
    private static int COL_AUTHOR_URL;
    private static int COL_TEXT;

    // see comment in ReaderPostTable.java for purpose
    private static void resetColumnIndexes(Cursor c) {
        COL_COMMENT_ID = c.getColumnIndex("comment_id");
        COL_BLOG_ID = c.getColumnIndex("blog_id");
        COL_POST_ID = c.getColumnIndex("post_id");
        COL_PARENT_ID = c.getColumnIndex("parent_id");
        COL_PUBLISHED = c.getColumnIndex("published");
        COL_TIMESTAMP = c.getColumnIndex("timestamp");
        COL_AUTHOR_AVATAR = c.getColumnIndex("author_avatar");
        COL_AUTHOR_NAME = c.getColumnIndex("author_name");
        COL_AUTHOR_URL = c.getColumnIndex("author_url");
        COL_STATUS = c.getColumnIndex("status");
        COL_TEXT = c.getColumnIndex("text");
    }

    public static ReaderComment getCommentFromCursor(Cursor c) {
        if (c==null)
            throw new IllegalArgumentException("null comment cursor");

        ReaderComment comment = new ReaderComment();

        comment.commentId = c.getLong(COL_COMMENT_ID);
        comment.blogId = c.getLong(COL_BLOG_ID);
        comment.postId = c.getLong(COL_POST_ID);
        comment.parentId = c.getLong(COL_PARENT_ID);

        comment.setPublished(c.getString(COL_PUBLISHED));
        comment.timestamp = c.getLong(COL_TIMESTAMP);

        comment.setAuthorAvatar(c.getString(COL_AUTHOR_AVATAR));
        comment.setAuthorName(c.getString(COL_AUTHOR_NAME));
        comment.setAuthorUrl(c.getString(COL_AUTHOR_URL));
        comment.setStatus(c.getString(COL_STATUS));
        comment.setText(c.getString(COL_TEXT));

        return comment;
    }
}
