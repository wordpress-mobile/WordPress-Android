package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.SuggestionTable;
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
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    private static final int DATABASE_VERSION = 49;

    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";

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


    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

    //add boolean to posts to check uploaded posts that have local changes
    private static final String ADD_LOCAL_POST_CHANGES = "alter table posts add isLocalChange boolean default 0";

    // add wp_post_thumbnail to posts table
    private static final String ADD_POST_THUMBNAIL = "alter table posts add wp_post_thumbnail integer default 0;";

    // add postid and blogID indexes to posts table
    private static final String ADD_POST_ID_INDEX = "CREATE INDEX idx_posts_post_id ON posts(postid);";
    private static final String ADD_BLOG_ID_INDEX = "CREATE INDEX idx_posts_blog_id ON posts(blogID);";

    //add boolean to track if featured image should be included in the post content
    private static final String ADD_FEATURED_IN_POST = "alter table media add isFeaturedInPost boolean default false;";

    // add category parent id to keep track of category hierarchy
    private static final String ADD_PARENTID_IN_CATEGORIES = "alter table cats add parent_id integer default 0;";

    // add thumbnailURL, thumbnailPath and fileURL to media
    private static final String ADD_MEDIA_THUMBNAIL_URL = "alter table media add thumbnailURL text default '';";
    private static final String ADD_MEDIA_FILE_URL = "alter table media add fileURL text default '';";
    private static final String ADD_MEDIA_UNIQUE_ID = "alter table media add mediaId text default '';";
    private static final String ADD_MEDIA_BLOG_ID = "alter table media add blogId text default '';";
    private static final String ADD_MEDIA_DATE_GMT = "alter table media add date_created_gmt date;";
    private static final String ADD_MEDIA_UPLOAD_STATE = "alter table media add uploadState default '';";
    private static final String ADD_MEDIA_VIDEOPRESS_SHORTCODE = "alter table media add videoPressShortcode text default '';";

    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";

    private SQLiteDatabase db;

    protected static final String PASSWORD_SECRET = BuildConfig.DB_SECRET;
    private Context context;

    public WordPressDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
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

        // TODO: STORES: only migrate auth token and local drafts to wpstores, drop everything else.
        switch (currentVersion) {
            case 0:
                // New install
                currentVersion++;
            case 1:
                // Add columns that were added in very early releases, then move on to version 9
                currentVersion = 9;
            case 9:
                currentVersion++;
            case 10:
                db.delete(POSTS_TABLE, null, null);
                db.execSQL(CREATE_TABLE_POSTS);
                currentVersion++;
            case 11:
                db.execSQL(ADD_LOCAL_POST_CHANGES);
                currentVersion++;
            case 12:
                db.execSQL(ADD_FEATURED_IN_POST);
                currentVersion++;
            case 13:
                currentVersion++;
            case 14:
                currentVersion++;
            case 15:
                // No longer used (preferences migration)
                currentVersion++;
            case 16:
                currentVersion++;
            case 17:
                db.execSQL(ADD_PARENTID_IN_CATEGORIES);
                currentVersion++;
            case 18:
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
                // NOPE: removeDotComCredentials();
                currentVersion++;
            case 29:
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
                // TODO: STORES: kill this - updateDotcomFlag();
                currentVersion++;
            case 39:
                currentVersion++;
            case 40:
                currentVersion++;
            case 41:
                currentVersion++;
            case 42:
                currentVersion++;
            case 43:
                currentVersion++;
            case 44:
                PeopleTable.createTables(db);
                currentVersion++;
            case 45:
                currentVersion++;
            case 46:
                AppPrefs.setVisualEditorAvailable(true);
                AppPrefs.setVisualEditorEnabled(true);
                currentVersion++;
            case 47:
                PeopleTable.reset(db);
                currentVersion++;
            case 48:
                PeopleTable.createViewersTable(db);
                currentVersion++;
        }
        db.setVersion(DATABASE_VERSION);
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

    public SQLiteDatabase getDatabase() {
        return db;
    }

    public static void deleteDatabase(Context ctx) {
        ctx.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Deletes all the things! Use wisely.
     */
    public void dangerouslyDeleteAllContent() {
        db.delete(POSTS_TABLE, null, null);
        db.delete(MEDIA_TABLE, null, null);
        db.delete(CATEGORIES_TABLE, null, null);
        db.delete(CommentTable.COMMENTS_TABLE, null, null);
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

    /** For a given blogId, get all the media files **/
    public Cursor getMediaFilesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[] { blogId, "uploading" });
    }

    /** For a given blogId, get the media file with the given media_id **/
    public Cursor getMediaFile(String blogId, String mediaId) {
        return db.rawQuery("SELECT * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?", new String[]{blogId, mediaId});
    }

    public String getMediaThumbnailUrl(int blogId, long mediaId) {
        String query = "SELECT " + COLUMN_NAME_THUMBNAIL_URL + " FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId=?";
        return SqlUtils.stringForQuery(db, query, new String[]{Integer.toString(blogId), Long.toString(mediaId)});
    }

    public Cursor getMediaImagesForBlog(String blogId) {
        return db.rawQuery("SELECT id as _id, * FROM " + MEDIA_TABLE + " WHERE blogId=? AND mediaId <> '' AND "
                + "(uploadState IS NULL OR uploadState IN ('uploaded', 'queued', 'failed', 'uploading')) AND mimeType LIKE ? ORDER BY (uploadState=?) DESC, date_created_gmt DESC", new String[]{blogId, "image%", "uploading"});
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
}
