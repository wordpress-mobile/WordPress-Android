package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

/**
 * tbl_blog_info contains information about blogs viewed in the reader, and blogs the
 * user is following. Note that this table is populated from two endpoints:
 *
 *      1. sites/{$siteId}
 *      2. read/following/mine?meta=site,feed
 *
 *  The first endpoint is called when the user views blog preview, the second endpoint is called
 *  at startup to get the full list of blogs the user is following
 */
public class ReaderBlogTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_info ("
                 + "    blog_id       INTEGER DEFAULT 0,"
                 + "    feed_id       INTEGER DEFAULT 0,"
                 + "	blog_url      TEXT NOT NULL COLLATE NOCASE,"
                 + "    image_url     TEXT,"
                 + "    name          TEXT,"
                 + "    description   TEXT,"
                 + "    is_private    INTEGER DEFAULT 0,"
                 + "    is_jetpack    INTEGER DEFAULT 0,"
                 + "    is_following  INTEGER DEFAULT 0,"
                 + "    num_followers INTEGER DEFAULT 0,"
                 + "    PRIMARY KEY (blog_id, feed_id, blog_url)"
                 + ")");

        db.execSQL("CREATE TABLE tbl_recommended_blogs ("
                + "     blog_id         INTEGER DEFAULT 0,"
                + "     follow_reco_id  INTEGER DEFAULT 0,"
                + "     score           INTEGER DEFAULT 0,"
                + "	    title           TEXT COLLATE NOCASE,"
                + "	    blog_url        TEXT COLLATE NOCASE,"
                + "	    image_url       TEXT,"
                + "	    reason          TEXT,"
                + "     PRIMARY KEY (blog_id)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_blogs");
    }

    /*
     * get a blog's info by either id or url
     */
    public static ReaderBlog getBlogInfo(long blogId, String blogUrl) {
        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);

        if (!hasBlogId && !hasBlogUrl) {
            return null;
        }

        // search by id if it's passed (may be zero for feeds), otherwise search by url
        final Cursor cursor;
        SQLiteDatabase db = ReaderDatabase.getReadableDb();
        if (hasBlogId) {
            String[] args = {Long.toString(blogId)};
            cursor = db.rawQuery("SELECT * FROM tbl_blog_info WHERE blog_id=?", args);
        } else {
            String[] args = {blogUrl};
            cursor = db.rawQuery("SELECT * FROM tbl_blog_info WHERE blog_url=?", args);
        }

        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return getBlogInfoFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static String getBlogUrl(long blogId) {
        String[] args = {Long.toString(blogId)};
        return SqlUtils.stringForQuery(
                ReaderDatabase.getReadableDb(), "SELECT blog_url FROM tbl_blog_info WHERE blog_id=?", args);
    }

    private static ReaderBlog getBlogInfoFromCursor(Cursor c) {
        if (c == null) {
            return null;
        }

        ReaderBlog blogInfo = new ReaderBlog();
        blogInfo.blogId = c.getLong(c.getColumnIndex("blog_id"));
        blogInfo.feedId = c.getLong(c.getColumnIndex("feed_id"));
        blogInfo.setUrl(c.getString(c.getColumnIndex("blog_url")));
        blogInfo.setImageUrl(c.getString(c.getColumnIndex("image_url")));
        blogInfo.setName(c.getString(c.getColumnIndex("name")));
        blogInfo.setDescription(c.getString(c.getColumnIndex("description")));
        blogInfo.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        blogInfo.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));
        blogInfo.isFollowing = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_following")));
        blogInfo.numSubscribers = c.getInt(c.getColumnIndex("num_followers"));

        return blogInfo;
    }

    public static void addOrUpdateBlog(ReaderBlog blogInfo) {
        if (blogInfo == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO tbl_blog_info"
                + "   (blog_id, feed_id, blog_url, image_url, name, description, is_private, is_jetpack, is_following, num_followers)"
                + "   VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindLong  (1, blogInfo.blogId);
            stmt.bindLong  (2, blogInfo.feedId);
            stmt.bindString(3, blogInfo.getUrl());
            stmt.bindString(4, blogInfo.getImageUrl());
            stmt.bindString(5, blogInfo.getName());
            stmt.bindString(6, blogInfo.getDescription());
            stmt.bindLong  (7, SqlUtils.boolToSql(blogInfo.isPrivate));
            stmt.bindLong  (8, SqlUtils.boolToSql(blogInfo.isJetpack));
            stmt.bindLong  (9, SqlUtils.boolToSql(blogInfo.isFollowing));
            stmt.bindLong  (10, blogInfo.numSubscribers);
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns blogInfo for all followed blogs
     */
    public static ReaderBlogList getFollowedBlogs() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(
                "SELECT * FROM tbl_blog_info WHERE is_following!=0 ORDER BY name COLLATE NOCASE, blog_url",
                null);
        try {
            ReaderBlogList blogs = new ReaderBlogList();
            if (c.moveToFirst()) {
                do {
                    ReaderBlog blogInfo = getBlogInfoFromCursor(c);
                    blogs.add(blogInfo);
                } while (c.moveToNext());
            }
            return blogs;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /*
     * set followed blogs from the read/following/mine endpoint
     */
    public static void setFollowedBlogs(ReaderBlogList followedBlogs) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_blog_info SET is_following=0");

            // then insert passed ones
            if (followedBlogs != null) {
                for (ReaderBlog blog: followedBlogs) {
                    addOrUpdateBlog(blog);
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

    /*
     * return list of URLs of followed blogs
     */
    public static ReaderUrlList getFollowedBlogUrls() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT DISTINCT blog_url FROM tbl_blog_info WHERE is_following!=0", null);
        try {
            ReaderUrlList urls = new ReaderUrlList();
            if (c.moveToFirst()) {
                do {
                    urls.add(c.getString(0));
                } while (c.moveToNext());
            }
            return urls;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /*
     * sets the follow state for passed blog without creating a record for it if it doesn't exist
     */
    public static void setIsFollowedBlogId(long blogId, boolean isFollowed) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET is_following="
                        + SqlUtils.boolToSql(isFollowed)
                        + " WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    /*
     * sets the followed state for the passed blog, creating a record for it if it doesn't exist
     */
    public static void setIsFollowedBlog(long blogId, String url, boolean isFollowed) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        // get existing info for this blog
        ReaderBlog blogInfo = getBlogInfo(blogId, url);

        if (blogInfo == null) {
            // blogInfo doesn't exist, create it with just the passed id & url
            blogInfo = new ReaderBlog();
            blogInfo.blogId = blogId;
            blogInfo.setUrl(url);
        } else if (blogInfo.isFollowing == isFollowed) {
            // blogInfo already has passed following status, so nothing more to do
            return;
        }

        blogInfo.isFollowing = isFollowed;
        addOrUpdateBlog(blogInfo);
    }

    public static boolean isFollowedBlogUrl(String blogUrl) {
        return isFollowedBlog(0, blogUrl);
    }

    public static boolean isFollowedBlog(long blogId, String blogUrl) {
        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);

        if (!hasBlogId && !hasBlogUrl) {
            return false;
        }

        String sql;
        if (hasBlogId && hasBlogUrl) {
            // both id and url were passed, match on either
            sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND (blog_id=? OR blog_url=?)";
            String[] args = {Long.toString(blogId), blogUrl};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        } else if (hasBlogId) {
            // only id passed, match on id
            sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_id=?";
            String[] args = {Long.toString(blogId)};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        } else {
            // only url passed, match on url
            sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_url=?";
            String[] args = {blogUrl};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        }
    }

    public static ReaderRecommendBlogList getAllRecommendedBlogs() {
        return getRecommendedBlogs(0, 0);
    }
    public static ReaderRecommendBlogList getRecommendedBlogs(int limit, int offset) {
        String sql = " SELECT * FROM tbl_recommended_blogs ORDER BY title";

        if (limit > 0) {
            sql += " LIMIT " + Integer.toString(limit);
            if (offset > 0) {
                sql += " OFFSET " + Integer.toString(offset);
            }
        }

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, null);
        try {
            ReaderRecommendBlogList blogs = new ReaderRecommendBlogList();
            if (c.moveToFirst()) {
                do {
                    ReaderRecommendedBlog blog = new ReaderRecommendedBlog();
                    blog.blogId = c.getLong(c.getColumnIndex("blog_id"));
                    blog.followRecoId = c.getLong(c.getColumnIndex("follow_reco_id"));
                    blog.score = c.getInt(c.getColumnIndex("score"));
                    blog.setTitle(c.getString(c.getColumnIndex("title")));
                    blog.setBlogUrl(c.getString(c.getColumnIndex("blog_url")));
                    blog.setImageUrl(c.getString(c.getColumnIndex("image_url")));
                    blog.setReason(c.getString(c.getColumnIndex("reason")));
                    blogs.add(blog);
                } while (c.moveToNext());
            }
            return blogs;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setRecommendedBlogs(ReaderRecommendBlogList blogs) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO tbl_recommended_blogs"
                        + " (blog_id, follow_reco_id, score, title, blog_url, image_url, reason)"
                        + " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)");
        db.beginTransaction();
        try {
            try {
                // first delete all recommended blogs
                SqlUtils.deleteAllRowsInTable(db, "tbl_recommended_blogs");

                // then insert the passed ones
                if (blogs != null && blogs.size() > 0) {
                    for (ReaderRecommendedBlog blog : blogs) {
                        stmt.bindLong(1, blog.blogId);
                        stmt.bindLong(2, blog.followRecoId);
                        stmt.bindLong(3, blog.score);
                        stmt.bindString(4, blog.getTitle());
                        stmt.bindString(5, blog.getBlogUrl());
                        stmt.bindString(6, blog.getImageUrl());
                        stmt.bindString(7, blog.getReason());
                        stmt.execute();
                    }
                }
                db.setTransactionSuccessful();

            } catch (SQLException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        } finally {
            SqlUtils.closeStatement(stmt);
            db.endTransaction();
        }
    }
}
