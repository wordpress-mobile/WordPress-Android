package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderBlogInfoList;
import org.wordpress.android.models.ReaderFollowedBlog;
import org.wordpress.android.models.ReaderFollowedBlogList;
import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.UrlUtils;

/**
 * contains information about blogs viewed in the reader, and blogs the user is following.
 * Note that this table is populated from two endpoints:
 *
 *      1. sites/{$siteId}
 *      2. read/following/mine
 *
 *  The first endpoint is called when the user views blog detail, and it returns all the fields
 *  stored in this table. The second endpoint is called at startup to get the full list of
 *  blogs the user is following, and it only returns blog_id and blog_url.
 *
 *  This means that many of the fields in this table will be empty. Note also that blog_id
 *  may be zero if the blog is actually a feed.
 */
public class ReaderBlogTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_info ("
                 + "    blog_id       INTEGER DEFAULT 0,"
                 + "	blog_url      TEXT NOT NULL COLLATE NOCASE,"
                 + "    name          TEXT,"
                 + "    description   TEXT,"
                 + "    is_private    INTEGER DEFAULT 0,"
                 + "    is_jetpack    INTEGER DEFAULT 0,"
                 + "    is_following  INTEGER DEFAULT 0,"
                 + "    num_followers INTEGER DEFAULT 0,"
                 + "    PRIMARY KEY (blog_id, blog_url))");

        db.execSQL("CREATE TABLE tbl_recommended_blogs ("
                + " blog_id         INTEGER DEFAULT 0 PRIMARY KEY,"
                + " follow_reco_id  INTEGER DEFAULT 0,"
                + " score           INTEGER DEFAULT 0,"
                + "	title           TEXT,"
                + "	blog_url        TEXT COLLATE NOCASE,"
                + "	image_url       TEXT,"
                + "	reason          TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_blogs");
    }

    /*
     * get a blog's info by either id or url
     */
    public static ReaderBlogInfo getBlogInfo(long blogId, String blogUrl) {
        // search by id if it's passed (may be zero for feeds), otherwise search by url
        final Cursor cursor;
        if (blogId != 0) {
            String[] args = {Long.toString(blogId)};
            cursor = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE blog_id=?", args);
        } else if (!TextUtils.isEmpty(blogUrl)) {
            String[] args = {UrlUtils.normalizeUrl(blogUrl)};
            cursor = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE blog_url=?", args);
        } else {
            return null;
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

    private static ReaderBlogInfo getBlogInfoFromCursor(Cursor c) {
        if (c == null) {
            return null;
        }

        ReaderBlogInfo blogInfo = new ReaderBlogInfo();
        blogInfo.blogId = c.getLong(c.getColumnIndex("blog_id"));
        blogInfo.setUrl(UrlUtils.normalizeUrl(c.getString(c.getColumnIndex("blog_url"))));
        blogInfo.setName(c.getString(c.getColumnIndex("name")));
        blogInfo.setDescription(c.getString(c.getColumnIndex("description")));
        blogInfo.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        blogInfo.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));
        blogInfo.isFollowing = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_following")));
        blogInfo.numSubscribers = c.getInt(c.getColumnIndex("num_followers"));

        return blogInfo;
    }

    public static void setBlogInfo(ReaderBlogInfo blogInfo) {
        if (blogInfo == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO tbl_blog_info"
                + "   (blog_id, blog_url, name, description, is_private, is_jetpack, is_following, num_followers)"
                + "   VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindLong  (1, blogInfo.blogId);
            stmt.bindString(2, UrlUtils.normalizeUrl(blogInfo.getUrl()));
            stmt.bindString(3, blogInfo.getName());
            stmt.bindString(4, blogInfo.getDescription());
            stmt.bindLong(5, SqlUtils.boolToSql(blogInfo.isPrivate));
            stmt.bindLong  (6, SqlUtils.boolToSql(blogInfo.isJetpack));
            stmt.bindLong  (7, SqlUtils.boolToSql(blogInfo.isFollowing));
            stmt.bindLong  (8, blogInfo.numSubscribers);
            stmt.execute();
            stmt.clearBindings();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns blogInfo for all followed blogs - note that the blogInfo may be incomplete (may
     * just be id & url)
     */
    public static ReaderBlogInfoList getAllFollowedBlogInfo() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE is_following!=0 ORDER BY name, blog_url", null);
        try {
            ReaderBlogInfoList blogs = new ReaderBlogInfoList();
            if (c.moveToFirst()) {
                do {
                    ReaderBlogInfo blogInfo = getBlogInfoFromCursor(c);
                    blogs.add(blogInfo);
                } while (c.moveToNext());
            }
            return blogs;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /*
     * returns list of all followed blogs (id/url only)
     */
    public static ReaderFollowedBlogList getFollowedBlogs() {
        ReaderFollowedBlogList followedBlogs = new ReaderFollowedBlogList();
        ReaderBlogInfoList blogInfoList = getAllFollowedBlogInfo();
        for (ReaderBlogInfo blogInfo: blogInfoList) {
            ReaderFollowedBlog blog = new ReaderFollowedBlog();
            blog.blogId = blogInfo.blogId;
            blog.setUrl(blogInfo.getUrl());
            followedBlogs.add(blog);
        }
        return followedBlogs;
    }

    /*
     * set followed blogs from the read/following/mine endpoint
     */
    public static void setFollowedBlogs(ReaderFollowedBlogList followedBlogs) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_blog_info SET is_following=0");

            // then set passed ones as followed
            if (followedBlogs != null) {
                for (ReaderFollowedBlog blog: followedBlogs) {
                    setIsFollowedBlogUrl(blog.blogId, blog.getUrl(), true);
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

    public static void setIsFollowedBlogUrl(long blogId, String url, boolean isFollowed) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        // get existing info for this blog
        ReaderBlogInfo blogInfo = getBlogInfo(blogId, url);

        if (blogInfo == null) {
            // blogInfo doesn't exist, create it with just the passed id & url
            blogInfo = new ReaderBlogInfo();
            blogInfo.blogId = blogId;
            blogInfo.setUrl(UrlUtils.normalizeUrl(url));
        } else if (blogInfo.isFollowing == isFollowed) {
            // blogInfo already has passed following status, so nothing more to do
            return;
        }

        blogInfo.isFollowing = isFollowed;
        setBlogInfo(blogInfo);
    }

    public static boolean isFollowedBlog(long blogId, String blogUrl) {
        boolean hasBlogId = (blogId != 0);
        boolean hasBlogUrl = !TextUtils.isEmpty(blogUrl);

        if (hasBlogId && hasBlogUrl) {
            // both id and url were passed, match on either
            String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND (blog_id=? OR blog_url=?)";
            String[] args = {Long.toString(blogId), UrlUtils.normalizeUrl(blogUrl)};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        } else if (hasBlogId) {
            // only id passed, match on id
            String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_id=?";
            String[] args = {Long.toString(blogId)};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        } else if (hasBlogUrl) {
            // only url passed, match on url
            String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_url=?";
            String[] args = {UrlUtils.normalizeUrl(blogUrl)};
            return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
        } else {
            // neither id nor url passed
            return false;
        }
    }

    public static ReaderRecommendBlogList getRecommendedBlogs() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_blogs ORDER BY title", null);
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
                db.execSQL("DELETE FROM tbl_recommended_blogs");

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
                        stmt.clearBindings();
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
