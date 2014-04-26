package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderRecommendBlogList;
import org.wordpress.android.models.ReaderRecommendedBlog;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.Iterator;

/**
 * urls of blogs we know about and whether they're followed by the current user
 */
public class ReaderBlogTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_urls ("
                + "	blog_url        TEXT COLLATE NOCASE PRIMARY KEY,"
                + " is_followed     INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE tbl_recommended_blogs ("
                + " blog_id         INTEGER DEFAULT 0 PRIMARY KEY,"
                + " follow_reco_id  INTEGER DEFAULT 0,"
                + " score           INTEGER DEFAULT 0,"
                + "	title           TEXT,"
                + "	blog_domain     TEXT COLLATE NOCASE,"
                + "	image_url       TEXT,"
                + "	reason          TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_urls");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_blogs");
    }

    public static ReaderRecommendBlogList getRecommendedBlogs() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_blogs", null);
        try {
            ReaderRecommendBlogList blogs = new ReaderRecommendBlogList();
            if (c.moveToFirst()) {
                do {
                    ReaderRecommendedBlog blog = new ReaderRecommendedBlog();
                    blog.blogId = c.getLong(c.getColumnIndex("blog_id"));
                    blog.followRecoId = c.getLong(c.getColumnIndex("follow_reco_id"));
                    blog.score = c.getInt(c.getColumnIndex("score"));
                    blog.setTitle(c.getString(c.getColumnIndex("title")));
                    blog.setBlogDomain(c.getString(c.getColumnIndex("blog_domain")));
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
                                + " (blog_id, follow_reco_id, score, title, blog_domain, image_url, reason)"
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
                        stmt.bindString(5, blog.getBlogDomain());
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

    public static void setFollowedBlogUrls(ReaderUrlList urls) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_blog_urls (blog_url, is_followed) VALUES (?1,?2)");
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_blog_urls SET is_followed=0");

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
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT blog_url FROM tbl_blog_urls WHERE is_followed!=0", null);
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
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_followed FROM tbl_blog_urls WHERE blog_url=?", args);
    }

    public static void setIsFollowedBlogUrl(String url, boolean isFollowed) {
        if (TextUtils.isEmpty(url))
            return;
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement("INSERT OR REPLACE INTO tbl_blog_urls (blog_url, is_followed) VALUES (?1,?2)");
        try {
            stmt.bindString(1, UrlUtils.normalizeUrl(url));
            stmt.bindLong  (2, SqlUtils.boolToSql(isFollowed));
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }
}
