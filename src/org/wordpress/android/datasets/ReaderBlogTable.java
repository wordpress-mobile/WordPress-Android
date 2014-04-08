package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderBlogInfo;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.UrlUtils;

import java.io.Reader;
import java.util.Iterator;

/**
 * (1) blog info for blogs shown in the reader (used by reader blog detail)
 * (2) urls of blogs we know about and whether they're followed by the current user
 */
public class ReaderBlogTable {
    protected static void createTables(SQLiteDatabase db) {
        // blog info
        db.execSQL("CREATE TABLE tbl_blog_info ("
                 + "	blog_url      TEXT COLLATE NOCASE PRIMARY KEY,"
                 + "    blog_id       INTEGER DEFAULT 0,"
                 + "    name          TEXT,"
                 + "    description   TEXT,"
                 + "    is_private    INTEGER DEFAULT 0,"
                 + "    is_jetpack    INTEGER DEFAULT 0,"
                 + "    is_following  INTEGER DEFAULT 0,"
                 + "    num_followers INTEGER DEFAULT 0)");
        db.execSQL("CREATE UNIQUE INDEX idx_blog_info_blog_id ON tbl_blog_info(blog_id)");

        // followed blog urls
        db.execSQL("CREATE TABLE tbl_followed_blogs ("
                + "	blog_url        TEXT COLLATE NOCASE PRIMARY KEY,"
                + " is_following    INTEGER DEFAULT 0)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
        db.execSQL("DROP TABLE IF EXISTS tbl_followed_blogs");
    }

    public static ReaderBlogInfo getBlogInfo(String blogUrl) {
        if (TextUtils.isEmpty(blogUrl)) {
            return null;
        }
        String[] args = {UrlUtils.normalizeUrl(blogUrl)};
        String sql = "SELECT blog_id, blog_url, name, description, is_private, is_jetpack, is_following, num_followers FROM tbl_blog_info WHERE blog_url=?";
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (!c.moveToFirst())
                return null;

            ReaderBlogInfo blog = new ReaderBlogInfo();
            blog.blogId = c.getLong(0);
            blog.setUrl(c.getString(1));
            blog.setName(c.getString(2));
            blog.setDescription(c.getString(3));
            blog.isPrivate = SqlUtils.sqlToBool(c.getInt(4));
            blog.isJetpack = SqlUtils.sqlToBool(c.getInt(5));
            blog.isFollowing = SqlUtils.sqlToBool(c.getInt(6));
            blog.numSubscribers = c.getInt(7);

            return blog;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setBlogInfo(ReaderBlogInfo blog) {
        if (blog == null)
            return;
        String sql = "INSERT OR REPLACE INTO tbl_blog_info"
                + "   (blog_id, blog_url, name, description, is_private, is_jetpack, is_following, num_followers)"
                + "   VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindLong  (1, blog.blogId);
            stmt.bindString(2, blog.getUrl());
            stmt.bindString(3, blog.getName());
            stmt.bindString(4, blog.getDescription());
            stmt.bindLong  (5, SqlUtils.boolToSql(blog.isPrivate));
            stmt.bindLong  (6, SqlUtils.boolToSql(blog.isJetpack));
            stmt.bindLong  (7, SqlUtils.boolToSql(blog.isFollowing));
            stmt.bindLong  (8, blog.numSubscribers);
            stmt.execute();
            stmt.clearBindings();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static void setFollowedBlogUrls(ReaderUrlList urls) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_followed_blogs (blog_url, is_following) VALUES (?1,?2)");
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_followed_blogs SET is_following=0");

            // then insert/replace passed ones as followed
            if (urls!=null && urls.size() > 0) {
                Iterator<String> it = urls.iterator();
                long sqlTrue = SqlUtils.boolToSql(true);
                while (it.hasNext()) {
                    stmt.bindString(1, it.next());
                    stmt.bindLong  (2, sqlTrue);
                    stmt.execute();
                    stmt.clearBindings();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    public static ReaderUrlList getFollowedBlogUrls() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT blog_url FROM tbl_followed_blogs WHERE is_following!=0", null);
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

    public static boolean isFollowedBlogUrl(String url) {
        if (TextUtils.isEmpty(url))
            return false;
        String[] args = {UrlUtils.normalizeUrl(url)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_following FROM tbl_followed_blogs WHERE blog_url=?", args);
    }

    public static void setIsFollowedBlogUrl(String url, boolean isFollowed) {
        if (TextUtils.isEmpty(url))
            return;

        final String normUrl = UrlUtils.normalizeUrl(url);

        // update in tbl_followed_blogs
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement("INSERT OR REPLACE INTO tbl_followed_blogs (blog_url, is_following) VALUES (?1,?2)");
        try {
            stmt.bindString(1, normUrl);
            stmt.bindLong  (2, SqlUtils.boolToSql(isFollowed));
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }

        // update in tbl_blog_info
        ContentValues values = new ContentValues();
        values.put("is_following", SqlUtils.boolToSql(isFollowed));
        String[] args = {UrlUtils.normalizeUrl(url)};
        ReaderDatabase.getWritableDb().update("tbl_blog_info", values, "blog_url=?", args);

    }
}
