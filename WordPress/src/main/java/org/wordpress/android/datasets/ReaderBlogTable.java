package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderBlogList;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.Date;
import java.util.List;

/**
 * tbl_blog_info contains information about blogs viewed in the reader, and blogs the
 * user is following. Note that this table is populated from three endpoints:
 * <p>
 * 1. sites/{$siteId}
 * 2. read/following/mine?meta=site,feed
 * 3. read/feed/{$feedId}
 * <p>
 * The first endpoint is called when the user views blog preview, the second is called
 * to get the full list of blogs the user is following, the third is called when user views
 * by feed
 */
public class ReaderBlogTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_blog_info ("
                   + " blog_id INTEGER DEFAULT 0," // will be same as feedId for feeds
                   + " feed_id INTEGER DEFAULT 0," // will be 0 for blogs
                   + " blog_url TEXT NOT NULL COLLATE NOCASE,"
                   + " image_url TEXT,"
                   + " feed_url TEXT,"
                   + " name TEXT,"
                   + " description TEXT,"
                   + " is_private INTEGER DEFAULT 0,"
                   + " is_jetpack INTEGER DEFAULT 0,"
                   + " is_following INTEGER DEFAULT 0,"
                   + " num_followers INTEGER DEFAULT 0,"
                   + " is_notifications_enabled INTEGER DEFAULT 0,"
                   + " date_updated TEXT,"
                   + " organization_id INTEGER DEFAULT 0,"
                   + " unseen_count INTEGER DEFAULT 0,"
                   + " PRIMARY KEY (blog_id)"
                   + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_blog_info");
    }

    public static ReaderBlog getBlogInfo(long blogId) {
        if (blogId == 0) {
            return null;
        }
        String[] args = {Long.toString(blogId)};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE blog_id=?", args);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return getBlogInfoFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static ReaderBlog getFeedInfo(long feedId) {
        if (feedId == 0) {
            return null;
        }
        String[] args = {Long.toString(feedId)};
        Cursor cursor = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_blog_info WHERE feed_id=?", args);
        try {
            if (!cursor.moveToFirst()) {
                return null;
            }
            return getBlogInfoFromCursor(cursor);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }

    public static long getFeedIdFromUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return 0;
        }
        String[] args = {UrlUtils.normalizeUrl(url)};
        return SqlUtils.longForQuery(ReaderDatabase.getReadableDb(),
                                     "SELECT feed_id FROM tbl_blog_info WHERE feed_url=?",
                                     args);
    }

    private static ReaderBlog getBlogInfoFromCursor(Cursor c) {
        if (c == null) {
            return null;
        }

        ReaderBlog blogInfo = new ReaderBlog();
        blogInfo.blogId = c.getLong(c.getColumnIndexOrThrow("blog_id"));
        blogInfo.feedId = c.getLong(c.getColumnIndexOrThrow("feed_id"));
        blogInfo.setUrl(c.getString(c.getColumnIndexOrThrow("blog_url")));
        blogInfo.setImageUrl(c.getString(c.getColumnIndexOrThrow("image_url")));
        blogInfo.setFeedUrl(c.getString(c.getColumnIndexOrThrow("feed_url")));
        blogInfo.setName(c.getString(c.getColumnIndexOrThrow("name")));
        blogInfo.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        blogInfo.isPrivate = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow("is_private")));
        blogInfo.isJetpack = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow("is_jetpack")));
        blogInfo.isFollowing = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow("is_following")));
        blogInfo.isNotificationsEnabled = SqlUtils.sqlToBool(c.getInt(c.getColumnIndexOrThrow(
                "is_notifications_enabled")));
        blogInfo.numSubscribers = c.getInt(c.getColumnIndexOrThrow("num_followers"));
        blogInfo.organizationId = c.getInt(c.getColumnIndexOrThrow("organization_id"));
        blogInfo.numUnseenPosts = c.getInt(c.getColumnIndexOrThrow("unseen_count"));

        return blogInfo;
    }

    public static void addOrUpdateBlog(ReaderBlog blogInfo) {
        if (blogInfo == null) {
            return;
        }
        String sql = "INSERT OR REPLACE INTO tbl_blog_info"
                     + " (blog_id, feed_id, blog_url, image_url, feed_url, name, description, is_private, is_jetpack, "
                     + "  is_following, is_notifications_enabled, num_followers, date_updated, "
                     + "  organization_id, unseen_count)"
                     + " VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindLong(1, blogInfo.blogId);
            stmt.bindLong(2, blogInfo.feedId);
            stmt.bindString(3, blogInfo.getUrl());
            stmt.bindString(4, blogInfo.getImageUrl());
            stmt.bindString(5, blogInfo.getFeedUrl());
            stmt.bindString(6, blogInfo.getName());
            stmt.bindString(7, blogInfo.getDescription());
            stmt.bindLong(8, SqlUtils.boolToSql(blogInfo.isPrivate));
            stmt.bindLong(9, SqlUtils.boolToSql(blogInfo.isJetpack));
            stmt.bindLong(10, SqlUtils.boolToSql(blogInfo.isFollowing));
            stmt.bindLong(11, SqlUtils.boolToSql(blogInfo.isNotificationsEnabled));
            stmt.bindLong(12, blogInfo.numSubscribers);
            stmt.bindString(13, DateTimeUtils.iso8601FromDate(new Date()));
            stmt.bindLong(14, blogInfo.organizationId);
            stmt.bindLong(15, blogInfo.numUnseenPosts);
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
                for (ReaderBlog blog : followedBlogs) {
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
        Cursor c = ReaderDatabase.getReadableDb()
                                 .rawQuery("SELECT DISTINCT blog_url FROM tbl_blog_info WHERE is_following!=0", null);
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

    public static void setIsFollowedFeedId(long feedId, boolean isFollowed) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET is_following="
                + SqlUtils.boolToSql(isFollowed)
                + " WHERE feed_id=?",
                new String[]{Long.toString(feedId)});
    }

    public static boolean hasFollowedBlogs() {
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 LIMIT 1";
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, null);
    }

    public static boolean isFollowedBlogUrl(String blogUrl) {
        if (TextUtils.isEmpty(blogUrl)) {
            return false;
        }
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_url=?";
        String[] args = {UrlUtils.normalizeUrl(blogUrl)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    public static boolean isFollowedBlog(long blogId) {
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND blog_id=?";
        String[] args = {Long.toString(blogId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    public static boolean isFollowedFeedUrl(String feedUrl) {
        if (TextUtils.isEmpty(feedUrl)) {
            return false;
        }
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND feed_url=?";
        String[] args = {UrlUtils.normalizeUrl(feedUrl)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    public static boolean isFollowedFeed(long feedId) {
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_following!=0 AND feed_id=?";
        String[] args = {Long.toString(feedId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    public static boolean isNotificationsEnabled(long blogId) {
        String sql = "SELECT 1 FROM tbl_blog_info WHERE is_notifications_enabled!=0 AND blog_id=?";
        String[] args = {Long.toString(blogId)};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), sql, args);
    }

    public static void setNotificationsEnabledByBlogId(long blogId, boolean isEnabled) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET is_notifications_enabled="
                + SqlUtils.boolToSql(isEnabled)
                + " WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    public static String getBlogName(long blogId) {
        String[] args = {Long.toString(blogId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                       "SELECT name FROM tbl_blog_info WHERE blog_id=?",
                                       args);
    }

    public static String getBlogUrl(long blogId) {
        String[] args = {Long.toString(blogId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                       "SELECT blog_url FROM tbl_blog_info WHERE blog_id=?",
                                       args);
    }

    public static String getFeedName(long feedId) {
        String[] args = {Long.toString(feedId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                       "SELECT name FROM tbl_blog_info WHERE feed_id=?",
                                       args);
    }

    public static void deleteBlogsWithIds(final List<Long> blogIds) {
        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement(
                "DELETE FROM tbl_blog_info"
                + " WHERE blog_id IN ("
                + TextUtils.join(",", blogIds)
                + ")"
        );
        db.beginTransaction();
        try {
            try {
                stmt.execute();
                db.setTransactionSuccessful();
            } catch (SQLException e) {
                AppLog.e(AppLog.T.READER, e);
            }
        } finally {
            SqlUtils.closeStatement(stmt);
            db.endTransaction();
        }
    }

    public static void incrementUnseenCount(long blogId) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET unseen_count = unseen_count+1"
                + " WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    public static void decrementUnseenCount(long blogId) {
        ReaderDatabase.getWritableDb().execSQL(
                "UPDATE tbl_blog_info SET unseen_count = unseen_count-1"
                + " WHERE blog_id=?",
                new String[]{Long.toString(blogId)});
    }

    /*
     * determine whether the passed blog info should be updated based on when it was last updated
     */
    public static boolean isTimeToUpdateBlogInfo(ReaderBlog blogInfo) {
        int minutes = minutesSinceLastUpdate(blogInfo);
        if (minutes == NEVER_UPDATED) {
            return true;
        }
        return (minutes >= ReaderConstants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    private static String getBlogInfoLastUpdated(ReaderBlog blogInfo) {
        if (blogInfo == null) {
            return "";
        }
        if (blogInfo.blogId != 0) {
            String[] args = {Long.toString(blogInfo.blogId)};
            return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                           "SELECT date_updated FROM tbl_blog_info WHERE blog_id=?",
                                           args);
        } else {
            String[] args = {Long.toString(blogInfo.feedId)};
            return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                                           "SELECT date_updated FROM tbl_blog_info WHERE feed_id=?",
                                           args);
        }
    }

    private static final int NEVER_UPDATED = -1;

    private static int minutesSinceLastUpdate(ReaderBlog blogInfo) {
        if (blogInfo == null) {
            return 0;
        }

        String updated = getBlogInfoLastUpdated(blogInfo);
        if (TextUtils.isEmpty(updated)) {
            return NEVER_UPDATED;
        }

        Date dtUpdated = DateTimeUtils.dateFromIso8601(updated);
        if (dtUpdated == null) {
            return 0;
        }

        Date dtNow = new Date();
        return DateTimeUtils.minutesBetween(dtUpdated, dtNow);
    }
}
