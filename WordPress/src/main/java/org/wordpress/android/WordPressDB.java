package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.wordpress.android.datasets.AccountTable;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.models.PostsListPostList;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.media.services.MediaEvents.MediaChanged;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.ShortcodeUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.util.helpers.MediaFile;

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
import java.util.Set;
import java.util.Vector;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import de.greenrobot.event.EventBus;

public class WordPressDB {
    public static final String COLUMN_NAME_ID                    = "_id";
    public static final String COLUMN_NAME_POST_ID               = "postID";
    public static final String COLUMN_NAME_FILE_PATH             = "filePath";
    public static final String COLUMN_NAME_FILE_NAME             = "fileName";
    public static final String COLUMN_NAME_TITLE                 = "title";
    public static final String COLUMN_NAME_DESCRIPTION           = "description";
    public static final String COLUMN_NAME_CAPTION               = "caption";
    public static final String COLUMN_NAME_HORIZONTAL_ALIGNMENT  = "horizontalAlignment";
    public static final String COLUMN_NAME_WIDTH                 = "width";
    public static final String COLUMN_NAME_HEIGHT                = "height";
    public static final String COLUMN_NAME_MIME_TYPE             = "mimeType";
    public static final String COLUMN_NAME_FEATURED              = "featured";
    public static final String COLUMN_NAME_IS_VIDEO              = "isVideo";
    public static final String COLUMN_NAME_IS_FEATURED_IN_POST   = "isFeaturedInPost";
    public static final String COLUMN_NAME_FILE_URL              = "fileURL";
    public static final String COLUMN_NAME_THUMBNAIL_URL         = "thumbnailURL";
    public static final String COLUMN_NAME_MEDIA_ID              = "mediaId";
    public static final String COLUMN_NAME_BLOG_ID               = "blogId";
    public static final String COLUMN_NAME_DATE_CREATED_GMT      = "date_created_gmt";
    public static final String COLUMN_NAME_VIDEO_PRESS_SHORTCODE = "videoPressShortcode";
    public static final String COLUMN_NAME_UPLOAD_STATE          = "uploadState";

    private static final int DATABASE_VERSION = 48;

    private static final String CREATE_TABLE_BLOGS = "create table if not exists accounts (id integer primary key autoincrement, "
            + "url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer);";
    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";
    public static final String BLOGS_TABLE = "accounts";

    // Warning if you rename DATABASE_NAME, that could break previous App backups (see: xml/backup_scheme.xml)
    private static final String DATABASE_NAME = "wordpress";
    private static final String MEDIA_TABLE = "media";
    private static final String NOTES_TABLE = "notes";

    private static final String CREATE_TABLE_POSTS =
        "create table if not exists posts ("
            + "id integer primary key autoincrement,"
            + "blogID text,"
            + "postid text,"
            + "title text default '',"
            + "dateCreated date,"
            + "date_created_gmt date,"
            + "categories text default '',"
            + "custom_fields text default '',"
            + "description text default '',"
            + "link text default '',"
            + "mt_allow_comments boolean,"
            + "mt_allow_pings boolean,"
            + "mt_excerpt text default '',"
            + "mt_keywords text default '',"
            + "mt_text_more text default '',"
            + "permaLink text default '',"
            + "post_status text default '',"
            + "userid integer default 0,"
            + "wp_author_display_name text default '',"
            + "wp_author_id text default '',"
            + "wp_password text default '',"
            + "wp_post_format text default '',"
            + "wp_slug text default '',"
            + "mediaPaths text default '',"
            + "latitude real,"
            + "longitude real,"
            + "localDraft boolean default 0,"
            + "isPage boolean default 0,"
            + "wp_page_parent_id text,"
            + "wp_page_parent_title text);";

    private static final String POSTS_TABLE = "posts";

    private static final String THEMES_TABLE = "themes";
    private static final String CREATE_TABLE_THEMES = "create table if not exists themes ("
            + COLUMN_NAME_ID + " integer primary key autoincrement, "
            + Theme.ID + " text, "
            + Theme.AUTHOR + " text, "
            + Theme.SCREENSHOT + " text, "
            + Theme.AUTHOR_URI + " text, "
            + Theme.DEMO_URI + " text, "
            + Theme.NAME + " text, "
            + Theme.STYLESHEET + " text, "
            + Theme.PRICE + " text, "
            + Theme.BLOG_ID + " text, "
            + Theme.IS_CURRENT + " boolean default false);";

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

    // add wp_post_thumbnail to posts table
    private static final String ADD_POST_THUMBNAIL = "alter table posts add wp_post_thumbnail integer default 0;";

    // add postid and blogID indexes to posts table
    private static final String ADD_POST_ID_INDEX = "CREATE INDEX idx_posts_post_id ON posts(postid);";
    private static final String ADD_BLOG_ID_INDEX = "CREATE INDEX idx_posts_blog_id ON posts(blogID);";

    //add boolean to track if featured image should be included in the post content
    private static final String ADD_FEATURED_IN_POST = "alter table media add isFeaturedInPost boolean default false;";

    // add home url to blog settings
    private static final String ADD_HOME_URL = "alter table accounts add homeURL text default '';";

    private static final String ADD_BLOG_OPTIONS = "alter table accounts add blog_options text default '';";

    // add category parent id to keep track of category hierarchy
    private static final String ADD_PARENTID_IN_CATEGORIES = "alter table cats add parent_id integer default 0;";

    // add admin flag to blog settings
    private static final String ADD_BLOGS_ADMIN_FLAG = "alter table accounts add isAdmin boolean default false;";

    // add thumbnailURL, thumbnailPath and fileURL to media
    private static final String ADD_MEDIA_THUMBNAIL_URL = "alter table media add thumbnailURL text default '';";
    private static final String ADD_MEDIA_FILE_URL = "alter table media add fileURL text default '';";
    private static final String ADD_MEDIA_UNIQUE_ID = "alter table media add mediaId text default '';";
    private static final String ADD_MEDIA_BLOG_ID = "alter table media add blogId text default '';";
    private static final String ADD_MEDIA_DATE_GMT = "alter table media add date_created_gmt date;";
    private static final String ADD_MEDIA_UPLOAD_STATE = "alter table media add uploadState default '';";
    private static final String ADD_MEDIA_VIDEOPRESS_SHORTCODE = "alter table media add videoPressShortcode text default '';";

    // add hidden flag to blog settings (accounts)
    private static final String ADD_BLOGS_HIDDEN_FLAG = "alter table accounts add isHidden boolean default 0;";

    // add plan_product_id to blog
    private static final String ADD_BLOGS_PLAN_ID = "alter table accounts add plan_product_id integer default 0;";

    // add plan_product_name_short to blog
    private static final String ADD_BLOGS_PLAN_PRODUCT_NAME_SHORT = "alter table accounts add plan_product_name_short text default '';";

    // add capabilities to blog
    private static final String ADD_BLOGS_CAPABILITIES = "alter table accounts add capabilities text default '';";

    // used for migration
    private static final String DEPRECATED_WPCOM_USERNAME_PREFERENCE = "wp_pref_wpcom_username";
    private static final String DEPRECATED_ACCESS_TOKEN_PREFERENCE = "wp_pref_wpcom_access_token";

    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";

    private SQLiteDatabase db;

    protected static final String PASSWORD_SECRET = BuildConfig.DB_SECRET;
    private Context context;

    public WordPressDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_BLOGS);
        db.execSQL(CREATE_TABLE_POSTS);
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        db.execSQL(CREATE_TABLE_MEDIA);
        db.execSQL(CREATE_TABLE_THEMES);
        SiteSettingsTable.createTable(db);
        CommentTable.createTables(db);
        SuggestionTable.createTables(db);

        // Update tables for new installs and app updates
        int currentVersion = db.getVersion();
        boolean isNewInstall = (currentVersion == 0);

        if (!isNewInstall && currentVersion != DATABASE_VERSION) {
            AppLog.d(T.DB, "upgrading database from version " + currentVersion + " to " + DATABASE_VERSION);
        }

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
                db.execSQL(ADD_BLOGS_ADMIN_FLAG);
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
                db.execSQL(ADD_BLOGS_HIDDEN_FLAG);
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
                currentVersion++;
            case 25:
                //ver 26 "virtually" remove columns 'lastCommentId' and 'runService' from the DB
                //SQLite supports a limited subset of ALTER TABLE.
                //The ALTER TABLE command in SQLite allows the user to rename a table or to add a new column to an existing table.
                //It is not possible to rename a column, remove a column, or add or remove constraints from a table.
                currentVersion++;
            case 26:
                // Drop the notes table, no longer needed with Simperium.
                db.execSQL(DROP_TABLE_PREFIX + NOTES_TABLE);
                currentVersion++;
            case 27:
                // versions prior to v4.5 added an "isUploading" column here, but that's no longer used
                // so we don't bother to add it
                currentVersion++;
            case 28:
                // Remove WordPress.com credentials
                removeDotComCredentials();
                currentVersion++;
            case 29:
                // Migrate WordPress.com token and infos to the DB
                AccountTable.createTables(db);
                if (!isNewInstall) {
                    migratePreferencesToAccountTable(context);
                }
                currentVersion++;
            case 30:
                // Fix big comments issue #2855
                CommentTable.deleteBigComments(db);
                currentVersion++;
            case 31:
                // add wp_post_thumbnail to posts table
                db.execSQL(ADD_POST_THUMBNAIL);
                currentVersion++;
            case 32:
                // add postid index and blogID index to posts table
                db.execSQL(ADD_POST_ID_INDEX);
                db.execSQL(ADD_BLOG_ID_INDEX);
                currentVersion++;
            case 33:
                deleteUploadedLocalDrafts();
                currentVersion++;
            case 34:
                AccountTable.migrationAddEmailAddressField(db);
                currentVersion++;
            case 35:
                // Delete simperium DB - from 4.6 to 4.6.1
                // Fix an issue when note id > MAX_INT
                ctx.deleteDatabase("simperium-store");
                currentVersion++;
            case 36:
                // Delete simperium DB again - from 4.6.1 to 4.7
                // Fix a sync issue happening for users who have both wpios and wpandroid active clients
                ctx.deleteDatabase("simperium-store");
                currentVersion++;
            case 37:
                resetThemeTable();
                currentVersion++;
            case 38:
                updateDotcomFlag();
                currentVersion++;
            case 39:
                AccountTable.migrationAddFirstNameLastNameAboutMeFields(db);
                currentVersion++;
            case 40:
                AccountTable.migrationAddDateFields(db);
                currentVersion++;
            case 41:
                AccountTable.migrationAddAccountSettingsFields(db);
                currentVersion++;
            case 42:
                db.execSQL(ADD_BLOGS_PLAN_ID);
                currentVersion++;
            case 43:
                db.execSQL(ADD_BLOGS_PLAN_PRODUCT_NAME_SHORT);
                currentVersion++;
            case 44:
                PeopleTable.createTables(db);
                currentVersion++;
            case 45:
                db.execSQL(ADD_BLOGS_CAPABILITIES);
                currentVersion++;
            case 46:
                AppPrefs.setVisualEditorAvailable(true);
                AppPrefs.setVisualEditorEnabled(true);
                currentVersion++;
            case 47:
                PeopleTable.reset(db);
                currentVersion++;
        }
        db.setVersion(DATABASE_VERSION);
    }

    private void updateDotcomFlag() {
        // Loop over all .com blogs in the app and check that are really hosted on wpcom
        List<Map<String, Object>> allBlogs = getBlogsBy("dotcomFlag=1", null, 0, false);
        for (Map<String, Object> blog : allBlogs) {
            String xmlrpcURL = MapUtils.getMapStr(blog, "url");
            if (!WPUrlUtils.isWordPressCom(xmlrpcURL)) {
                // .org blog marked as .com. Fix it.
                int blogID = MapUtils.getMapInt(blog, "id");
                if (blogID > 0) {
                    ContentValues values = new ContentValues();
                    values.put("dotcomFlag", false); // Mark as .org blog
                    db.update(BLOGS_TABLE, values, "id=" + blogID, null);
                }
            }
        }
    }

    /*
     * v4.5 (db version 34) no longer uses the "uploaded" column, and it's no longer added to the
     * db upon creation - however, earlier versions would set "uploaded=1" for local drafts after
     * they were uploaded and then exclude these "uploaded local drafts" from the post list - so
     * we must delete these posts to avoid having them appear (as dups) in the post list.
     */
    private void deleteUploadedLocalDrafts() {
        try {
            int numDeleted = db.delete(POSTS_TABLE, "uploaded=1 AND localDraft=1", null);
            if (numDeleted > 0) {
                AppLog.i(T.DB, "deleted " + numDeleted + " uploaded local drafts");
            }
        } catch (SQLiteException e) {
            // ignore - "uploaded" column doesn't exist
        }
    }

    private void resetThemeTable() {
        db.execSQL(DROP_TABLE_PREFIX + THEMES_TABLE);
        db.execSQL(CREATE_TABLE_THEMES);
    }

    private void migratePreferencesToAccountTable(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String oldAccessToken = settings.getString(DEPRECATED_ACCESS_TOKEN_PREFERENCE, null);
        String oldUsername = settings.getString(DEPRECATED_WPCOM_USERNAME_PREFERENCE, null);
        Account account = new Account();
        account.setUserName(oldUsername);
        if (oldAccessToken != null) {
            account.setAccessToken(oldAccessToken);
        }
        AccountTable.save(account, db);

        // Remove preferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.remove(DEPRECATED_WPCOM_USERNAME_PREFERENCE);
        editor.remove(DEPRECATED_ACCESS_TOKEN_PREFERENCE);
        editor.apply();
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }

    public static void deleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    private void migrateWPComAccount() {
        Cursor c = db.query(BLOGS_TABLE, new String[]{"username"}, "dotcomFlag=1", null, null,
                null, null);

        if (c.getCount() > 0) {
            c.moveToFirst();
            String username = c.getString(0);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(DEPRECATED_WPCOM_USERNAME_PREFERENCE, username);
            editor.commit();
        }

        c.close();
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
        values.put("blogId", blog.getRemoteBlogId());
        values.put("dotcomFlag", blog.isDotcomFlag());
        values.put("plan_product_id", blog.getPlanID());
        values.put("plan_product_name_short", blog.getPlanShortName());
        if (blog.getWpVersion() != null) {
            values.put("wpVersion", blog.getWpVersion());
        } else {
            values.putNull("wpVersion");
        }
        values.put("isAdmin", blog.isAdmin());
        values.put("isHidden", blog.isHidden());
        values.put("capabilities", blog.getCapabilities());
        return db.insert(BLOGS_TABLE, null, values) > -1;
    }

    public List<Integer> getAllBlogsIDs() {
        Cursor c = db.rawQuery("SELECT DISTINCT id FROM " + BLOGS_TABLE, null);
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

    public List<Map<String, Object>> getBlogsBy(String byString, String[] extraFields) {
        return getBlogsBy(byString, extraFields, 0, true);
    }

    public List<Map<String, Object>> getBlogsBy(String byString, String[] extraFields,
                                                int limit, boolean hideJetpackWithoutCredentials) {
        if (db == null) {
            return new Vector<>();
        }

        if (hideJetpackWithoutCredentials) {
            // Hide Jetpack blogs that were added in FetchBlogListWPCom
            // They will have a false dotcomFlag and an empty (but encrypted) password
            String hideJetpackArgs = String.format("NOT(dotcomFlag=0 AND password='%s')", encryptPassword(""));
            if (TextUtils.isEmpty(byString)) {
                byString = hideJetpackArgs;
            } else {
                byString = hideJetpackArgs + " AND " + byString;
            }
        }

        String limitStr = null;
        if (limit != 0) {
            limitStr = String.valueOf(limit);
        }
        String[] baseFields = new String[]{"id", "blogName", "username", "blogId", "url"};
        String[] allFields = baseFields;
        if (extraFields != null) {
            allFields = (String[]) ArrayUtils.addAll(baseFields, extraFields);
        }
        Cursor c = db.query(BLOGS_TABLE, allFields, byString, null, null, null, null, limitStr);
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> blogs = new Vector<>();
        for (int i = 0; i < numRows; i++) {
            int id = c.getInt(0);
            String blogName = c.getString(1);
            String username = c.getString(2);
            int blogId = c.getInt(3);
            String url = c.getString(4);
            if (id > 0) {
                Map<String, Object> blogMap = new HashMap<>();
                blogMap.put("id", id);
                blogMap.put("blogName", blogName);
                blogMap.put("username", username);
                blogMap.put("blogId", blogId);
                blogMap.put("url", url);
                int extraFieldsIndex = baseFields.length;
                if (extraFields != null) {
                    for (int j = 0; j < extraFields.length; ++j) {
                        blogMap.put(extraFields[j], c.getString(extraFieldsIndex + j));
                    }
                }
                blogs.add(blogMap);
            }
            c.moveToNext();
        }
        c.close();
        Collections.sort(blogs, BlogUtils.BlogNameComparator);
        return blogs;
    }

    public List<Map<String, Object>> getVisibleBlogs() {
        return getBlogsBy("isHidden = 0", null);
    }

    public int getFirstVisibleBlogId() {
        return SqlUtils.intForQuery(db, "SELECT id FROM " + BLOGS_TABLE + " WHERE isHidden = 0 LIMIT 1", null);
    }

    public int getFirstHiddenBlogId() {
        return SqlUtils.intForQuery(db, "SELECT id FROM " + BLOGS_TABLE + " WHERE isHidden = 1 LIMIT 1", null);
    }

    public List<Map<String, Object>> getVisibleDotComBlogs() {
        return getBlogsBy("isHidden = 0 AND dotcomFlag = 1", null);
    }

    public int getNumVisibleBlogs() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + BLOGS_TABLE + " WHERE isHidden = 0", null);
    }

    public int getNumHiddenBlogs() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + BLOGS_TABLE + " WHERE isHidden = 1", null);
    }

    public int getNumDotComBlogs() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + BLOGS_TABLE + " WHERE dotcomFlag = 1", null);
    }

    public int getNumBlogs() {
        return SqlUtils.intForQuery(db, "SELECT COUNT(*) FROM " + BLOGS_TABLE, null);
    }

    // Removes stored DotCom credentials. As of March 2015 only the OAuth token is used
    private void removeDotComCredentials() {
        // First clear out the password for all WP.com sites
        ContentValues dotComValues = new ContentValues();
        dotComValues.put("password", "");
        db.update(BLOGS_TABLE, dotComValues, "dotcomFlag=1", null);

        // Next, we'll clear out the credentials stored for Jetpack sites
        ContentValues jetPackValues = new ContentValues();
        jetPackValues.put("dotcom_username", "");
        jetPackValues.put("dotcom_password", "");
        db.update(BLOGS_TABLE, jetPackValues, null, null);

        // Lastly we'll remove the preference that previously stored the WP.com password
        if (this.context != null) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this.context);
            SharedPreferences.Editor editor = settings.edit();
            editor.remove("wp_pref_wpcom_password");
            editor.apply();
        }
    }

    public List<Map<String, Object>> getAllBlogs() {
        return getBlogsBy(null, null);
    }

    public int setAllDotComBlogsVisibility(boolean visible) {
        ContentValues values = new ContentValues();
        values.put("isHidden", !visible);
        return db.update(BLOGS_TABLE, values, "dotcomFlag=1", null);
    }

    public int setDotComBlogsVisibility(int id, boolean visible) {
        ContentValues values = new ContentValues();
        values.put("isHidden", !visible);
        return db.update(BLOGS_TABLE, values, "dotcomFlag=1 AND id=" + id, null);
    }

    public boolean isDotComBlogVisible(int blogId) {
        String[] args = {Integer.toString(blogId)};
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + BLOGS_TABLE +
                " WHERE isHidden = 0 AND blogId=?", args);
    }

    public boolean isBlogInDatabase(int blogId, String xmlRpcUrl) {
        Cursor c = db.query(BLOGS_TABLE, new String[]{"id"}, "blogId=? AND url=?",
                new String[]{Integer.toString(blogId), xmlRpcUrl}, null, null, null, null);
        boolean result =  c.getCount() > 0;
        c.close();
        return result;
    }

    public boolean isLocalBlogIdInDatabase(int localBlogId) {
        String[] args = {Integer.toString(localBlogId)};
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + BLOGS_TABLE + " WHERE id=?", args);
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
        values.put("isHidden", blog.isHidden());
        values.put("plan_product_id", blog.getPlanID());
        values.put("plan_product_name_short", blog.getPlanShortName());
        values.put("capabilities", blog.getCapabilities());
        if (blog.getWpVersion() != null) {
            values.put("wpVersion", blog.getWpVersion());
        } else {
            values.putNull("wpVersion");
        }
        boolean returnValue = db.update(BLOGS_TABLE, values, "id=" + blog.getLocalTableBlogId(),
                null) > 0;
        if (blog.isDotcomFlag()) {
            returnValue = updateWPComCredentials(blog.getUsername(), blog.getPassword());
        }

        updateCurrentBlog(blog);

        return (returnValue);
    }

    public boolean updateWPComCredentials(String username, String password) {
        // update the login for wordpress.com blogs
        ContentValues userPass = new ContentValues();
        userPass.put("username", username);
        userPass.put("password", encryptPassword(password));
        return db.update(BLOGS_TABLE, userPass, "username=\""
                + username + "\" AND dotcomFlag=1", null) > 0;
    }

    public boolean deleteBlog(Context ctx, int id) {
        int rowsAffected = db.delete(BLOGS_TABLE, "id=?", new String[]{Integer.toString(id)});
        deleteQuickPressShortcutsForLocalTableBlogId(ctx, id);
        deleteAllPostsForLocalTableBlogId(id);
        PeopleTable.deletePeopleForLocalBlogId(id);
        return (rowsAffected > 0);
    }

    public boolean deleteWordPressComBlogs(Context ctx) {
        List<Map<String, Object>> wordPressComBlogs = getBlogsBy("isHidden = 0 AND dotcomFlag = 1", null);
        for (Map<String, Object> blog : wordPressComBlogs) {
            int localBlogId = MapUtils.getMapInt(blog, "id");
            deleteQuickPressShortcutsForLocalTableBlogId(ctx, localBlogId);
            deleteAllPostsForLocalTableBlogId(localBlogId);
            PeopleTable.deletePeopleForLocalBlogId(localBlogId);
        }

        // H4ck alert: We need to delete the Jetpack sites that were added in the initial
        // WP.com get blogs call. These sites will not have the dotcomFlag set and will
        // have an empty password.
        String args = String.format("dotcomFlag=1 OR (dotcomFlag=0 AND password='%s')", encryptPassword(""));

        // Delete blogs
        int rowsAffected = db.delete(BLOGS_TABLE, args, null);
        return (rowsAffected > 0);
    }

    /**
     * Deletes all the things! Use wisely.
     */
    public void dangerouslyDeleteAllContent() {
        db.delete(BLOGS_TABLE, null, null);
        db.delete(POSTS_TABLE, null, null);
        db.delete(MEDIA_TABLE, null, null);
        db.delete(CATEGORIES_TABLE, null, null);
        db.delete(CommentTable.COMMENTS_TABLE, null, null);
    }

    public boolean hasDotOrgBlogForUsernameAndUrl(String username, String url) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(url)) {
            return false;
        }

        Cursor c = db.query(BLOGS_TABLE, new String[]{"id"}, "username=? AND url=?", new String[]{username, url}, null,
                null, null);
        try {
            return c.getCount() > 0;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public boolean isCurrentUserAdminOfRemoteBlogId(long remoteBlogId) {
        String args[] = {String.valueOf(remoteBlogId)};
        String sql = "SELECT isAdmin FROM " + BLOGS_TABLE + " WHERE blogId=?";
        return SqlUtils.boolForQuery(db, sql, args);
    }

    /**
     * Instantiate a new Blog object from it's local id
     *
     * @param localId local blog id
     * @return a new Blog instance or null if the localId was not found
     */
    public Blog instantiateBlogByLocalId(int localId) {
        String[] fields =
                new String[]{"url", "blogName", "username", "password", "httpuser", "httppassword", "imagePlacement",
                             "centerThumbnail", "fullSizeImage", "maxImageWidth", "maxImageWidthId",
                             "blogId", "dotcomFlag", "dotcom_username", "dotcom_password", "api_key",
                             "api_blogid", "wpVersion", "postFormats", "isScaledImage",
                             "scaledImgWidth", "homeURL", "blog_options", "isAdmin", "isHidden",
                             "plan_product_id", "plan_product_name_short", "capabilities"};
        Cursor c = db.query(BLOGS_TABLE, fields, "id=?", new String[]{Integer.toString(localId)}, null, null, null);

        Blog blog = null;
        if (c.moveToFirst()) {
            if (c.getString(0) != null) {
                blog = new Blog();
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
                blog.setRemoteBlogId(c.getInt(c.getColumnIndex("blogId")));
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
                blog.setScaledImage(c.getInt(c.getColumnIndex("isScaledImage")) > 0);
                blog.setScaledImageWidth(c.getInt(c.getColumnIndex("scaledImgWidth")));
                blog.setHomeURL(c.getString(c.getColumnIndex("homeURL")));
                if (c.getString(c.getColumnIndex("blog_options")) == null) {
                    blog.setBlogOptions("{}");
                } else {
                    blog.setBlogOptions(c.getString(c.getColumnIndex("blog_options")));
                }
                blog.setAdmin(c.getInt(c.getColumnIndex("isAdmin")) > 0);
                blog.setHidden(c.getInt(c.getColumnIndex("isHidden")) > 0);
                blog.setPlanID(c.getLong(c.getColumnIndex("plan_product_id")));
                blog.setPlanShortName(c.getString(c.getColumnIndex("plan_product_name_short")));
                blog.setCapabilities(c.getString(c.getColumnIndex("capabilities")));
            }
        }
        c.close();
        return blog;
    }

    /*
     * returns true if the passed blog is wp.com or jetpack-enabled (ie: returns false for
     * self-hosted blogs that don't use jetpack)
     */
    public boolean isRemoteBlogIdDotComOrJetpack(int remoteBlogId) {
        int localId = getLocalTableBlogIdForRemoteBlogId(remoteBlogId);
        Blog blog = instantiateBlogByLocalId(localId);
        return blog != null && (blog.isDotcomFlag() || blog.isJetpackPowered());
    }

    public Blog getBlogForDotComBlogId(String dotComBlogId) {
        Cursor c = db.query(BLOGS_TABLE, new String[]{"id"}, "api_blogid=? OR (blogId=? AND dotcomFlag=1)",
                new String[]{dotComBlogId, dotComBlogId}, null, null, null);
        Blog blog = null;
        if (c.moveToFirst()) {
            blog = instantiateBlogByLocalId(c.getInt(0));
        }
        c.close();
        return blog;
    }

    public List<String> loadStatsLogin(int id) {
        Cursor c = db.query(BLOGS_TABLE, new String[]{"dotcom_username",
                "dotcom_password"}, "id=" + id, null, null, null, null);

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

    /*
     * Jetpack blogs have the "wpcom" blog_id stored in options->api_blogid. This is because self-hosted blogs have both
     * a blogID (local to their network), and a unique blogID on wpcom.
     */
    public int getLocalTableBlogIdForJetpackRemoteID(int remoteBlogId, String xmlRpcUrl) {
        if (TextUtils.isEmpty(xmlRpcUrl)) {
            String sql = "SELECT id FROM " + BLOGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=?";
            String[] args = {Integer.toString(remoteBlogId)};
            return SqlUtils.intForQuery(db, sql, args);
        } else {
            String sql = "SELECT id FROM " + BLOGS_TABLE + " WHERE dotcomFlag=0 AND api_blogid=? AND url=?";
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
            List<Map<String,Object>> allBlogs = this.getBlogsBy("dotcomFlag=0", new String[]{"api_blogid"});
            for (Map<String, Object> currentBlog : allBlogs) {
                if (MapUtils.getMapInt(currentBlog, "id")==localBlogId) {
                    remoteBlogID = MapUtils.getMapInt(currentBlog, "api_blogid");
                    break;
                }
            }
        }
        return remoteBlogID;
    }

    public long getPlanIdForLocalTableBlogId(int localBlogId) {
        return SqlUtils.longForQuery(db,
                "SELECT plan_product_id FROM accounts WHERE id=?",
                new String[]{Integer.toString(localBlogId)});
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
        AppPrefs.resetLastActivityStr();
    }

    /**
     * Get the ID of the most recently active blog. -1 is returned if there is no recently active
     * blog.
     */
    public int getLastBlogId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt("last_blog_id", -1);
    }

    public boolean deletePost(Post post) {
        int result = db.delete(POSTS_TABLE,
                "blogID=? AND id=?",
                new String[]{String.valueOf(post.getLocalTableBlogId()), String.valueOf(post.getLocalTablePostId())});

        return (result == 1);
    }

    // Deletes all posts for the given blogId
    public void deleteAllPostsForLocalTableBlogId(int localBlogId) {
        db.delete(POSTS_TABLE, "blogID=?", new String[]{String.valueOf(localBlogId)});
    }

    public Object[] arrayListToArray(Object array) {
        if (array instanceof ArrayList) {
            return ((ArrayList) array).toArray();
        }
        return (Object[]) array;
    }

    /*
     * returns true if the post matching the passed local blog ID and remote post ID
     * has local changes
     */
    private boolean postHasLocalChanges(int localBlogId, String remotePostId) {
        if (TextUtils.isEmpty(remotePostId)) {
            return false;
        }
        String[] args = {String.valueOf(localBlogId), remotePostId};
        String sql = "SELECT 1 FROM " + POSTS_TABLE + " WHERE blogID=? AND postid=? AND isLocalChange=1";
        return SqlUtils.boolForQuery(db, sql, args);
    }

    /**
     * Saves a list of posts to the db
     * @param postsList: list of post objects
     * @param localBlogId: the posts table blog id
     * @param isPage: boolean to save as pages
     * @param overwriteLocalChanges boolean which determines whether to overwrite posts with local changes
     */
    public void savePosts(List<?> postsList, int localBlogId, boolean isPage, boolean overwriteLocalChanges) {
        if (postsList != null && postsList.size() != 0) {
            db.beginTransaction();
            try {
                for (Object post : postsList) {
                    ContentValues values = new ContentValues();

                    // Sanity checks
                    if (!(post instanceof Map)) {
                        continue;
                    }
                    Map<?, ?> postMap = (Map<?, ?>) post;
                    String postID = MapUtils.getMapStr(postMap, (isPage) ? "page_id" : "postid");
                    if (TextUtils.isEmpty(postID)) {
                        // If we don't have a post or page ID, move on
                        continue;
                    }

                    values.put("blogID", localBlogId);
                    values.put("postid", postID);
                    values.put("title", MapUtils.getMapStr(postMap, "title"));
                    Date dateCreated = MapUtils.getMapDate(postMap, "dateCreated");
                    if (dateCreated != null) {
                        values.put("dateCreated", dateCreated.getTime());
                    } else {
                        Date now = new Date();
                        values.put("dateCreated", now.getTime());
                    }

                    Date dateCreatedGmt = MapUtils.getMapDate(postMap, "date_created_gmt");
                    if (dateCreatedGmt != null) {
                        values.put("date_created_gmt", dateCreatedGmt.getTime());
                    } else {
                        dateCreatedGmt = new Date((Long) values.get("dateCreated"));
                        values.put("date_created_gmt", dateCreatedGmt.getTime() + (dateCreatedGmt.getTimezoneOffset() * 60000));
                    }

                    values.put("description", MapUtils.getMapStr(postMap, "description"));
                    values.put("link", MapUtils.getMapStr(postMap, "link"));
                    values.put("permaLink", MapUtils.getMapStr(postMap, "permaLink"));

                    Object[] postCategories = (Object[]) postMap.get("categories");
                    JSONArray jsonCategoriesArray = new JSONArray();
                    if (postCategories != null) {
                        for (Object postCategory : postCategories) {
                            jsonCategoriesArray.put(postCategory.toString());
                        }
                    }
                    values.put("categories", jsonCategoriesArray.toString());

                    Object[] custom_fields = (Object[]) postMap.get("custom_fields");
                    JSONArray jsonCustomFieldsArray = new JSONArray();
                    if (custom_fields != null) {
                        for (Object custom_field : custom_fields) {
                            jsonCustomFieldsArray.put(custom_field.toString());
                            // Update geo_long and geo_lat from custom fields
                            if (!(custom_field instanceof Map))
                                continue;
                            Map<?, ?> customField = (Map<?, ?>) custom_field;
                            if (customField.get("key") != null && customField.get("value") != null) {
                                if (customField.get("key").equals("geo_longitude"))
                                    values.put("longitude", customField.get("value").toString());
                                if (customField.get("key").equals("geo_latitude"))
                                    values.put("latitude", customField.get("value").toString());
                            }
                        }
                    }
                    values.put("custom_fields", jsonCustomFieldsArray.toString());

                    values.put("mt_excerpt", MapUtils.getMapStr(postMap, (isPage) ? "excerpt" : "mt_excerpt"));
                    values.put("mt_text_more", MapUtils.getMapStr(postMap, (isPage) ? "text_more" : "mt_text_more"));
                    values.put("mt_allow_comments", MapUtils.getMapInt(postMap, "mt_allow_comments", 0));
                    values.put("mt_allow_pings", MapUtils.getMapInt(postMap, "mt_allow_pings", 0));
                    values.put("wp_slug", MapUtils.getMapStr(postMap, "wp_slug"));
                    values.put("wp_password", MapUtils.getMapStr(postMap, "wp_password"));
                    values.put("wp_author_id", MapUtils.getMapStr(postMap, "wp_author_id"));
                    values.put("wp_author_display_name", MapUtils.getMapStr(postMap, "wp_author_display_name"));
                    values.put("wp_post_thumbnail", MapUtils.getMapInt(postMap, "wp_post_thumbnail"));
                    values.put("post_status", MapUtils.getMapStr(postMap, (isPage) ? "page_status" : "post_status"));
                    values.put("userid", MapUtils.getMapStr(postMap, "userid"));

                    if (isPage) {
                        values.put("isPage", true);
                        values.put("wp_page_parent_id", MapUtils.getMapStr(postMap, "wp_page_parent_id"));
                        values.put("wp_page_parent_title", MapUtils.getMapStr(postMap, "wp_page_parent_title"));
                    } else {
                        values.put("mt_keywords", MapUtils.getMapStr(postMap, "mt_keywords"));
                        values.put("wp_post_format", MapUtils.getMapStr(postMap, "wp_post_format"));
                    }

                    if (overwriteLocalChanges) {
                        values.put("isLocalChange", false);
                    }

                    String whereClause = "blogID=? AND postID=? AND isPage=?";
                    if (!overwriteLocalChanges) {
                        whereClause += " AND NOT isLocalChange=1";
                    }

                    String[] args = {String.valueOf(localBlogId), postID, String.valueOf(SqlUtils.boolToSql(isPage))};
                    int updateResult = db.update(POSTS_TABLE, values, whereClause, args);

                    // only perform insert if update didn't match any rows, and only then if we're
                    // overwriting local changes or local changes for this post don't exist
                    if (updateResult == 0 && (overwriteLocalChanges || !postHasLocalChanges(localBlogId, postID))) {
                        db.insert(POSTS_TABLE, null, values);
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /*
     * returns list of posts for use in the post list fragment
     */
    public PostsListPostList getPostsListPosts(int localBlogId, boolean loadPages) {
        PostsListPostList listPosts = new PostsListPostList();

        String[] args = {Integer.toString(localBlogId), Integer.toString(loadPages ? 1 : 0)};
        Cursor c = db.query(POSTS_TABLE, null, "blogID=? AND isPage=?", args, null, null, "localDraft DESC, date_created_gmt DESC");
        try {
            while (c.moveToNext()) {
                listPosts.add(new PostsListPost(getPostFromCursor(c)));
            }
            return listPosts;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private Post getPostFromCursor(Cursor c) {
        Post post = new Post();

        post.setLocalTableBlogId(c.getInt(c.getColumnIndex("blogID")));
        post.setLocalTablePostId(c.getLong(c.getColumnIndex("id")));
        post.setRemotePostId(c.getString(c.getColumnIndex("postid")));
        post.setTitle(StringUtils.unescapeHTML(c.getString(c.getColumnIndex("title"))));
        post.setDateCreated(c.getLong(c.getColumnIndex("dateCreated")));
        post.setDate_created_gmt(c.getLong(c.getColumnIndex("date_created_gmt")));
        post.setCategories(c.getString(c.getColumnIndex("categories")));
        post.setCustomFields(c.getString(c.getColumnIndex("custom_fields")));
        post.setDescription(c.getString(c.getColumnIndex("description")));
        post.setLink(c.getString(c.getColumnIndex("link")));
        post.setAllowComments(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("mt_allow_comments"))));
        post.setAllowPings(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("mt_allow_pings"))));
        post.setPostExcerpt(c.getString(c.getColumnIndex("mt_excerpt")));
        post.setKeywords(c.getString(c.getColumnIndex("mt_keywords")));
        post.setMoreText(c.getString(c.getColumnIndex("mt_text_more")));
        post.setPermaLink(c.getString(c.getColumnIndex("permaLink")));
        post.setPostStatus(c.getString(c.getColumnIndex("post_status")));
        post.setUserId(c.getString(c.getColumnIndex("userid")));
        post.setAuthorDisplayName(c.getString(c.getColumnIndex("wp_author_display_name")));
        post.setAuthorId(c.getString(c.getColumnIndex("wp_author_id")));
        post.setPassword(c.getString(c.getColumnIndex("wp_password")));
        post.setPostFormat(c.getString(c.getColumnIndex("wp_post_format")));
        post.setSlug(c.getString(c.getColumnIndex("wp_slug")));
        post.setMediaPaths(c.getString(c.getColumnIndex("mediaPaths")));
        post.setFeaturedImageId(c.getInt(c.getColumnIndex("wp_post_thumbnail")));

        int latColumnIndex = c.getColumnIndex("latitude");
        int lngColumnIndex = c.getColumnIndex("longitude");
        if (!c.isNull(latColumnIndex) && !c.isNull(lngColumnIndex)) {
            post.setLocation(c.getDouble(latColumnIndex), c.getDouble(lngColumnIndex));
        }

        post.setLocalDraft(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("localDraft"))));
        post.setIsPage(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isPage"))));
        post.setPageParentId(c.getString(c.getColumnIndex("wp_page_parent_id")));
        post.setPageParentTitle(c.getString(c.getColumnIndex("wp_page_parent_title")));
        post.setLocalChange(SqlUtils.sqlToBool(c.getInt(c.getColumnIndex("isLocalChange"))));

        return post;
    }

    public long savePost(Post post) {
        long result = -1;
        if (post != null) {
            ContentValues values = new ContentValues();
            values.put("blogID", post.getLocalTableBlogId());
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMoreText());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mt_keywords", post.getKeywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            putPostLocation(post, values);
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getPostExcerpt());
            values.put("wp_post_thumbnail", post.getFeaturedImageId());

            result = db.insert(POSTS_TABLE, null, values);

            if (result >= 0 && post.isLocalDraft()) {
                post.setLocalTablePostId(result);
            }
        }

        return (result);
    }

    public int updatePost(Post post) {
        int result = 0;
        if (post != null) {
            ContentValues values = new ContentValues();
            values.put("title", post.getTitle());
            values.put("date_created_gmt", post.getDate_created_gmt());
            values.put("description", post.getDescription());
            values.put("mt_text_more", post.getMoreText());
            values.put("postid", post.getRemotePostId());

            JSONArray categoriesJsonArray = post.getJSONCategories();
            if (categoriesJsonArray != null) {
                values.put("categories", categoriesJsonArray.toString());
            }

            values.put("localDraft", post.isLocalDraft());
            values.put("mediaPaths", post.getMediaPaths());
            values.put("mt_keywords", post.getKeywords());
            values.put("wp_password", post.getPassword());
            values.put("post_status", post.getPostStatus());
            values.put("isPage", post.isPage());
            values.put("wp_post_format", post.getPostFormat());
            values.put("isLocalChange", post.isLocalChange());
            values.put("mt_excerpt", post.getPostExcerpt());
            values.put("wp_post_thumbnail", post.getFeaturedImageId());

            putPostLocation(post, values);

            result = db.update(POSTS_TABLE, values, "blogID=? AND id=? AND isPage=?",
                    new String[]{
                            String.valueOf(post.getLocalTableBlogId()),
                            String.valueOf(post.getLocalTablePostId()),
                            String.valueOf(SqlUtils.boolToSql(post.isPage()))
                    });
        }

        return (result);
    }

    private void putPostLocation(Post post, ContentValues values) {
        if (post.hasLocation()) {
            PostLocation location = post.getLocation();
            values.put("latitude", location.getLatitude());
            values.put("longitude", location.getLongitude());
        } else {
            values.putNull("latitude");
            values.putNull("longitude");
        }
    }

    /*
     * removes all posts/pages in the passed blog that don't have local changes
     */
    public void deleteUploadedPosts(int blogID, boolean isPage) {
        String[] args = {String.valueOf(blogID), isPage ? "1" : "0"};
        db.delete(POSTS_TABLE, "blogID=? AND isPage=? AND localDraft=0 AND isLocalChange=0", args);
    }

    public Post getPostForLocalTablePostId(long localTablePostId) {
        Cursor c = db.query(POSTS_TABLE, null, "id=?", new String[]{String.valueOf(localTablePostId)}, null, null, null);
        try {
            if (c.moveToFirst()) {
                return getPostFromCursor(c);
            } else {
                return null;
            }
        } finally {
            SqlUtils.closeCursor(c);
        }
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
                "category_name" }, "blog_id=" + Integer.toString(id), null, null, null, null);
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

    public boolean addQuickPressShortcut(int blogId, String name) {
        ContentValues values = new ContentValues();
        values.put("accountId", blogId);
        values.put("name", name);
        boolean returnValue = false;
        synchronized (this) {
            returnValue = db.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    /*
     * return all QuickPress shortcuts connected with the passed blog
     *
     */
    public List<Map<String, Object>> getQuickPressShortcuts(int blogId) {
        Cursor c = db.query(QUICKPRESS_SHORTCUTS_TABLE, new String[]{"id",
                        "accountId", "name"}, "accountId = " + blogId, null, null,
                null, null);
        String id, name;
        int numRows = c.getCount();
        c.moveToFirst();
        List<Map<String, Object>> blogs = new Vector<Map<String, Object>>();
        for (int i = 0; i < numRows; i++) {
            id = c.getString(0);
            name = c.getString(2);
            if (id != null) {
                Map<String, Object> thisHash = new HashMap<String, Object>();

                thisHash.put("id", id);
                thisHash.put("name", name);
                blogs.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();

        return blogs;
    }

    /*
     * delete QuickPress home screen shortcuts connected with the passed blog
     */
    private void deleteQuickPressShortcutsForLocalTableBlogId(Context ctx, int blogId) {
        List<Map<String, Object>> shortcuts = getQuickPressShortcuts(blogId);
        if (shortcuts.size() == 0)
            return;

        for (int i = 0; i < shortcuts.size(); i++) {
            Map<String, Object> shortcutHash = shortcuts.get(i);

            Intent shortcutIntent = new Intent(WordPress.getContext(), EditPostActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

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
        Cursor c = db.query(BLOGS_TABLE, new String[] { "id", "password",
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

            db.update(BLOGS_TABLE, values, "id=" + c.getInt(0), null);

            c.moveToNext();
        }
        c.close();
    }

    public int getUnmoderatedCommentCount(int blogID) {
        int commentCount = 0;

        Cursor c = db
                .rawQuery(
                        "select count(*) from comments where blogID=? AND status='hold'",
                        new String[]{String.valueOf(blogID)});
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
        values.put(COLUMN_NAME_POST_ID, mf.getPostID());
        values.put(COLUMN_NAME_FILE_PATH, mf.getFilePath());
        values.put(COLUMN_NAME_FILE_NAME, mf.getFileName());
        values.put(COLUMN_NAME_TITLE, mf.getTitle());
        values.put(COLUMN_NAME_DESCRIPTION, mf.getDescription());
        values.put(COLUMN_NAME_CAPTION, mf.getCaption());
        values.put(COLUMN_NAME_HORIZONTAL_ALIGNMENT, mf.getHorizontalAlignment());
        values.put(COLUMN_NAME_WIDTH, mf.getWidth());
        values.put(COLUMN_NAME_HEIGHT, mf.getHeight());
        values.put(COLUMN_NAME_MIME_TYPE, mf.getMimeType());
        values.put(COLUMN_NAME_FEATURED, mf.isFeatured());
        values.put(COLUMN_NAME_IS_VIDEO, mf.isVideo());
        values.put(COLUMN_NAME_IS_FEATURED_IN_POST, mf.isFeaturedInPost());
        values.put(COLUMN_NAME_FILE_URL, mf.getFileURL());
        values.put(COLUMN_NAME_THUMBNAIL_URL, mf.getThumbnailURL());
        values.put(COLUMN_NAME_MEDIA_ID, mf.getMediaId());
        values.put(COLUMN_NAME_BLOG_ID, mf.getBlogId());
        values.put(COLUMN_NAME_DATE_CREATED_GMT, mf.getDateCreatedGMT());
        values.put(COLUMN_NAME_VIDEO_PRESS_SHORTCODE, mf.getVideoPressShortCode());
        if (mf.getUploadState() != null)
            values.put(COLUMN_NAME_UPLOAD_STATE, mf.getUploadState());
        else
            values.putNull(COLUMN_NAME_UPLOAD_STATE);

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

        String term = searchTerm.toLowerCase(LanguageUtils.getCurrentDeviceLanguage(WordPress.getContext()));
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND title LIKE ? AND (uploadState IS NULL OR uploadState ='uploaded') ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[]{blogId, "%" + term + "%", "uploading"});
    }

    /** For a given blogId, get the media file with the given media_id **/
    public Cursor getMediaFile(String blogId, String mediaId) {
        return db.rawQuery("SELECT * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?", new String[]{blogId, mediaId});
    }


    /**
     * Given a VideoPress id, returns the corresponding remote video URL stored in the DB
     */
    public String getMediaUrlByVideoPressId(String blogId, String videoId) {
        if (TextUtils.isEmpty(blogId) || TextUtils.isEmpty(videoId)) {
            return "";
        }

        String shortcode = ShortcodeUtils.getVideoPressShortcodeFromId(videoId);

        String query = "SELECT " + COLUMN_NAME_FILE_URL + " FROM " + MEDIA_TABLE + " WHERE blogId=? AND videoPressShortcode=?";
        return SqlUtils.stringForQuery(db, query, new String[]{blogId, shortcode});
    }

    public String getMediaThumbnailUrl(int blogId, long mediaId) {
        String query = "SELECT " + COLUMN_NAME_THUMBNAIL_URL + " FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?";
        return SqlUtils.stringForQuery(db, query, new String[]{Integer.toString(blogId), Long.toString(mediaId)});
    }

    public int getMediaCountAll(String blogId) {
        Cursor cursor = getMediaFilesForBlog(blogId);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public boolean mediaFileExists(String blogId, String mediaId) {
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?",
                new String[]{blogId, mediaId});
    }

    public Cursor getMediaImagesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND mimeType LIKE ? ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[]{blogId, "image%", "uploading"});
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
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND mimeType LIKE ? " + mediaIdsStr + " ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[]{blogId, "image%", "uploading"});
    }

    public int getMediaCountImages(String blogId) {
        return getMediaImagesForBlog(blogId).getCount();
    }

    public Cursor getMediaUnattachedForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND " +
                "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND postId=0 ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[]{blogId, "uploading"});
    }

    public int getMediaCountUnattached(String blogId) {
        return getMediaUnattachedForBlog(blogId).getCount();
    }

    public Cursor getMediaFilesForBlog(String blogId, long startDate, long endDate) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND (uploadState IS NULL OR uploadState ='uploaded') AND (date_created_gmt >= ? AND date_created_gmt <= ?) ", new String[]{blogId, String.valueOf(startDate), String.valueOf(endDate)});
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
        Cursor c = db.query(MEDIA_TABLE, null, "postID=? AND filePath=?",
                new String[]{String.valueOf(post.getLocalTablePostId()), src}, null, null, null);

        try {
            if (c.moveToFirst()) {
                MediaFile mf = new MediaFile();
                mf.setId(c.getInt(0));
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

                return mf;
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }

    public void deleteMediaFilesForPost(Post post) {
        db.delete(MEDIA_TABLE, "blogId='" + post.getLocalTableBlogId() + "' AND postID=" + post.getLocalTablePostId(), null);
    }

    /** Get the queued media files for upload for a given blogId **/
    public Cursor getMediaUploadQueue(String blogId) {
        return db.rawQuery("SELECT * FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=?", new String[] {"queued", blogId});
    }

    /** Update a media file to a new upload state **/
    public void updateMediaUploadState(String blogId, String mediaId, MediaUploadState uploadState) {
        if (blogId == null || blogId.equals("")) {
            return;
        }

        ContentValues values = new ContentValues();
        if (uploadState == null) {
            values.putNull("uploadState");
        } else {
            values.put("uploadState", uploadState.toString());
        }

        if (mediaId == null) {
            db.update(MEDIA_TABLE, values, "blogId=? AND (uploadState IS NULL OR uploadState ='uploaded')",
                    new String[]{blogId});
        } else {
            db.update(MEDIA_TABLE, values, "blogId=? AND mediaId=?", new String[]{blogId, mediaId});
            EventBus.getDefault().post(new MediaChanged(blogId, mediaId));
        }
    }

    public void updateMediaLocalToRemoteId(String blogId, String localMediaId, String remoteMediaId) {
        ContentValues values = new ContentValues();
        values.put("mediaId", remoteMediaId);
        db.update(MEDIA_TABLE, values, "blogId=? AND mediaId=?", new String[]{blogId, localMediaId});
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
        db.update(MEDIA_TABLE, values, "blogId=? AND uploadState=?", new String[]{blogId, "uploading"});
    }

    /** For a given blogId, clear the upload states in the upload queue **/
    public void clearMediaUploaded(String blogId) {
        if (blogId == null || blogId.equals(""))
            return;

        ContentValues values = new ContentValues();
        values.putNull("uploadState");
        db.update(MEDIA_TABLE, values, "blogId=? AND uploadState=?", new String[]{blogId, "uploaded"});
    }

    /** Delete a media item from a blog locally **/
    public void deleteMediaFile(String blogId, String mediaId) {
        db.delete(MEDIA_TABLE, "blogId=? AND mediaId=?", new String[]{blogId, mediaId});
    }

    /** Mark media files for deletion without actually deleting them. **/
    public void setMediaFilesMarkedForDelete(String blogId, Set<String> ids) {
        // This is for queueing up files to delete on the server
        for (String id : ids) {
            updateMediaUploadState(blogId, id, MediaUploadState.DELETE);
        }
    }

    /** Mark media files as deleted without actually deleting them **/
    public void setMediaFilesMarkedForDeleted(String blogId) {
        // This is for syncing our files to the server:
        // when we pull from the server, everything that is still 'deleted'
        // was not downloaded from the server and can be removed via deleteFilesMarkedForDeleted()
        updateMediaUploadState(blogId, null, MediaUploadState.DELETED);
    }

    /** Delete files marked as deleted **/
    public void deleteFilesMarkedForDeleted(String blogId) {
        db.delete(MEDIA_TABLE, "blogId=? AND uploadState=?", new String[]{blogId, "deleted"});
    }

    /** Get a media file scheduled for delete for a given blogId **/
    public Cursor getMediaDeleteQueueItem(String blogId) {
        return db.rawQuery("SELECT blogId, mediaId FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=? LIMIT 1",
                new String[]{"delete", blogId});
    }

    /** Get all media files scheduled for delete for a given blogId **/
    public Cursor getMediaDeleteQueueItems(String blogId) {
        return db.rawQuery("SELECT blogId, mediaId FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=?",
                new String[]{"delete", blogId});
    }

    public boolean hasMediaDeleteQueueItems(int blogId) {
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + MEDIA_TABLE + " WHERE uploadState=? AND blogId=?",
                new String[]{"delete", Integer.toString(blogId)});
    }

    public int getWPCOMBlogID() {
        int id = -1;
        Cursor c = db.query(BLOGS_TABLE, new String[] { "id" },
                "dotcomFlag=1", null, null, null, null);
        int numRows = c.getCount();
        c.moveToFirst();
        if (numRows > 0) {
            id = c.getInt(0);
        }

        c.close();

        return id;
    }

    /*
     * returns true if any posts in the passed blog have changes which haven't been uploaded yet
     */
    public boolean blogHasLocalChanges(int localBlogId, boolean isPage) {
        String sql = "SELECT 1 FROM " + POSTS_TABLE + " WHERE isLocalChange=1 AND blogID=? AND isPage=?";
        String[] args = {String.valueOf(localBlogId), isPage ? "1" : "0"};
        return SqlUtils.boolForQuery(db, sql, args);
    }

    /*
     * returns the number of posts/pages in the passed blog that aren't local drafts
     */
    public int getUploadedCountInBlog(int localBlogId, boolean isPage) {
        String sql = "SELECT COUNT(*) FROM " + POSTS_TABLE + " WHERE blogID=? AND isPage=? AND localDraft=0";
        String[] args = {String.valueOf(localBlogId), isPage ? "1" : "0"};
        return SqlUtils.intForQuery(db, sql, args);
    }

    public boolean saveTheme(Theme theme) {
        boolean returnValue = false;

        ContentValues values = new ContentValues();
        values.put(Theme.ID, theme.getId());
        values.put(Theme.AUTHOR, theme.getAuthor());
        values.put(Theme.SCREENSHOT, theme.getScreenshot());
        values.put(Theme.AUTHOR_URI, theme.getAuthorURI());
        values.put(Theme.DEMO_URI, theme.getDemoURI());
        values.put(Theme.NAME, theme.getName());
        values.put(Theme.STYLESHEET, theme.getStylesheet());
        values.put(Theme.PRICE, theme.getPrice());
        values.put(Theme.BLOG_ID, theme.getBlogId());
        values.put(Theme.IS_CURRENT, theme.getIsCurrent() ? 1 : 0);

        synchronized (this) {
            int result = db.update(
                    THEMES_TABLE,
                    values,
                    Theme.ID + "=?",
                    new String[]{theme.getId()});
            if (result == 0)
                returnValue = db.insert(THEMES_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public Cursor getThemesAll(String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=?", selection, null, null, null);
    }

    public Cursor getThemesFree(String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, ""};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.PRICE + "=?", selection, null, null, null);
    }

    public Cursor getThemesPremium(String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, ""};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.PRICE + "!=?", selection, null, null, null);
    }

    public String getCurrentThemeId(String blogId) {
        String[] selection = {blogId, String.valueOf(1)};
        String currentThemeId;
        try {
            currentThemeId = DatabaseUtils.stringForQuery(db, "SELECT " + Theme.ID + " FROM " + THEMES_TABLE + " WHERE " + Theme.BLOG_ID + "=? and " + Theme.IS_CURRENT + "=?", selection);
        } catch (SQLiteException e) {
            currentThemeId = "";
        }

        return currentThemeId;
    }

    public void setCurrentTheme(String blogId, String id) {
        // update any old themes that are set to true to false
        ContentValues values = new ContentValues();
        values.put(Theme.IS_CURRENT, false);
        db.update(THEMES_TABLE, values, Theme.BLOG_ID + "=?", new String[] { blogId });

        values = new ContentValues();
        values.put(Theme.IS_CURRENT, true);
        db.update(THEMES_TABLE, values, Theme.BLOG_ID + "=? AND " + Theme.ID + "=?", new String[] { blogId, id });
    }

    public int getThemeCount(String blogId) {
        return getThemesAll(blogId).getCount();
    }

    public Cursor getThemes(String blogId, String searchTerm) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, "%" + searchTerm + "%"};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.NAME + " LIKE ?", selection, null, null, null);
    }

    public Theme getTheme(String blogId, String themeId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.AUTHOR, Theme.SCREENSHOT, Theme.AUTHOR_URI, Theme.DEMO_URI, Theme.NAME, Theme.STYLESHEET, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, themeId};
        Cursor cursor = db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.ID + "=?", selection, null, null, null);

        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(Theme.ID));
            String author = cursor.getString(cursor.getColumnIndex(Theme.AUTHOR));
            String screenshot = cursor.getString(cursor.getColumnIndex(Theme.SCREENSHOT));
            String authorURI = cursor.getString(cursor.getColumnIndex(Theme.AUTHOR_URI));
            String demoURI = cursor.getString(cursor.getColumnIndex(Theme.DEMO_URI));
            String name = cursor.getString(cursor.getColumnIndex(Theme.NAME));
            String stylesheet = cursor.getString(cursor.getColumnIndex(Theme.STYLESHEET));
            String price = cursor.getString(cursor.getColumnIndex(Theme.PRICE));
            boolean isCurrent = cursor.getInt(cursor.getColumnIndex(Theme.IS_CURRENT)) > 0;

            Theme theme = new Theme(id, author, screenshot, authorURI, demoURI, name, stylesheet, price, blogId, isCurrent);
            cursor.close();

            return theme;
        } else {
            cursor.close();
            return null;
        }
    }

    public Theme getCurrentTheme(String blogId) {
        String currentThemeId = getCurrentThemeId(blogId);

        return getTheme(blogId, currentThemeId);
    }

    /*
     * used during development to copy database to SD card so we can access it via DDMS
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

    public boolean hasAnyJetpackBlogs() {
        return SqlUtils.boolForQuery(db, "SELECT 1 FROM " + BLOGS_TABLE + " WHERE api_blogid != 0 LIMIT 1", null);
    }

    private void updateCurrentBlog(Blog blog) {
        Blog currentBlog = WordPress.currentBlog;
        if (currentBlog != null && blog.getLocalTableBlogId() == currentBlog.getLocalTableBlogId()) {
            WordPress.currentBlog = blog;
        }
    }
}
