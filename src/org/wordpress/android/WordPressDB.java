package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.TrustedSslDomainTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaFile;
import org.wordpress.android.models.Note;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.Utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class WordPressDB {

    private static final int DATABASE_VERSION = 25;

    private static final String CREATE_TABLE_SETTINGS = "create table if not exists accounts (id integer primary key autoincrement, "
            + "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean);";
    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";
    public static final String SETTINGS_TABLE = "accounts";
    private static final String DATABASE_NAME = "wordpress";
    private static final String MEDIA_TABLE = "media";

    private static final String CREATE_TABLE_POSTS = "create table if not exists posts (id integer primary key autoincrement, blogID text, "
            + "postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', "
            + "description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, "
            + "mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, "
            + "wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', "
            + "latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text);";

    private static final String POSTS_TABLE = "posts";

    private static final String THEMES_TABLE = "themes";
    private static final String CREATE_TABLE_THEMES = "create table if not exists themes (_id integer primary key autoincrement, "
            + "themeId text, name text, description text, screenshotURL text, trendingRank integer default 0, popularityRank integer default 0, launchDate date, previewURL text, blogId text, isCurrent boolean default false, isPremium boolean default false, features text);";

    // categories
    private static final String CREATE_TABLE_CATEGORIES = "create table if not exists cats (id integer primary key autoincrement, "
            + "blog_id text, wp_id integer, category_name text not null);";
    private static final String CATEGORIES_TABLE = "cats";

    // for capturing blogID
    private static final String ADD_BLOGID = "alter table accounts add blogId integer;";
    private static final String UPDATE_BLOGID = "update accounts set blogId = 1;";

    // for capturing blogID, trac ticket #
    private static final String ADD_LOCATION_FLAG = "alter table accounts add location boolean default false;";

    // add wordpress.com stats login info
    private static final String ADD_DOTCOM_USERNAME = "alter table accounts add dotcom_username text;";
    private static final String ADD_DOTCOM_PASSWORD = "alter table accounts add dotcom_password text;";
    private static final String ADD_API_KEY = "alter table accounts add api_key text;";
    private static final String ADD_API_BLOGID = "alter table accounts add api_blogid text;";

    // add wordpress.com flag and version column
    private static final String ADD_DOTCOM_FLAG = "alter table accounts add dotcomFlag boolean default false;";
    private static final String ADD_WP_VERSION = "alter table accounts add wpVersion text;";

    // add httpuser and httppassword
    private static final String ADD_HTTPUSER = "alter table accounts add httpuser text;";
    private static final String ADD_HTTPPASSWORD = "alter table accounts add httppassword text;";

    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

    // add field to store last used blog
    private static final String ADD_POST_FORMATS = "alter table accounts add postFormats text default '';";

    //add scaled image settings
    private static final String ADD_SCALED_IMAGE = "alter table accounts add isScaledImage boolean default false;";
    private static final String ADD_SCALED_IMAGE_IMG_WIDTH = "alter table accounts add scaledImgWidth integer default 1024;";

    //add boolean to posts to check uploaded posts that have local changes
    private static final String ADD_LOCAL_POST_CHANGES = "alter table posts add isLocalChange boolean default 0";

    //add boolean to track if featured image should be included in the post content
    private static final String ADD_FEATURED_IN_POST = "alter table media add isFeaturedInPost boolean default false;";

    // add home url to blog settings
    private static final String ADD_HOME_URL = "alter table accounts add homeURL text default '';";

    private static final String ADD_BLOG_OPTIONS = "alter table accounts add blog_options text default '';";

    // add category parent id to keep track of category hierarchy
    private static final String ADD_PARENTID_IN_CATEGORIES = "alter table cats add parent_id integer default 0;";

    // add admin flag to blog settings
    private static final String ADD_ACCOUNTS_ADMIN_FLAG = "alter table accounts add isAdmin boolean default false;";

    // add thumbnailURL, thumbnailPath and fileURL to media
    private static final String ADD_MEDIA_THUMBNAIL_URL = "alter table media add thumbnailURL text default '';";
    private static final String ADD_MEDIA_FILE_URL = "alter table media add fileURL text default '';";
    private static final String ADD_MEDIA_UNIQUE_ID = "alter table media add mediaId text default '';";
    private static final String ADD_MEDIA_BLOG_ID = "alter table media add blogId text default '';";
    private static final String ADD_MEDIA_DATE_GMT = "alter table media add date_created_gmt date;";
    private static final String ADD_MEDIA_UPLOAD_STATE = "alter table media add uploadState default '';";
    private static final String ADD_MEDIA_VIDEOPRESS_SHORTCODE = "alter table media add videoPressShortcode text default '';";

    // create table to store notifications
    private static final String NOTES_TABLE = "notes";
    private static final String CREATE_TABLE_NOTES = "create table if not exists notes (id integer primary key, " +
            "note_id text, message text, type text, raw_note_data text, timestamp integer, placeholder boolean);";

    // add hidden flag to blog settings (accounts)
    private static final String ADD_ACCOUNTS_HIDDEN_FLAG = "alter table accounts add isHidden boolean default 0;";

    private SQLiteDatabase db;

    protected static final String PASSWORD_SECRET = Config.DB_SECRET;

    private Context context;

    public WordPressDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_SETTINGS);
        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        db.execSQL(CREATE_TABLE_MEDIA);
        db.execSQL(CREATE_TABLE_THEMES);
        db.execSQL(CREATE_TABLE_NOTES);
        CommentTable.createTables(db);
        TrustedSslDomainTable.createTable(db);

        // Update tables for new installs and app updates
        int currentVersion = db.getVersion();
        switch (currentVersion) {
            case 0:
                // New install
                currentVersion++;
            case 1:
                // Add columns that were added in very early releases, then move on to version 9
                db.execSQL(ADD_BLOGID);
                db.execSQL(UPDATE_BLOGID);
                db.execSQL(ADD_LOCATION_FLAG);
                db.execSQL(ADD_DOTCOM_USERNAME);
                db.execSQL(ADD_DOTCOM_PASSWORD);
                db.execSQL(ADD_API_KEY);
                db.execSQL(ADD_API_BLOGID);
                db.execSQL(ADD_DOTCOM_FLAG);
                db.execSQL(ADD_WP_VERSION);
                currentVersion = 9;
            case 9:
                db.execSQL(ADD_HTTPUSER);
                db.execSQL(ADD_HTTPPASSWORD);
                migratePasswords();
                currentVersion++;
            case 10:
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                migrateDrafts();
                db.execSQL(ADD_POST_FORMATS);
                currentVersion++;
            case 11:
                db.execSQL(ADD_SCALED_IMAGE);
                db.execSQL(ADD_SCALED_IMAGE_IMG_WIDTH);
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                currentVersion++;
            case 12:
                db.execSQL(ADD_FEATURED_IN_POST);
                currentVersion++;
            case 13:
                db.execSQL(ADD_HOME_URL);
                currentVersion++;
            case 14:
                db.execSQL(ADD_BLOG_OPTIONS);
                currentVersion++;
            case 15:
                // No longer used (preferences migration)
                currentVersion++;
            case 16:
                migrateWPComAccount();
                currentVersion++;
            case 17:
                db.execSQL(ADD_PARENTID_IN_CATEGORIES);
                currentVersion++;
            case 18:
                db.execSQL(ADD_ACCOUNTS_ADMIN_FLAG);
                db.execSQL(ADD_MEDIA_FILE_URL);
                db.execSQL(ADD_MEDIA_THUMBNAIL_URL);
                db.execSQL(ADD_MEDIA_UNIQUE_ID);
                db.execSQL(ADD_MEDIA_BLOG_ID);
                db.execSQL(ADD_MEDIA_DATE_GMT);
                db.execSQL(ADD_MEDIA_UPLOAD_STATE);
                currentVersion++;
            case 19:
                // revision 20: create table "notes"
                currentVersion++;
            case 20:
                db.execSQL(ADD_ACCOUNTS_HIDDEN_FLAG);
                currentVersion++;
            case 21:
                db.execSQL(ADD_MEDIA_VIDEOPRESS_SHORTCODE);
                currentVersion++;
                // version 23 added CommentTable.java, version 24 changed the comment table schema
            case 22:
                currentVersion++;
            case 23:
                CommentTable.reset(db);
                currentVersion++;
            case 24:
                // create table TrustedSslDomainTable
                currentVersion++;
        }
        db.setVersion(DATABASE_VERSION);
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }

    public void deleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    private void migrateWPComAccount() {
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "username", "password" }, "dotcomFlag=1", null, null,
                null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            String username = c.getString(0);
            String password = c.getString(1);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(WordPress.WPCOM_USERNAME_PREFERENCE, username);
            editor.putString(WordPress.WPCOM_PASSWORD_PREFERENCE, password);
            editor.commit();
        }

        c.close();
    }

    private void migrateDrafts() {
        try {
            // Migrate drafts to unified posts table
            Cursor c = db.query("localdrafts",
                    new String[]{"blogID", "title", "content", "picturePaths", "date", "categories", "tags", "status",
                                 "password", "latitude", "longitude"}, null, null, null, null, "id desc");
            int numRows = c.getCount();
            c.moveToFirst();

            for (int i = 0; i < numRows; ++i) {
                if (c.getString(0) != null) {
                    Post post = new Post(c.getInt(0), c.getString(1), c.getString(2), "", c.getString(3), c.getLong(4),
                            c.getString(5), c.getString(6), c.getString(7), c.getString(8), c.getDouble(9), c.getDouble(
                            10), false, "", false);
                    post.setLocalDraft(true);
                    post.setPost_status("localdraft");
                    savePost(post, c.getInt(0));
                }
                c.moveToNext();
            }
            c.close();

            db.delete("localdrafts", null, null);

            // pages
            c = db.query("localpagedrafts",
                    new String[]{"blogID", "title", "content", "picturePaths", "date", "status", "password"}, null,
                    null, null, null, "id desc");
            numRows = c.getCount();
            c.moveToFirst();

            for (int i = 0; i < numRows; ++i) {
                if (c.getString(0) != null) {
                    Post post = new Post(c.getInt(0), c.getString(1), c.getString(2), "", c.getString(3), c.getLong(4),
                            c.getString(5), "", "", c.getString(6), 0, 0, true, "", false);
                    post.setLocalDraft(true);
                    post.setPost_status("localdraft");
                    post.setIsPage(true);
                    savePost(post, c.getInt(0));
                }
                c.moveToNext();
            }
            c.close();
            db.delete("localpagedrafts", null, null);
        } catch (Exception e) {
            // Ignore exception (localdrafts doesn't exist)
        }
    }

    public boolean addBlog(Blog blog) {
        ContentValues values = new ContentValues();
        values.put("url", blog.getUrl());
        values.put("homeURL", blog.getHomeURL());
        values.put("blogName", blog.getBlogName());
        values.put("username", blog.getUsername());
        values.put("password", encryptPassword(blog.getPassword()));
        values.put("httpuser", blog.getHttpuser());
        values.put("httppassword", encryptPassword(blog.getHttppassword()));
        values.put("imagePlacement", blog.getImagePlacement());
        values.put("centerThumbnail", false);
        values.put("fullSizeImage", false);
        values.put("maxImageWidth", blog.getMaxImageWidth());
        values.put("maxImageWidthId", blog.getMaxImageWidthId());
        values.put("runService", false);
        values.put("blogId", blog.getRemoteBlogId());
        values.put("dotcomFlag", blog.isDotcomFlag());
        values.put("wpVersion", blog.getWpVersion());
        values.put("isAdmin", blog.isAdmin());
        return db.insert(SETTINGS_TABLE, null, values) > -1;
    }

    public List<Integer> getAllAccountIDs() {
        Cursor c = db.rawQuery("SELECT DISTINCT id FROM " + SETTINGS_TABLE, null);
        try {
            List<Integer> ids = new ArrayList<Integer>();
            if (c.moveToFirst()) {
                do {
                    ids.add(c.getInt(0));
                } while (c.moveToNext());
            }
            return ids;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public List<Map<String, Object>> getAccountsBy(String byString, String[] extraFields) {
        if (db == null) {
            return new Vector<Map<String, Object>>();
        }
        String[] baseFields = new String[]{"id", "blogName", "username", "blogId", "url",
                "password"};
        String[] allFields = baseFields;
        if (extraFields != null) {
            allFields = (String[]) ArrayUtils.addAll(baseFields, extraFields);
        }
        Cursor c = db.query(SETTINGS_TABLE, allFields, byString, null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {
            int id = c.getInt(0);
            String blogName = c.getString(1);
            String username = c.getString(2);
            int blogId = c.getInt(3);
            String url = c.getString(4);
            String password = c.getString(5);
            if (password != null && !password.equals("") && id > 0) {
                Map<String, Object> thisHash = new HashMap<String, Object>();
                thisHash.put("id", id);
                thisHash.put("blogName", blogName);
                thisHash.put("username", username);
                thisHash.put("blogId", blogId);
                thisHash.put("url", url);
                if (extraFields != null) {
                    for (int j = 0; j < extraFields.length; ++j) {
                        thisHash.put(extraFields[j], c.getString(6 + j));
                    }
                }
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();
        Collections.sort(accounts, Utils.BlogNameComparator);
        return accounts;
    }

    public List<Map<String, Object>> getVisibleAccounts() {
        return getAccountsBy("isHidden = 0", null);
    }

    public List<Map<String, Object>> getVisibleDotComAccounts() {
        return getAccountsBy("isHidden = 0 AND dotcomFlag = 1", null);
    }

    public int getNumVisibleAccounts() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + SETTINGS_TABLE + " WHERE isHidden = 0", null);
    }

    public int getNumDotComAccounts() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + SETTINGS_TABLE + " WHERE dotcomFlag = 1", null);
    }

    public List<Map<String, Object>> getAllAccounts() {
        return getAccountsBy(null, null);
    }

    public int setAllDotComAccountsVisibility(boolean visible) {
        ContentValues values = new ContentValues();
        values.put("isHidden", !visible);
        return db.update(SETTINGS_TABLE, values, "dotcomFlag = 1", null);
    }

    public int setDotComAccountsVisibility(int id, boolean visible) {
        ContentValues values = new ContentValues();
        values.put("isHidden", !visible);
        return db.update(SETTINGS_TABLE, values, "dotcomFlag=1 AND id=" + id, null);
    }

    public boolean isDotComAccountVisible(int blogId) {
        String[] args = {Integer.toString(blogId)};
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + SETTINGS_TABLE +
                                         " WHERE isHidden = 0 AND blogId=?", args);
    }

    public boolean isBlogInDatabase(int blogId, String xmlRpcUrl) {
        Cursor c = db.query(SETTINGS_TABLE, new String[]{"id"}, "blogId=? AND url=?",
                new String[]{Integer.toString(blogId), xmlRpcUrl}, null, null, null, null);
        boolean result =  c.getCount() > 0;
        c.close();
        return result;
    }

    public boolean isLocalBlogIdInDatabase(int localBlogId) {
        String[] args = {Integer.toString(localBlogId)};
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + SETTINGS_TABLE + " WHERE id=?", args);
    }

    public boolean saveBlog(Blog blog) {
        if (blog.getLocalTableBlogId() == -1) {
            return addBlog(blog);
        }

        ContentValues values = new ContentValues();
        values.put("url", blog.getUrl());
        values.put("homeURL", blog.getHomeURL());
        values.put("username", blog.getUsername());
        values.put("password", encryptPassword(blog.getPassword()));
        values.put("httpuser", blog.getHttpuser());
        values.put("httppassword", encryptPassword(blog.getHttppassword()));
        values.put("imagePlacement", blog.getImagePlacement());
        values.put("centerThumbnail", blog.isFeaturedImageCapable());
        values.put("fullSizeImage", blog.isFullSizeImage());
        values.put("maxImageWidth", blog.getMaxImageWidth());
        values.put("maxImageWidthId", blog.getMaxImageWidthId());
        values.put("location", blog.isLocation());
        values.put("postFormats", blog.getPostFormats());
        values.put("dotcom_username", blog.getDotcom_username());
        values.put("dotcom_password", encryptPassword(blog.getDotcom_password()));
        values.put("api_blogid", blog.getApi_blogid());
        values.put("api_key", blog.getApi_key());
        values.put("isScaledImage", blog.isScaledImage());
        values.put("scaledImgWidth", blog.getScaledImageWidth());
        values.put("blog_options", blog.getBlogOptions());
        values.put("isHidden", blog.isHidden());
        values.put("blogName", blog.getBlogName());
        values.put("isAdmin", blog.isAdmin());

        boolean returnValue = db.update(SETTINGS_TABLE, values, "id=" + blog.getLocalTableBlogId(),
                null) > 0;
        if (blog.isDotcomFlag()) {
            returnValue = updateWPComCredentials(blog.getUsername(), blog.getPassword());
        }

        return (returnValue);
    }

    public boolean updateWPComCredentials(String username, String password) {
        // update the login for wordpress.com blogs
        ContentValues userPass = new ContentValues();
        userPass.put("username", username);
        userPass.put("password", encryptPassword(password));
        return db.update(SETTINGS_TABLE, userPass, "username=\""
                + username + "\" AND dotcomFlag=1", null) > 0;
    }

    public boolean deleteAccount(Context ctx, int id) {
        // TODO: should this also delete posts and other related info?
        int rowsAffected = db.delete(SETTINGS_TABLE, "id=?", new String[]{Integer.toString(id)});
        deleteQuickPressShortcutsForAccount(ctx, id);
        return (rowsAffected > 0);
    }

    public void deleteAllAccounts() {
        List<Integer> ids = getAllAccountIDs();
        if (ids.size() == 0)
            return;

        db.beginTransaction();
        try {
            for (int id: ids) {
                deleteAccount(context, id);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Blog instantiateBlogByLocalId(int localId) {
        String[] fields =
                new String[]{"url", "blogName", "username", "password", "httpuser", "httppassword", "imagePlacement",
                             "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId", "runService",
                             "blogId", "location", "dotcomFlag", "dotcom_username", "dotcom_password", "api_key",
                             "api_blogid", "wpVersion", "postFormats", "lastCommentId", "isScaledImage",
                             "scaledImgWidth", "homeURL", "blog_options", "isAdmin", "isHidden"};
        Cursor c = db.query(SETTINGS_TABLE, fields, "id=" + localId, null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        Blog blog = new Blog();
        if (numRows > 0) {
            if (c.getString(0) != null) {
                blog.setLocalTableBlogId(localId);
                blog.setUrl(c.getString(c.getColumnIndex("url"))); // 0

                blog.setBlogName(c.getString(c.getColumnIndex("blogName"))); // 1
                blog.setUsername(c.getString(c.getColumnIndex("username"))); // 2
                blog.setPassword(decryptPassword(c.getString(c.getColumnIndex("password")))); // 3
                if (c.getString(c.getColumnIndex("httpuser")) == null) {
                    blog.setHttpuser("");
                } else {
                    blog.setHttpuser(c.getString(c.getColumnIndex("httpuser")));
                }
                if (c.getString(c.getColumnIndex("httppassword")) == null) {
                    blog.setHttppassword("");
                } else {
                    blog.setHttppassword(decryptPassword(c.getString(c.getColumnIndex("httppassword"))));
                }
                blog.setImagePlacement(c.getString(c.getColumnIndex("imagePlacement")));
                blog.setFeaturedImageCapable(c.getInt(c.getColumnIndex("centerThumbnail")) > 0);
                blog.setFullSizeImage(c.getInt(c.getColumnIndex("fullSizeImage")) > 0);
                blog.setMaxImageWidth(c.getString(c.getColumnIndex("maxImageWidth")));
                blog.setMaxImageWidthId(c.getInt(c.getColumnIndex("maxImageWidthId")));
                blog.setRunService(c.getInt(c.getColumnIndex("runService")) > 0);
                blog.setRemoteBlogId(c.getInt(c.getColumnIndex("blogId")));
                blog.setLocation(c.getInt(c.getColumnIndex("location")) > 0);
                blog.setDotcomFlag(c.getInt(c.getColumnIndex("dotcomFlag")) > 0);
                if (c.getString(c.getColumnIndex("dotcom_username")) != null) {
                    blog.setDotcom_username(c.getString(c.getColumnIndex("dotcom_username")));
                }
                if (c.getString(c.getColumnIndex("dotcom_password")) != null) {
                    blog.setDotcom_password(decryptPassword(c.getString(c.getColumnIndex("dotcom_password"))));
                }
                if (c.getString(c.getColumnIndex("api_key")) != null) {
                    blog.setApi_key(c.getString(c.getColumnIndex("api_key")));
                }
                if (c.getString(c.getColumnIndex("api_blogid")) != null) {
                    blog.setApi_blogid(c.getString(c.getColumnIndex("api_blogid")));
                }
                if (c.getString(c.getColumnIndex("wpVersion")) != null) {
                    blog.setWpVersion(c.getString(c.getColumnIndex("wpVersion")));
                }
                blog.setPostFormats(c.getString(c.getColumnIndex("postFormats")));
                blog.setLastCommentId(c.getInt(c.getColumnIndex("lastCommentId")));
                blog.setScaledImage(c.getInt(c.getColumnIndex("isScaledImage")) > 0);
                blog.setScaledImageWidth(c.getInt(c.getColumnIndex("scaledImgWidth")));
                blog.setHomeURL(c.getString(c.getColumnIndex("homeURL")));
                if (c.getString(c.getColumnIndex("blog_options")) == null) {
                    blog.setBlogOptions("");
                } else {
                    blog.setBlogOptions(c.getString(c.getColumnIndex("blog_options")));
                }
                blog.setAdmin(c.getInt(c.getColumnIndex("isAdmin")) > 0);
                blog.setHidden(c.getInt(c.getColumnIndex("isHidden")) > 0);
            }
        }
        c.close();
        return blog;
    }

    public Blog getBlogForDotComBlogId(String dotComBlogId) {
        Cursor c = db.query(SETTINGS_TABLE, new String[]{"id"}, "api_blogid=? OR (blogId=? AND dotcomFlag=1)", new String[]{dotComBlogId, dotComBlogId}, null, null, null);

        int id = -1;
        int numRows = c.getCount();
        c.moveToFirst();

        if (numRows > 0) {
            id = c.getInt(0);
        }

        c.close();
        try {
            return instantiateBlogByLocalId(id);
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> loadStatsLogin(int id) {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "dotcom_username",
                "dotcom_password" }, "id=" + id, null, null, null, null);

        c.moveToFirst();

        List<String> returnVector = new Vector<String>();
        if (c.getString(0) != null) {
            returnVector.add(c.getString(0));
            returnVector.add(decryptPassword(c.getString(1)));
        } else {
            returnVector = null;
        }
        c.close();

        return returnVector;
    }

    public boolean updateLatestCommentID(int id, long newCommentID) {
        boolean returnValue = false;

        synchronized (this) {
            ContentValues values = new ContentValues();
            values.put("lastCommentId", newCommentID);

            returnValue = db.update(SETTINGS_TABLE, values, "id=" + id, null) > 0;
        }

        return (returnValue);
    }

    public List<Integer> getNotificationAccounts() {

        Cursor c = null;
        try {
            c = db.query(SETTINGS_TABLE, new String[]{"id"}, "runService=1",
                    null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int numRows = c.getCount();
        c.moveToFirst();

        List<Integer> returnVector = new Vector<Integer>();
        for (int i = 0; i < numRows; ++i) {
            int tempID = c.getInt(0);
            returnVector.add(tempID);
            c.moveToNext();
        }

        c.close();

        return returnVector;
    }

    /*
     * Jetpack blogs have the "wpcom" blog_id stored in options->api_blogid. This is because self-hosted blogs have both
     * a blogID (local to their network), and a unique blogID on wpcom.
     */
    private int getLocalTableBlogIdForJetpackRemoteID(int remoteBlogId, String xmlRpcUrl) {
        if (TextUtils.isEmpty(xmlRpcUrl)) {
            String sql = "SELECT id FROM " + SETTINGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=?";
            String[] args = {Integer.toString(remoteBlogId)};
            return SqlUtils.intForQuery(db, sql, args);
        } else {
            String sql = "SELECT id FROM " + SETTINGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=? AND url=?";
            String[] args = {Integer.toString(remoteBlogId), xmlRpcUrl};
            return SqlUtils.intForQuery(db, sql, args);
        }
    }

    public int getLocalTableBlogIdForRemoteBlogId(int remoteBlogId) {
        int localBlogID = SqlUtils.intForQuery(db, "SELECT id FROM accounts WHERE blogId=?",
                new String[]{Integer.toString(remoteBlogId)});
        if (localBlogID == 0) {
            localBlogID = this.getLocalTableBlogIdForJetpackRemoteID(remoteBlogId, null);
        }
        return localBlogID;
    }

    public int getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(int remoteBlogId, String xmlRpcUrl) {
        int localBlogID = SqlUtils.intForQuery(db, "SELECT id FROM accounts WHERE blogId=? AND url=?",
                new String[]{Integer.toString(remoteBlogId), xmlRpcUrl});
        if (localBlogID==0) {
            localBlogID = this.getLocalTableBlogIdForJetpackRemoteID(remoteBlogId, xmlRpcUrl);
        }
        return localBlogID;
    }

    public int getRemoteBlogIdForLocalTableBlogId(int localBlogId) {
        int remoteBlogID = SqlUtils.intForQuery(db, "SELECT blogId FROM accounts WHERE id=?", new String[]{Integer.toString(localBlogId)});
        if (remoteBlogID<=1) { //Make sure we're not returning a wrong ID for jetpack blog.
            List<Map<String,Object>> allAccounts = this.getAccountsBy("dotcomFlag=0", new String[]{"api_blogid"});
            for (Map<String, Object> currentAccount : allAccounts) {
                if (MapUtils.getMapInt(currentAccount, "id")==localBlogId) {
                    remoteBlogID = MapUtils.getMapInt(currentAccount, "api_blogid");
                    break;
                }
            }
        }
        return remoteBlogID;
    }

    public void updateNotificationFlag(int id, boolean flag) {

        ContentValues values = new ContentValues();
        int iFlag = 0;
        if (flag) {
            iFlag = 1;
        }
        values.put("runService", iFlag);

        boolean returnValue = db.update(SETTINGS_TABLE, values,
                "id=" + String.valueOf(id), null) > 0;
        if (returnValue) {
        }

    }

    /**
     * Set the ID of the most recently active blog. This value will persist between application
     * launches.
     *
     * @param id ID of the most recently active blog.
     */
    public void updateLastBlogId(int id) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("last_blog_id", id);
        editor.commit();
    }

    /**
     * Delete the ID for the most recently active blog.
     */
    public void deleteLastBlogId() {
        updateLastBlogId(-1);
        // Clear the last selected activity
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("wp_pref_last_activity", -1);
        editor.commit();
    }

    /**
     * Get the ID of the most recently active blog. -1 is returned if there is no recently active
     * blog.
     */
    public int getLastBlogId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("last_blog_id", -1);
    }

    public List<Map<String, Object>> loadDrafts(int blogID,
            boolean loadPages) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c;
        if (loadPages)
            c = db.query(POSTS_TABLE, new String[] { "id", "title",
                    "post_status", "uploaded", "date_created_gmt",
                    "post_status" }, "blogID=" + blogID
                    + " AND localDraft=1 AND uploaded=0 AND isPage=1", null,
                    null, null, null);
        else
            c = db.query(POSTS_TABLE, new String[] { "id", "title",
                    "post_status", "uploaded", "date_created_gmt",
                    "post_status" }, "blogID=" + blogID
                    + " AND localDraft=1 AND uploaded=0 AND isPage=0", null,
                    null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getString(0));
                returnHash.put("title", c.getString(1));
                returnHash.put("status", c.getString(2));
                returnHash.put("uploaded", c.getInt(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("post_status", c.getString(5));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public boolean deletePost(Post post) {
        int result = db.delete(POSTS_TABLE,
                "blogID=? AND id=?",
                new String[]{String.valueOf(post.getBlogID()), String.valueOf(post.getId())});

        return (result == 1);
    }

    public Object[] arrayListToArray(Object array) {
        if (array instanceof ArrayList) {
            return ((ArrayList) array).toArray();
        }
        return (Object[]) array;
    }

    public boolean savePosts(List<?> postValues, int blogID, boolean isPage) {
        boolean returnValue = false;
        if (postValues.size() != 0) {
            for (int i = 0; i < postValues.size(); i++) {
                try {
                    ContentValues values = new ContentValues();
                    Map<?, ?> thisHash = (Map<?, ?>) postValues.get(i);
                    values.put("blogID", blogID);
                    if (thisHash.get((isPage) ? "page_id" : "postid") == null) {
                        return false;
                    }
                    String postID = thisHash.get((isPage) ? "page_id" : "postid").toString();
                    values.put("postid", postID);
                    values.put("title", thisHash.get("title").toString());
                    Date d;
                    try {
                        d = (Date) thisHash.get("dateCreated");
                        values.put("dateCreated", d.getTime());
                    } catch (Exception e) {
                        Date now = new Date();
                        values.put("dateCreated", now.getTime());
                    }
                    try {
                        d = (Date) thisHash.get("date_created_gmt");
                        values.put("date_created_gmt", d.getTime());
                    } catch (Exception e) {
                        d = new Date((Long) values.get("dateCreated"));
                        values.put("date_created_gmt", d.getTime() + (d.getTimezoneOffset() * 60000));
                    }
                    values.put("description", thisHash.get("description").toString());
                    values.put("link", thisHash.get("link").toString());
                    values.put("permaLink", thisHash.get("permaLink").toString());

                    Object[] categories = arrayListToArray(thisHash.get("categories"));
                    JSONArray jsonArray = new JSONArray();
                    if (categories != null) {
                        for (int x = 0; x < categories.length; x++) {
                            jsonArray.put(categories[x].toString());
                        }
                    }
                    values.put("categories", jsonArray.toString());

                    Object[] custom_fields = arrayListToArray(thisHash.get("custom_fields"));
                    jsonArray = new JSONArray();
                    if (custom_fields != null) {
                        for (int x = 0; x < custom_fields.length; x++) {
                            jsonArray.put(custom_fields[x].toString());
                            // Update geo_long and geo_lat from custom fields, if
                            // found:
                            Map<?, ?> customField = (Map<?, ?>) custom_fields[x];
                            if (customField.get("key") != null && customField.get("value") != null) {
                                if (customField.get("key").equals("geo_longitude")) {
                                    values.put("longitude", customField.get("value").toString());
                                }
                                if (customField.get("key").equals("geo_latitude")) {
                                    values.put("latitude", customField.get("value").toString());
                                }
                            }
                        }
                    }
                    values.put("custom_fields", jsonArray.toString());

                    values.put("mt_excerpt", thisHash.get((isPage) ? "excerpt" : "mt_excerpt").toString());
                    values.put("mt_text_more", thisHash.get((isPage) ? "text_more" : "mt_text_more").toString());
                    values.put("mt_allow_comments", (Integer) thisHash.get("mt_allow_comments"));
                    values.put("mt_allow_pings", (Integer) thisHash.get("mt_allow_pings"));
                    values.put("wp_slug", thisHash.get("wp_slug").toString());
                    values.put("wp_password", thisHash.get("wp_password").toString());
                    values.put("wp_author_id", thisHash.get("wp_author_id").toString());
                    values.put("wp_author_display_name", thisHash.get("wp_author_display_name").toString());
                    values.put("post_status", thisHash.get((isPage) ? "page_status" : "post_status").toString());
                    values.put("userid", thisHash.get("userid").toString());

                    int isPageInt = 0;
                    if (isPage) {
                        isPageInt = 1;
                        values.put("isPage", true);
                        values.put("wp_page_parent_id", thisHash.get("wp_page_parent_id").toString());
                        values.put("wp_page_parent_title", thisHash.get("wp_page_parent_title").toString());
                    } else {
                        values.put("mt_keywords", thisHash.get("mt_keywords").toString());
                        try {
                            values.put("wp_post_format", thisHash.get("wp_post_format").toString());
                        } catch (Exception e) {
                            values.put("wp_post_format", "");
                        }
                    }

                    int result = db.update(POSTS_TABLE, values, "postID=" + postID + " AND isPage=" + isPageInt, null);
                    if (result == 0) {
                        returnValue = db.insert(POSTS_TABLE, null, values) > 0;
                    } else {
                        returnValue = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return (returnValue);
    }

    public long savePost(Post post, int blogID) {
        long returnValue = -1;
        if (post != null) {

            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMt_text_more());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mt_keywords", post.getMt_keywords());
            values.put("wp_password", post.getWP_password());
            values.put("post_status", post.getPost_status());
            values.put("uploaded", post.isUploaded());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getWP_post_format());
            values.put("latitude", post.getLatitude());
            values.put("longitude", post.getLongitude());
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getMt_excerpt());

            returnValue = db.insert(POSTS_TABLE, null, values);

        }
        return (returnValue);
    }

    public int updatePost(Post post, int blogID) {
        int success = 0;
        if (post != null) {

            ContentValues values = new ContentValues();
            values.put("blogID", blogID);
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            if (post.getMt_text_more() != null)
                values.put("mt_text_more", post.getMt_text_more());
            values.put("uploaded", post.isUploaded());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mediaPaths", post.getMediaPaths());
            values.put("mt_keywords", post.getMt_keywords());
            values.put("wp_password", post.getWP_password());
            values.put("post_status", post.getPost_status());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getWP_post_format());
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getMt_excerpt());

            int pageInt = 0;
            if (post.isPage())
                pageInt = 1;

            success = db.update(POSTS_TABLE, values,
                    "blogID=" + post.getBlogID() + " AND id=" + post.getId()
                            + " AND isPage=" + pageInt, null);

        }
        return (success);
    }

    public List<Map<String, Object>> loadUploadedPosts(int blogID, boolean loadPages) {

        List<Map<String, Object>> returnVector = new Vector<Map<String, Object>>();
        Cursor c;
        if (loadPages)
            c = db.query(POSTS_TABLE,
                    new String[] { "id", "blogID", "postid", "title",
                            "date_created_gmt", "dateCreated", "post_status" },
                    "blogID=" + blogID + " AND localDraft != 1 AND isPage=1",
                    null, null, null, null);
        else
            c = db.query(POSTS_TABLE,
                    new String[] { "id", "blogID", "postid", "title",
                            "date_created_gmt", "dateCreated", "post_status" },
                    "blogID=" + blogID + " AND localDraft != 1 AND isPage=0",
                    null, null, null, null);

        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; ++i) {
            if (c.getString(0) != null) {
                Map<String, Object> returnHash = new HashMap<String, Object>();
                returnHash.put("id", c.getInt(0));
                returnHash.put("blogID", c.getString(1));
                returnHash.put("postID", c.getString(2));
                returnHash.put("title", c.getString(3));
                returnHash.put("date_created_gmt", c.getLong(4));
                returnHash.put("dateCreated", c.getLong(5));
                returnHash.put("post_status", c.getString(6));
                returnVector.add(i, returnHash);
            }
            c.moveToNext();
        }
        c.close();

        if (numRows == 0) {
            returnVector = null;
        }

        return returnVector;
    }

    public void deleteUploadedPosts(int blogID, boolean isPage) {

        if (isPage)
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=1", null);
        else
            db.delete(POSTS_TABLE, "blogID=" + blogID
                    + " AND localDraft != 1 AND isPage=0", null);

    }

    public List<Object> loadPost(int blogID, boolean isPage, long id) {
        List<Object> values = null;

        int pageInt = 0;
        if (isPage)
            pageInt = 1;
        Cursor c = db.query(POSTS_TABLE, null, "blogID=" + blogID + " AND id="
                + id + " AND isPage=" + pageInt, null, null, null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            if (c.getString(0) != null) {
                values = new Vector<Object>();
                values.add(c.getLong(0));
                values.add(c.getString(1));
                values.add(c.getString(2));
                values.add(c.getString(3));
                values.add(c.getLong(4));
                values.add(c.getLong(5));
                values.add(c.getString(6));
                values.add(c.getString(7));
                values.add(c.getString(8));
                values.add(c.getString(9));
                values.add(c.getInt(10));
                values.add(c.getInt(11));
                values.add(c.getString(12));
                values.add(c.getString(13));
                values.add(c.getString(14));
                values.add(c.getString(15));
                values.add(c.getString(16));
                values.add(c.getString(17));
                values.add(c.getString(18));
                values.add(c.getString(19));
                values.add(c.getString(20));
                values.add(c.getString(21));
                values.add(c.getString(22));
                values.add(c.getString(23));
                values.add(c.getDouble(24));
                values.add(c.getDouble(25));
                values.add(c.getInt(26));
                values.add(c.getInt(27));
                values.add(c.getInt(28));
                values.add(c.getInt(29));
            }
        }
        c.close();

        return values;
    }

    // Categories
    public boolean insertCategory(int id, int wp_id, int parent_id, String category_name) {

        ContentValues values = new ContentValues();
        values.put("blog_id", id);
        values.put("wp_id", wp_id);
        values.put("category_name", category_name.toString());
        values.put("parent_id", parent_id);
        boolean returnValue = false;
        synchronized (this) {
            returnValue = db.insert(CATEGORIES_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public List<String> loadCategories(int id) {

        Cursor c = db.query(CATEGORIES_TABLE, new String[] { "id", "wp_id",
                "category_name" }, "blog_id=" + id, null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        List<String> returnVector = new Vector<String>();
        for (int i = 0; i < numRows; ++i) {
            String category_name = c.getString(2);
            if (category_name != null) {
                returnVector.add(category_name);
            }
            c.moveToNext();
        }
        c.close();

        return returnVector;
    }

    public int getCategoryId(int id, String category) {
        Cursor c = db.query(CATEGORIES_TABLE, new String[] { "wp_id" },
                "category_name=? AND blog_id=?", new String[] {category, String.valueOf(id)},
                null, null, null);
        if (c.getCount() == 0)
            return 0;
        c.moveToFirst();
        int categoryID = 0;
        categoryID = c.getInt(0);

        c.close();

        return categoryID;
    }

    public int getCategoryParentId(int id, String category) {
        Cursor c = db.query(CATEGORIES_TABLE, new String[] { "parent_id" },
                "category_name=? AND blog_id=?", new String[] {category, String.valueOf(id)},
                null, null, null);
        if (c.getCount() == 0)
            return -1;
        c.moveToFirst();
        int categoryParentID = c.getInt(0);

        c.close();

        return categoryParentID;
    }

    public void clearCategories(int id) {

        // clear out the table since we are refreshing the whole enchilada
        db.delete(CATEGORIES_TABLE, "blog_id=" + id, null);

    }

    public boolean addQuickPressShortcut(int accountId, String name) {

        ContentValues values = new ContentValues();
        values.put("accountId", accountId);
        values.put("name", name);
        boolean returnValue = false;
        synchronized (this) {
            returnValue = db.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    /*
     * return all QuickPress shortcuts connected with the passed account
     */
    public List<Map<String, Object>> getQuickPressShortcuts(int accountId) {
        Cursor c = db.query(QUICKPRESS_SHORTCUTS_TABLE, new String[] { "id",
                "accountId", "name" }, "accountId = " + accountId, null, null,
                null, null);
        String id, name;
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> accounts = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {

            id = c.getString(0);
            name = c.getString(2);
            if (id != null) {
                Map<String, Object> thisHash = new HashMap<String, Object>();

                thisHash.put("id", id);
                thisHash.put("name", name);
                accounts.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();

        return accounts;
    }

    /*
     * delete QuickPress home screen shortcuts connected with the passed account
     */
    private void deleteQuickPressShortcutsForAccount(Context ctx, int accountId) {
        List<Map<String, Object>> shortcuts = getQuickPressShortcuts(accountId);
        if (shortcuts.size() == 0)
            return;

        String packageName = EditPostActivity.class.getPackage().getName();
        String className = EditPostActivity.class.getName();
        for (int i = 0; i < shortcuts.size(); i++) {
            Map<String, Object> shortcutHash = shortcuts.get(i);

            Intent shortcutIntent = new Intent();
            shortcutIntent.setClassName(packageName, className);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            Intent broadcastShortcutIntent = new Intent();
            broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                    shortcutIntent);
            broadcastShortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    shortcutHash.get("name").toString());
            broadcastShortcutIntent.putExtra("duplicate", false);
            broadcastShortcutIntent
                    .setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            ctx.sendBroadcast(broadcastShortcutIntent);

            // remove from shortcuts table
            String shortcutId = shortcutHash.get("id").toString();
            db.delete(QUICKPRESS_SHORTCUTS_TABLE, "id=?", new String[]{shortcutId});
        }
    }

    public static String encryptPassword(String clearText) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            String encrypedPwd = Base64.encodeToString(cipher.doFinal(clearText
                    .getBytes("UTF-8")), Base64.DEFAULT);
            return encrypedPwd;
        } catch (Exception e) {
        }
        return clearText;
    }

    public static String decryptPassword(String encryptedPwd) {
        try {
            DESKeySpec keySpec = new DESKeySpec(
                    PASSWORD_SECRET.getBytes("UTF-8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encryptedWithoutB64 = Base64.decode(encryptedPwd, Base64.DEFAULT);
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plainTextPwdBytes = cipher.doFinal(encryptedWithoutB64);
            return new String(plainTextPwdBytes);
        } catch (Exception e) {
        }
        return encryptedPwd;
    }

    private void migratePasswords() {

        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id", "password",
                "httppassword", "dotcom_password" }, null, null, null, null,
                null);
        int numRows = c.getCount();
        c.moveToFirst();

        for (int i = 0; i < numRows; i++) {
            ContentValues values = new ContentValues();

            if (c.getString(1) != null) {
                values.put("password", encryptPassword(c.getString(1)));
            }
            if (c.getString(2) != null) {
                values.put("httppassword", encryptPassword(c.getString(2)));
            }
            if (c.getString(3) != null) {
                values.put("dotcom_password", encryptPassword(c.getString(3)));
            }

            db.update(SETTINGS_TABLE, values, "id=" + c.getInt(0), null);

            c.moveToNext();
        }
        c.close();
    }

    public int getUnmoderatedCommentCount(int blogID) {
        int commentCount = 0;

        Cursor c = db
                .rawQuery(
                        "select count(*) from comments where blogID=? AND status='hold'",
                        new String[] { String.valueOf(blogID) });
        int numRows = c.getCount();
        c.moveToFirst();

        if (numRows > 0) {
            commentCount = c.getInt(0);
        }

        c.close();

        return commentCount;
    }

    public void saveMediaFile(MediaFile mf) {

        ContentValues values = new ContentValues();
        values.put("postID", mf.getPostID());
        values.put("filePath", mf.getFilePath());
        values.put("fileName", mf.getFileName());
        values.put("title", mf.getTitle());
        values.put("description", mf.getDescription());
        values.put("caption", mf.getCaption());
        values.put("horizontalAlignment", mf.getHorizontalAlignment());
        values.put("width", mf.getWidth());
        values.put("height", mf.getHeight());
        values.put("mimeType", mf.getMimeType());
        values.put("featured", mf.isFeatured());
        values.put("isVideo", mf.isVideo());
        values.put("isFeaturedInPost", mf.isFeaturedInPost());
        values.put("fileURL", mf.getFileURL());
        values.put("thumbnailURL", mf.getThumbnailURL());
        values.put("mediaId", mf.getMediaId());
        values.put("blogId", mf.getBlogId());
        values.put("date_created_gmt", mf.getDateCreatedGMT());
        values.put("videoPressShortcode", mf.getVideoPressShortCode());
        if (mf.getUploadState() != null)
            values.put("uploadState", mf.getUploadState());
        else
            values.putNull("uploadState");

        synchronized (this) {
            int result = 0;
            boolean isMarkedForDelete = false;
            if (mf.getMediaId() != null) {
                Cursor cursor = db.rawQuery("SELECT uploadState FROM " + MEDIA_TABLE + " WHERE mediaId=?",
                        new String[]{StringUtils.notNullStr(mf.getMediaId())});
                if (cursor != null && cursor.moveToFirst()) {
                    isMarkedForDelete = "delete".equals(cursor.getString(0));
                    cursor.close();
                }

                if (!isMarkedForDelete)
                    result = db.update(MEDIA_TABLE, values, "blogId=? AND mediaId=?",
                            new String[]{StringUtils.notNullStr(mf.getBlogId()), StringUtils.notNullStr(mf.getMediaId())});
            }

            if (result == 0 && !isMarkedForDelete) {
                result = db.update(MEDIA_TABLE, values, "postID=? AND filePath=?",
                        new String[]{String.valueOf(mf.getPostID()), StringUtils.notNullStr(mf.getFilePath())});
                if (result == 0)
                    db.insert(MEDIA_TABLE, null, values);
            }
        }

    }

    /** For a given blogId, get the first media files **/
    public Cursor getFirstMediaFileForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND " +
                           "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) ORDER BY (uploadState=?) DESC, date_created_gmt DESC LIMIT 1",
                new String[]{blogId, "uploading"});
    }

    /** For a given blogId, get all the media files **/
    public Cursor getMediaFilesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "uploading" });
    }

    /** For a given blogId, get all the media files with searchTerm **/
    public Cursor getMediaFilesForBlog(String blogId, String searchTerm) {
        // Currently on WordPress.com, the media search engine only searches the title.
        // We'll match this.

        String term = searchTerm.toLowerCase(Locale.getDefault());
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND title LIKE ? AND (uploadState IS NULL OR uploadState ='uploaded') ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "%" + term + "%", "uploading" });
    }

    /** For a given blogId, get the media file with the given media_id **/
    public Cursor getMediaFile(String blogId, String mediaId) {
        return db.rawQuery("SELECT * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?", new String[] { blogId, mediaId });
    }

    public int getMediaCountAll(String blogId) {
        Cursor cursor = getMediaFilesForBlog(blogId);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }


    public Cursor getMediaImagesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND mimeType LIKE ? ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "image%", "uploading" });
    }

    /** Ids in the filteredIds will not be selected **/
    public Cursor getMediaImagesForBlog(String blogId, ArrayList<String> filteredIds) {

        String mediaIdsStr = "";

        if (filteredIds != null && filteredIds.size() > 0) {
            mediaIdsStr = "AND mediaId NOT IN (";
            for (String mediaId : filteredIds) {
                mediaIdsStr += "'" + mediaId + "',";
            }
            mediaIdsStr = mediaIdsStr.subSequence(0, mediaIdsStr.length() - 1) + ")";
        }

        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND mimeType LIKE ? " + mediaIdsStr + " ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "image%", "uploading" });
    }

    public int getMediaCountImages(String blogId) {
        return getMediaImagesForBlog(blogId).getCount();
    }

    public Cursor getMediaUnattachedForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND " +
                "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND postId=0 ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "uploading" });
    }

    public int getMediaCountUnattached(String blogId) {
        return getMediaUnattachedForBlog(blogId).getCount();
    }

    public Cursor getMediaFilesForBlog(String blogId, long startDate, long endDate) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND (uploadState IS NULL OR uploadState ='uploaded') AND (date_created_gmt >= ? AND date_created_gmt <= ?) ", new String[] { blogId , String.valueOf(startDate), String.valueOf(endDate) });
    }

    public Cursor getMediaFiles(String blogId, ArrayList<String> mediaIds) {

        if (mediaIds == null || mediaIds.size() == 0)
            return null;

        String mediaIdsStr = "(";
        for (String mediaId : mediaIds) {
            mediaIdsStr += "'" + mediaId + "',";
        }
        mediaIdsStr = mediaIdsStr.subSequence(0, mediaIdsStr.length() - 1) + ")";

        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId IN " + mediaIdsStr, new String[] { blogId });
    }

    public MediaFile getMediaFile(String src, Post post) {

        Cursor c = db.query(MEDIA_TABLE, null, "postID=" + post.getId()
                + " AND filePath='" + src + "'", null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        MediaFile mf = new MediaFile();
        if (numRows >= 1) {
            mf.setPostID(c.getInt(1));
            mf.setFilePath(c.getString(2));
            mf.setFileName(c.getString(3));
            mf.setTitle(c.getString(4));
            mf.setDescription(c.getString(5));
            mf.setCaption(c.getString(6));
            mf.setHorizontalAlignment(c.getInt(7));
            mf.setWidth(c.getInt(8));
            mf.setHeight(c.getInt(9));
            mf.setMimeType(c.getString(10));
            mf.setFeatured(c.getInt(11) > 0);
            mf.setVideo(c.getInt(12) > 0);
            mf.setFeaturedInPost(c.getInt(13) > 0);
            mf.setFileURL(c.getString(14));
            mf.setThumbnailURL(c.getString(15));
            mf.setMediaId(c.getString(16));
            mf.setBlogId(c.getString(17));
            mf.setDateCreatedGMT(c.getLong(18));
            mf.setUploadState(c.getString(19));
            mf.setVideoPressShortCode(c.getString(20));
        } else {
            c.close();
            return null;
        }
        c.close();

        return mf;
    }

    public void deleteMediaFilesForPost(Post post) {

        db.delete(MEDIA_TABLE, "blogId='" + post.getBlogID() + "' AND postID=" + post.getId(), null);

    }

    /** Get the queued media files for upload for a given blogId **/
    public Cursor getMediaUploadQueue(String blogId) {
        return db.rawQuery("SELECT * FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=?", new String[] {"queued", blogId});
    }

    /** Update a media file to a new upload state **/
    public void updateMediaUploadState(String blogId, String mediaId, String uploadState) {
        if (blogId == null || blogId.equals(""))
            return;

        ContentValues values = new ContentValues();
        if (uploadState == null) values.putNull("uploadState");
        else values.put("uploadState", uploadState);

        if (mediaId == null) {
            db.update(MEDIA_TABLE, values, "blogId=? AND (uploadState IS NULL OR uploadState ='uploaded')", new String[] { blogId });
        } else {
            db.update(MEDIA_TABLE, values, "blogId=? AND mediaId=?", new String[] { blogId, mediaId });
        }
    }

    public void updateMediaFile(String blogId, String mediaId, String title, String description, String caption) {
        if (blogId == null || blogId.equals("")) {
            return;
        }

        ContentValues values = new ContentValues();

        if (title == null || title.equals("")) {
            values.put("title", "");
        } else {
            values.put("title", title);
        }

        if (title == null || title.equals("")) {
            values.put("description", "");
        } else {
            values.put("description", description);
        }

        if (caption == null || caption.equals("")) {
            values.put("caption", "");
        } else {
            values.put("caption", caption);
        }

        db.update(MEDIA_TABLE, values, "blogId = ? AND mediaId=?", new String[] { blogId, mediaId });
    }

    /**
     * For a given blogId, set all uploading states to failed.
     * Useful for cleaning up files stuck in the "uploading" state.
     **/
    public void setMediaUploadingToFailed(String blogId) {
        if (blogId == null || blogId.equals(""))
            return;

        ContentValues values = new ContentValues();
        values.put("uploadState", "failed");
        db.update(MEDIA_TABLE, values, "blogId=? AND uploadState=?", new String[] { blogId, "uploading" });
    }

    /** For a given blogId, clear the upload states in the upload queue **/
    public void clearMediaUploaded(String blogId) {
        if (blogId == null || blogId.equals(""))
            return;

        ContentValues values = new ContentValues();
        values.putNull("uploadState");
        db.update(MEDIA_TABLE, values, "blogId=? AND uploadState=?", new String[] { blogId, "uploaded" });
    }

    /** Delete a media item from a blog locally **/
    public void deleteMediaFile(String blogId, String mediaId) {
        db.delete(MEDIA_TABLE, "blogId=? AND mediaId=?", new String[] { blogId, mediaId });
    }

    /** Mark media files for deletion without actually deleting them. **/
    public void setMediaFilesMarkedForDelete(String blogId, List<String> ids) {
        // This is for queueing up files to delete on the server
        for (String id : ids)
            updateMediaUploadState(blogId, id, "delete");
    }

    /** Mark media files as deleted without actually deleting them **/
    public void setMediaFilesMarkedForDeleted(String blogId) {
        // This is for syncing our files to the server:
        // when we pull from the server, everything that is still 'deleted'
        // was not downloaded from the server and can be removed via deleteFilesMarkedForDeleted()
        updateMediaUploadState(blogId, null, "deleted");
    }

    /** Delete files marked as deleted **/
    public void deleteFilesMarkedForDeleted(String blogId) {
        db.delete(MEDIA_TABLE, "blogId=? AND uploadState=?", new String[] { blogId, "deleted" });
    }

    /** Get a media file scheduled for delete for a given blogId **/
    public Cursor getMediaDeleteQueueItem(String blogId) {
        return db.rawQuery("SELECT blogId, mediaId FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=? LIMIT 1",
                new String[]{"delete", blogId});
    }


    public int getWPCOMBlogID() {
        int id = -1;
        Cursor c = db.query(SETTINGS_TABLE, new String[] { "id" },
                "dotcomFlag=1", null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        if (numRows > 0) {
            id = c.getInt(0);
        }

        c.close();

        return id;
    }

    public boolean findLocalChanges(int blogId, boolean isPage) {
        Cursor c = db.query(POSTS_TABLE, null,
                "isLocalChange=? AND blogID=? AND isPage=?", new String[]{"1", String.valueOf(blogId), (isPage) ? "1" : "0"}, null, null, null);
        int numRows = c.getCount();
        c.close();
        if (numRows > 0) {
            return true;
        }

        return false;
    }

    public boolean saveTheme(Theme theme) {
        boolean returnValue = false;

        ContentValues values = new ContentValues();
        values.put("themeId", theme.getThemeId());
        values.put("name", theme.getName());
        values.put("description", theme.getDescription());
        values.put("screenshotURL", theme.getScreenshotURL());
        values.put("trendingRank", theme.getTrendingRank());
        values.put("popularityRank", theme.getPopularityRank());
        values.put("launchDate", theme.getLaunchDateMs());
        values.put("previewURL", theme.getPreviewURL());
        values.put("blogId", theme.getBlogId());
        values.put("isCurrent", theme.isCurrent());
        values.put("isPremium", theme.isPremium());
        values.put("features", theme.getFeatures());

        synchronized (this) {
            int result = db.update(
                    THEMES_TABLE,
                    values,
                    "themeId=?",
                    new String[]{ theme.getThemeId() });
            if (result == 0)
                returnValue = db.insert(THEMES_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public Cursor getThemesAtoZ(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? ORDER BY name COLLATE NOCASE ASC", new String[] { blogId });
    }

    public Cursor getThemesTrending(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? ORDER BY trendingRank ASC", new String[] { blogId });
    }

    public Cursor getThemesPopularity(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? ORDER BY popularityRank ASC", new String[] { blogId });
    }

    public Cursor getThemesNewest(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? ORDER BY launchDate DESC", new String[] { blogId });
    }

    /*public Cursor getThemesPremium(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? AND price > 0 ORDER BY name ASC", new String[] { blogId });
    }

    public Cursor getThemesFriendsOfWP(String blogId) {
        return db.rawQuery("SELECT _id, themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? AND themeId LIKE ? ORDER BY popularityRank ASC", new String[] { blogId, "partner-%" });
    }

    public Cursor getCurrentTheme(String blogId) {
        return db.rawQuery("SELECT _id,  themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? AND isCurrentTheme='true'", new String[] { blogId });
    }*/

    public String getCurrentThemeId(String blogId) {
        return DatabaseUtils.stringForQuery(db, "SELECT themeId FROM " + THEMES_TABLE + " WHERE blogId=? AND isCurrent='1'", new String[] { blogId });
    }

    public void setCurrentTheme(String blogId, String themeId) {

        // update any old themes that are set to true to false
        ContentValues values = new ContentValues();
        values.put("isCurrent", false);
        db.update(THEMES_TABLE, values, "blogID=? AND isCurrent='1'", new String[] { blogId });

        values = new ContentValues();
        values.put("isCurrent", true);
        db.update(THEMES_TABLE, values, "blogId=? AND themeId=?", new String[] { blogId, themeId });
    }

    public int getThemeCount(String blogId) {
        return getThemesAtoZ(blogId).getCount();
    }

    public Cursor getThemes(String blogId, String searchTerm) {
        return db.rawQuery("SELECT _id,  themeId, name, screenshotURL, isCurrent, isPremium FROM " + THEMES_TABLE + " WHERE blogId=? AND (name LIKE ? OR description LIKE ?) ORDER BY name ASC", new String[] {blogId, "%" + searchTerm + "%", "%" + searchTerm + "%"});

    }

    public Theme getTheme(String blogId, String themeId) {
        Cursor cursor = db.rawQuery("SELECT name, description, screenshotURL, previewURL, isCurrent, isPremium, features FROM " + THEMES_TABLE + " WHERE blogId=? AND themeId=?", new String[]{blogId, themeId});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            String description = cursor.getString(1);
            String screenshotURL = cursor.getString(2);
            String previewURL = cursor.getString(3);
            boolean isCurrent = cursor.getInt(4) == 1;
            boolean isPremium = cursor.getInt(5) == 1;
            String features = cursor.getString(6);

            Theme theme = new Theme();
            theme.setThemeId(themeId);
            theme.setName(name);
            theme.setDescription(description);
            theme.setScreenshotURL(screenshotURL);
            theme.setPreviewURL(previewURL);
            theme.setCurrent(isCurrent);
            theme.setPremium(isPremium);
            theme.setFeatures(features);

            cursor.close();

            return theme;
        } else {
            cursor.close();
            return null;
        }
    }

    public ArrayList<Note> getLatestNotes() {
        return getLatestNotes(20);
    }

    public ArrayList<Note> getLatestNotes(int limit) {
        Cursor cursor = db.query(NOTES_TABLE, new String[] {"note_id", "raw_note_data", "placeholder"},
                null, null, null, null, "timestamp DESC", "" + limit);
        ArrayList<Note> notes = new ArrayList<Note>();
        while (cursor.moveToNext()) {
            String note_id = cursor.getString(0);
            String raw_note_data = cursor.getString(1);
            boolean placeholder = cursor.getInt(2) == 1;
            try {
                Note note = new Note(new JSONObject(raw_note_data));
                note.setPlaceholder(placeholder);
                notes.add(note);
            } catch (JSONException e) {
                AppLog.e(T.DB, "Can't parse notification with note_id:" + note_id + ", exception:" + e);
            }
        }
        cursor.close();
        return notes;
    }

    public void removePlaceholderNotes() {
        db.delete(NOTES_TABLE, "placeholder=1", null);
    }

    public void addNote(Note note, boolean placeholder) {
        ContentValues values = new ContentValues();
        values.put("type", note.getType());
        values.put("timestamp", note.getTimestamp());
        values.put("placeholder", placeholder);
        values.put("raw_note_data", note.toJSONObject().toString()); // easiest way to store schema-less data

        if (note.getId().equals("0") || note.getId().equals("")) {
            values.put("id", generateIdFor(note));
            values.put("note_id", "0");
        } else {
            values.put("id", note.getId());
            values.put("note_id", note.getId());
        }

        db.insertWithOnConflict(NOTES_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static int generateIdFor(Note note) {
        if (note == null) {
            return 0;
        }
        return StringUtils.getMd5IntHash(note.getSubject() + note.getType()).intValue();
    }

    public void saveNotes(List<Note> notes, boolean clearBeforeSaving) {
        db.beginTransaction();
        try {
            if (clearBeforeSaving)
                clearNotes();
            for (Note note: notes)
                addNote(note, false);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public Note getNoteById(int id) {
        Cursor cursor = db.query(NOTES_TABLE, new String[] {"raw_note_data"},  "id=" + id, null, null, null, null);
        cursor.moveToFirst();

        try {
            JSONObject jsonNote = new JSONObject(cursor.getString(0));
            return new Note(jsonNote);
        } catch (JSONException e) {
            AppLog.e(T.DB, "Can't parse JSON Note: " + e);
            return null;
        } catch (CursorIndexOutOfBoundsException e) {
            AppLog.v(T.DB, "No Note with this id: " + e);
            return null;
        }
    }

    protected void clearNotes() {
        db.delete(NOTES_TABLE, null, null);
    }

    /*
     * nbradbury - used during development to copy database to SD card so we can access it via DDMS
     */
    protected void copyDatabase() {
        String copyFrom = db.getPath();
        String copyTo = WordPress.getContext().getExternalFilesDir(null).getAbsolutePath() + "/" + DATABASE_NAME + ".db";

        try {
            InputStream input = new FileInputStream(copyFrom);
            OutputStream output = new FileOutputStream(copyTo);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0)
                output.write(buffer, 0, length);

            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            AppLog.e(T.DB, "failed to copy database", e);
        }
    }
}
