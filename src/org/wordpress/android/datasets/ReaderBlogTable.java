package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderFollowedBlog;
import org.wordpress.android.models.ReaderFollowedBlogList;
import org.wordpress.android.models.ReaderUrlList;
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
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
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

        ReaderBlogInfo blog = new ReaderBlogInfo();
        blog.blogId = c.getLong(c.getColumnIndex("blog_id"));
        blog.setUrl(UrlUtils.normalizeUrl(c.getString(c.getColumnIndex("blog_url"))));
        blog.setName(c.getString(c.getColumnIndex("name")));
        blog.setDescription(c.getString(c.getColumnIndex("description")));
        blog.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_private")));
        blog.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_jetpack")));
        blog.isFollowing = SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("is_following")));
        blog.numSubscribers = c.getInt(c.getColumnIndex("num_followers"));

        return blog;
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
            stmt.bindLong  (5, SqlUtils.boolToSql(blogInfo.isPrivate));
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
}
