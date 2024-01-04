package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.Nullable;

import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.SqlUtils;

/**
 * stores comments on reader posts
 */
public class ReaderCommentTable {
    private static final String COLUMN_NAMES =
            " blog_id,"
            + " post_id,"
            + " comment_id,"
            + " parent_id,"
            + " author_name,"
            + " author_avatar,"
            + " author_url,"
            + " author_id,"
            + " author_blog_id,"
            + " published,"
            + " timestamp,"
            + " status,"
            + " text,"
            + " num_likes,"
            + " is_liked,"
            + " page_number,"
            + " short_url,"
            + " author_email";


    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_comments ("
                   + " blog_id INTEGER DEFAULT 0,"
                   + " post_id INTEGER DEFAULT 0,"
                   + " comment_id INTEGER DEFAULT 0,"
                   + " parent_id INTEGER DEFAULT 0,"
                   + " author_name TEXT,"
                   + " author_avatar TEXT,"
                   + " author_url TEXT,"
                   + " author_id INTEGER DEFAULT 0,"
                   + " author_blog_id INTEGER DEFAULT 0,"
                   + " published TEXT,"
                   + " timestamp INTEGER DEFAULT 0,"
                   + " status TEXT,"
                   + " text TEXT,"
                   + " num_likes INTEGER DEFAULT 0,"
                   + " is_liked INTEGER DEFAULT 0,"
                   + " page_number INTEGER DEFAULT 0,"
                   + " short_url TEXT,"
                   + " author_email TEXT,"
                   + " PRIMARY KEY (blog_id, post_id, comment_id))");
        db.execSQL("CREATE INDEX idx_page_number ON tbl_comments(page_number)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_comments");
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    protected static int purge(SQLiteDatabase db) {
        // purge comments attached to posts that no longer exist
        int numDeleted = db.delete("tbl_comments", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);

        // purge all but the first page of comments
        numDeleted += db.delete("tbl_comments", "page_number != 1", null);

        return numDeleted;
    }

    public static boolean isEmpty() {
        return (getNumComments() == 0);
    }

    private static int getNumComments() {
        long count = SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_comments");
        return (int) count;
    }

    /*
     * returns the highest page_number for comments on the passed post
     */
    public static int getLastPageNumberForPost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT MAX(page_number) FROM tbl_comments WHERE blog_id=? AND post_id=?", args);
    }

    /*
     * returns the page number for a specific comment
     */
    public static int getPageNumberForComment(long blogId, long postId, long commentId) {
        String[] args = {Long.toString(blogId), Long.toString(postId), Long.toString(commentId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT page_number FROM tbl_comments "
                                    + " WHERE blog_id=? AND post_id=? AND comment_id=?",
                                    args);
    }

    /*
     * removes all comments for the passed post
     */
    public static void purgeCommentsForPost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        ReaderDatabase.getWritableDb().delete("tbl_comments", "blog_id=? AND post_id=?", args);
    }

    /*
     * returns the #comments stored locally for this post, which may differ from ReaderPostTable.getNumCommentsOnPost
     * (which is the #comments the server says exist for this post)
     */
    public static int getNumCommentsForPost(ReaderPost post) {
        if (post == null) {
            return 0;
        }
        return getNumCommentsForPost(post.blogId, post.postId);
    }

    private static int getNumCommentsForPost(long blogId, long postId) {
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT count(*) FROM tbl_comments WHERE blog_id=? AND post_id=?", args);
    }

    public static ReaderCommentList getCommentsForPost(ReaderPost post) {
        if (post == null) {
            return new ReaderCommentList();
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId), CommentStatus.APPROVED.toString()};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(
                "SELECT * FROM tbl_comments WHERE blog_id=? AND post_id=? AND status =? ORDER BY timestamp", args);
        try {
            ReaderCommentList comments = new ReaderCommentList();
            if (c.moveToFirst()) {
                do {
                    comments.add(getCommentFromCursor(c));
                } while (c.moveToNext());
            }
            return comments;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    @Nullable
    public static ReaderCommentList getCommentsForPostSnippet(ReaderPost post, int limit) {
        if (post == null) {
            return new ReaderCommentList();
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId), Integer.toString(limit)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(
                "SELECT * FROM tbl_comments WHERE blog_id=? AND post_id=? AND parent_id=0 ORDER BY timestamp LIMIT ?",
                args
        );
        try {
            ReaderCommentList comments = new ReaderCommentList();
            if (c.moveToFirst()) {
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
        if (comment == null) {
            return;
        }
        ReaderCommentList comments = new ReaderCommentList();
        comments.add(comment);
        addOrUpdateComments(comments);
    }

    public static void addOrUpdateComments(ReaderCommentList comments) {
        if (comments == null || comments.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_comments (" + COLUMN_NAMES + ") "
                                                   + "VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16,"
                                                   + "?17,?18)");
        try {
            for (ReaderComment comment : comments) {
                stmt.bindLong(1, comment.blogId);
                stmt.bindLong(2, comment.postId);
                stmt.bindLong(3, comment.commentId);
                stmt.bindLong(4, comment.parentId);
                stmt.bindString(5, comment.getAuthorName());
                stmt.bindString(6, comment.getAuthorAvatar());
                stmt.bindString(7, comment.getAuthorUrl());
                stmt.bindLong(8, comment.authorId);
                stmt.bindLong(9, comment.authorBlogId);
                stmt.bindString(10, comment.getPublished());
                stmt.bindLong(11, comment.timestamp);
                stmt.bindString(12, comment.getStatus());
                stmt.bindString(13, comment.getText());
                stmt.bindLong(14, comment.numLikes);
                stmt.bindLong(15, SqlUtils.boolToSql(comment.isLikedByCurrentUser));
                stmt.bindLong(16, comment.pageNumber);
                stmt.bindString(17, comment.getShortUrl());
                stmt.bindString(18, comment.getAuthorEmail());

                stmt.execute();
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    public static ReaderComment getComment(long blogId, long postId, long commentId) {
        String[] args = new String[]{Long.toString(blogId), Long.toString(postId), Long.toString(commentId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(
                "SELECT * FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id=? LIMIT 1", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getCommentFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void deleteComment(ReaderPost post, long commentId) {
        if (post == null) {
            return;
        }
        String[] args = {Long.toString(post.blogId), Long.toString(post.postId), Long.toString(commentId)};
        ReaderDatabase.getWritableDb().delete("tbl_comments", "blog_id=? AND post_id=? AND comment_id=?", args);
    }

    /*
     * returns the #likes known to exist for this comment
     */
    public static int getNumLikesForComment(long blogId, long postId, long commentId) {
        String[] args = {Long.toString(blogId),
                Long.toString(postId),
                Long.toString(commentId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                                    "SELECT num_likes FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id=?",
                                    args);
    }

    /*
     * updates both the like count for a comment and whether it's liked by the current user
     */
    public static void setLikesForComment(ReaderComment comment, int numLikes, boolean isLikedByCurrentUser) {
        if (comment == null) {
            return;
        }

        String[] args =
                {Long.toString(comment.blogId),
                        Long.toString(comment.postId),
                        Long.toString(comment.commentId)};

        ContentValues values = new ContentValues();
        values.put("num_likes", numLikes);
        values.put("is_liked", SqlUtils.boolToSql(isLikedByCurrentUser));

        ReaderDatabase.getWritableDb().update(
                "tbl_comments",
                values,
                "blog_id=? AND post_id=? AND comment_id=?",
                args);
    }

    public static boolean isCommentLikedByCurrentUser(ReaderComment comment) {
        if (comment == null) {
            return false;
        }
        return isCommentLikedByCurrentUser(comment.blogId, comment.postId, comment.commentId);
    }

    public static boolean isCommentLikedByCurrentUser(long blogId, long postId, long commentId) {
        String[] args = {Long.toString(blogId),
                Long.toString(postId),
                Long.toString(commentId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                                     "SELECT is_liked FROM tbl_comments WHERE blog_id=? AND post_id=? and comment_id=?",
                                     args);
    }

    public static boolean commentExists(long blogId, long postId, long commentId) {
        String[] args = {Long.toString(blogId),
                Long.toString(postId),
                Long.toString(commentId)};

        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                                     "SELECT 1 FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id=?", args);
    }

    private static ReaderComment getCommentFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("null comment cursor");
        }

        ReaderComment comment = new ReaderComment();

        comment.commentId = c.getLong(c.getColumnIndexOrThrow("comment_id"));
        comment.blogId = c.getLong(c.getColumnIndexOrThrow("blog_id"));
        comment.postId = c.getLong(c.getColumnIndexOrThrow("post_id"));
        comment.parentId = c.getLong(c.getColumnIndexOrThrow("parent_id"));

        comment.setPublished(c.getString(c.getColumnIndexOrThrow("published")));
        comment.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"));

        comment.setAuthorAvatar(c.getString(c.getColumnIndexOrThrow("author_avatar")));
        comment.setAuthorName(c.getString(c.getColumnIndexOrThrow("author_name")));
        comment.setAuthorUrl(c.getString(c.getColumnIndexOrThrow("author_url")));
        comment.authorId = c.getLong(c.getColumnIndexOrThrow("author_id"));
        comment.authorBlogId = c.getLong(c.getColumnIndexOrThrow("author_blog_id"));

        comment.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
        comment.setText(c.getString(c.getColumnIndexOrThrow("text")));

        comment.numLikes = c.getInt(c.getColumnIndexOrThrow("num_likes"));
        comment.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow("is_liked")));
        comment.pageNumber = c.getInt(c.getColumnIndexOrThrow("page_number"));

        comment.setShortUrl(c.getString(c.getColumnIndexOrThrow("short_url")));
        comment.setAuthorEmail(c.getString(c.getColumnIndexOrThrow("author_email")));

        return comment;
    }
}
