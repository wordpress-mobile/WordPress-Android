package org.wordpress.android.datasets;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.IntDef;

import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderCommentList;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.SqlUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * stores comments on reader posts
 */
public class ReaderCommentTable {

    //comments ordering constants for retrieving comments from database
    public static final int ORDER_BY_NEWEST_COMMENT_FIRST = 1;
    public static final int ORDER_BY_TIME_OF_COMMENT = 2;
    public static final int ORDER_BY_DEFAULT = ORDER_BY_NEWEST_COMMENT_FIRST;

    @SuppressLint("UniqueConstants")
    @IntDef({ ORDER_BY_NEWEST_COMMENT_FIRST , ORDER_BY_TIME_OF_COMMENT , ORDER_BY_DEFAULT })
    @Retention(RetentionPolicy.SOURCE)
    public @interface CommentsOrderBy{}

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
                    + " page_number";


    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_comments ("
                + " blog_id             INTEGER DEFAULT 0,"
                + " post_id             INTEGER DEFAULT 0,"
                + "	comment_id		    INTEGER DEFAULT 0,"
                + " parent_id           INTEGER DEFAULT 0,"
                + "	author_name	        TEXT,"
                + " author_avatar       TEXT,"
                + "	author_url	        TEXT,"
                + " author_id           INTEGER DEFAULT 0,"
                + " author_blog_id      INTEGER DEFAULT 0,"
                + " published           TEXT,"
                + " timestamp           INTEGER DEFAULT 0,"
                + " status              TEXT,"
                + " text                TEXT,"
                + " num_likes           INTEGER DEFAULT 0,"
                + " is_liked            INTEGER DEFAULT 0,"
                + " page_number         INTEGER DEFAULT 0,"
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
        return (getNumComments()==0);
    }

    private static int getNumComments() {
        long count = SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_comments");
        return (int)count;
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
     * return first not loaded or first partially loaded page number
     * if numOfComments for a page is less than commentsPerPage then that page is partially loaded
     */
    public static int getFirstNotLoadedPage(long blogId, long postId, final int commentsPerPage){
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        String query = "SELECT page_number,COUNT(comment_id) FROM tbl_comments WHERE blog_id=? AND post_id=? GROUP BY page_number ORDER BY page_number";
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(query, args);

        int pageNumber = 1;
        for( ; c.moveToNext() ; pageNumber++){
            int currPage = c.getInt(0);
            int currPageComments = c.getInt(1);
            if( currPage != pageNumber || currPageComments < commentsPerPage ){
                break;
            }
        }

        c.close();
        return pageNumber;
    }

    /**
     * <p>
     * NOTE: method check pages in local database not on server if you want fresh result than load first page of comments by
     *          {@link org.wordpress.android.ui.reader.services.ReaderCommentService#startService(Context, long, long, boolean, int)}
     *          with {@link #ORDER_BY_NEWEST_COMMENT_FIRST} and requestNextPage = false
     * </p>
     * @param pageNumberFromBack return page number from back i.e. 1 for last page, 2 for second last page and so on.
     * @return
     * <ul>
     *     <li>last not loaded or last partially loaded page number</li>
     *     <li>-1 if all pages are loaded</li>
     *     <li>0 if no any comments is loaded</li>
     * </ul>
     */
    public static int getLastNotLoadedPage(long blogId,
                                           long postId,
                                           final int commentsPerPage,
                                           final boolean pageNumberFromBack){
        String[] args = {Long.toString(blogId), Long.toString(postId)};
        String query = "SELECT page_number,COUNT(comment_id) FROM tbl_comments WHERE blog_id=? AND post_id=? GROUP BY page_number ORDER BY page_number DESC";
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(query, args);

        if( !c.moveToNext() ){
            c.close();
            return 0;
        }

        /* NOTE: never perform ( currPageComments < commentsPerPage ) check for last page
            because it is always fully loaded and
            it always has less comments if total comments is not divisible by commentsPerPage
            e.g. totalComments = 153 , commentsPerPage = 20 , last page have 13 comments*/
        int lastPage = c.getInt(0);
        int currPage = -1;

        int backwardPageNumber = 2;
        for( ; c.moveToNext() ; backwardPageNumber++){
            currPage = c.getInt(0);
            int currPageComments = c.getInt(1);
            if( currPage + 1 != lastPage || currPageComments < commentsPerPage){
                c.close();
                return pageNumberFromBack ? backwardPageNumber : currPage;
            }
            lastPage = currPage;
        }

        c.close();

        if(currPage == 1){
            //all comments are loaded
            return -1;
        }

        return pageNumberFromBack ? backwardPageNumber : (currPage-1) ;
    }

    /*
     * returns the page number for a specific comment
     */
    public static int getPageNumberForComment(long blogId, long postId, long commentId) {
        String[] args = {Long.toString(blogId), Long.toString(postId), Long.toString(commentId)};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT page_number FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id=?", args);
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
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), "SELECT count(*) FROM tbl_comments WHERE blog_id=? AND post_id=?", args);
    }

    /**
     *
     * @param orderBy one of ORDER_BY_* constants
     * @see org.wordpress.android.datasets.ReaderCommentTable.CommentsOrderBy
     *
     */
    public static ReaderCommentList getCommentsForPost(ReaderPost post,@CommentsOrderBy int orderBy) {
        if (post == null) {
            return new ReaderCommentList();
        }

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        String query = "SELECT * FROM tbl_comments WHERE blog_id=? AND post_id=? ORDER BY timestamp";

        switch(orderBy){
            case ORDER_BY_NEWEST_COMMENT_FIRST:
                query += " DESC";
                break;
            case ORDER_BY_TIME_OF_COMMENT:
                //ASC is default in ORDER BY clause of SQL
                break;
            default:
                throw new IllegalArgumentException("Unknown orderBy");
        }

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(query, args);
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
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_comments ("
                                                  + COLUMN_NAMES
                                                  + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11,?12,?13,?14,?15,?16)");
        try {
            for (ReaderComment comment: comments) {
                stmt.bindLong  (1,  comment.blogId);
                stmt.bindLong  (2,  comment.postId);
                stmt.bindLong  (3,  comment.commentId);
                stmt.bindLong  (4,  comment.parentId);
                stmt.bindString(5,  comment.getAuthorName());
                stmt.bindString(6,  comment.getAuthorAvatar());
                stmt.bindString(7,  comment.getAuthorUrl());
                stmt.bindLong  (8,  comment.authorId);
                stmt.bindLong  (9,  comment.authorBlogId);
                stmt.bindString(10, comment.getPublished());
                stmt.bindLong  (11, comment.timestamp);
                stmt.bindString(12, comment.getStatus());
                stmt.bindString(13, comment.getText());
                stmt.bindLong  (14, comment.numLikes);
                stmt.bindLong  (15, SqlUtils.boolToSql(comment.isLikedByCurrentUser));
                stmt.bindLong  (16, comment.pageNumber);

                stmt.execute();
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    public static ReaderComment getComment(long blogId, long postId, long commentId) {
        String[] args = new String[] {Long.toString(blogId), Long.toString(postId), Long.toString(commentId)};
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
     * returns true if any of the passed comments don't already exist
     * IMPORTANT: assumes passed comments are all for the same post
     */
    public static boolean hasNewComments(ReaderCommentList comments) {
        if (comments == null || comments.size() == 0) {
            return false;
        }

        StringBuilder sb = new StringBuilder(
                "SELECT COUNT(*) FROM tbl_comments WHERE blog_id=? AND post_id=? AND comment_id IN (");
        boolean isFirst = true;
        for (ReaderComment comment: comments) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(",");
            }
            sb.append(comment.commentId);
        }
        sb.append(")");

        String[] args = {Long.toString(comments.get(0).blogId),
                         Long.toString(comments.get(0).postId)};
        int numExisting = SqlUtils.intForQuery(ReaderDatabase.getReadableDb(), sb.toString(), args);
        return numExisting != comments.size();
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

        comment.commentId = c.getLong(c.getColumnIndex("comment_id"));
        comment.blogId = c.getLong(c.getColumnIndex("blog_id"));
        comment.postId = c.getLong(c.getColumnIndex("post_id"));
        comment.parentId = c.getLong(c.getColumnIndex("parent_id"));

        comment.setPublished(c.getString(c.getColumnIndex("published")));
        comment.timestamp = c.getLong(c.getColumnIndex("timestamp"));

        comment.setAuthorAvatar(c.getString(c.getColumnIndex("author_avatar")));
        comment.setAuthorName(c.getString(c.getColumnIndex("author_name")));
        comment.setAuthorUrl(c.getString(c.getColumnIndex("author_url")));
        comment.authorId = c.getLong(c.getColumnIndex("author_id"));
        comment.authorBlogId = c.getLong(c.getColumnIndex("author_blog_id"));

        comment.setStatus(c.getString(c.getColumnIndex("status")));
        comment.setText(c.getString(c.getColumnIndex("text")));

        comment.numLikes = c.getInt(c.getColumnIndex("num_likes"));
        comment.isLikedByCurrentUser = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_liked")));
        comment.pageNumber = c.getInt(c.getColumnIndex("page_number"));

        return comment;
    }
}
