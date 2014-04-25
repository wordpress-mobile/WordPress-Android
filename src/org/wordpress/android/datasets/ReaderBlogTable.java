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

/**
 * ideally this would be a single table, but a separate table is necessary for
 * followed blogs since the the read/following/mine endpoint only contains the
 * blog id and url (no name, description, etc). so two tables are required:
 *
 *   (1) blog info for blogs shown in the reader (used by reader blog detail)
 *   (2) urls of blogs we know about and whether they're followed by the current user
 *
 * note that URLs are normalized for comparison
 */
public class ReaderBlogTable {

    protected static void createTables(SQLiteDatabase db) {
        // blog info
        db.execSQL("CREATE TABLE tbl_blog_info ("
                 + "    blog_id       INTEGER DEFAULT 0,"
                 + "	blog_url      TEXT NOT NULL COLLATE NOCASE,"
                 + "    name          TEXT,"
                 + "    description   TEXT,"
                 + "    is_private    INTEGER DEFAULT 0,"
                 + "    is_jetpack    INTEGER DEFAULT 0,"
                 + "    is_following  INTEGER DEFAULT 0,"
                 + "    num_followers INTEGER DEFAULT 0,"
                 + "    PRIMARY KEY (blog_id))");
        db.execSQL("CREATE UNIQUE INDEX idx_blog_info_url ON tbl_blog_info(blog_url)");

        // followed blog urls
        db.execSQL("CREATE TABLE tbl_followed_blogs ("
                + "	blog_url        TEXT NOT NULL COLLATE NOCASE PRIMARY KEY,"
                + " is_following    INTEGER DEFAULT 0)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
        db.execSQL("DROP TABLE IF EXISTS tbl_followed_blogs");
    }

    /*
     * get a blog's info by id
     */
    public static ReaderBlogInfo getBlogInfoById(long blogId) {
        String[] args = {Long.toString(blogId)};
        String sql = "SELECT * FROM tbl_blog_info WHERE blog_id=?";
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (!c.moveToFirst())
                return null;
            return getBlogInfoFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    /*
     * get a blog's info by url
     */
    public static ReaderBlogInfo getBlogInfoByUrl(String blogUrl) {
        if (TextUtils.isEmpty(blogUrl)) {
            return null;
        }
        String[] args = {UrlUtils.normalizeUrl(blogUrl)};
        String sql = "SELECT * FROM tbl_blog_info WHERE blog_id=?";
        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            if (!c.moveToFirst())
                return null;
            return getBlogInfoFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
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

        // update in list of followed urls
        setIsFollowedBlogUrl(blogInfo.getUrl(), blogInfo.isFollowing, false);
    }

    public static void setFollowedBlogUrls(ReaderUrlList urls) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmtUrl = db.compileStatement("INSERT OR REPLACE INTO tbl_followed_blogs (blog_url, is_following) VALUES (?1,?2)");
        SQLiteStatement stmtInfo = db.compileStatement("UPDATE tbl_blog_info SET is_following=? WHERE blog_url=?");
        try {
            // first set all existing blogs to not followed
            db.execSQL("UPDATE tbl_followed_blogs SET is_following=0");
            db.execSQL("UPDATE tbl_blog_info SET is_following=0");

            // then set passed ones as followed
            if (urls != null && urls.size() > 0) {
                long sqlTrue = SqlUtils.boolToSql(true);
                for (String url : urls) {
                    String normUrl = UrlUtils.normalizeUrl(url);

                    // tbl_followed_blogs
                    stmtUrl.bindString(1, normUrl);
                    stmtUrl.bindLong(2, sqlTrue);
                    stmtUrl.execute();
                    stmtUrl.clearBindings();

                    // tbl_blog_info
                    stmtInfo.bindLong(1, sqlTrue);
                    stmtInfo.bindString(2, normUrl);
                    stmtInfo.execute();
                    stmtInfo.clearBindings();
                }
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmtUrl);
            SqlUtils.closeStatement(stmtInfo);
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
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        String[] args = {UrlUtils.normalizeUrl(url)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT is_following FROM tbl_followed_blogs WHERE blog_url=?", args);
    }

    public static void setIsFollowedBlogUrl(String url, boolean isFollowed) {
        setIsFollowedBlogUrl(url, isFollowed, true);
    }
    private static void setIsFollowedBlogUrl(String url, boolean isFollowed, boolean updateBlogInfoTable) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        String normUrl = UrlUtils.normalizeUrl(url);
        long sqlIsFollowed = SqlUtils.boolToSql(isFollowed);

        // update in tbl_followed_blogs
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement("INSERT OR REPLACE INTO tbl_followed_blogs (blog_url, is_following) VALUES (?1,?2)");
        try {
            stmt.bindString(1, normUrl);
            stmt.bindLong(2, sqlIsFollowed);
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }

        // update in tbl_blog_info
        if (updateBlogInfoTable) {
            ContentValues values = new ContentValues();
            values.put("is_following", sqlIsFollowed);
            String[] args = {normUrl};
            ReaderDatabase.getWritableDb().update("tbl_blog_info", values, "blog_url=?", args);
        }
    }
}
