package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;

import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.media.services.MediaEvents.MediaChanged;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.ShortcodeUtils;
import org.wordpress.android.util.SqlUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final int DATABASE_VERSION = 50;

    private static final String CREATE_TABLE_MEDIA = "create table if not exists media (id integer primary key autoincrement, "
            + "postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false);";

    // Warning if you rename DATABASE_NAME, that could break previous App backups (see: xml/backup_scheme.xml)
    private static final String DATABASE_NAME = "wordpress";
    private static final String MEDIA_TABLE = "media";
    private static final String NOTES_TABLE = "notes";

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

    private Context context;

    public WordPressDB(Context ctx) {
        this.context = ctx;
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_CATEGORIES);
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        db.execSQL(CREATE_TABLE_MEDIA);
        db.execSQL(CREATE_TABLE_THEMES);
        SiteSettingsTable.createTable(db);
        SuggestionTable.createTables(db);
        NotificationsTable.createTables(db);

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
                currentVersion++;
            case 11:
                currentVersion++;
            case 12:
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
                currentVersion++;
            case 28:
                // Remove WordPress.com credentials
                // NOPE: removeDotComCredentials();
                currentVersion++;
            case 29:
                currentVersion++;
            case 30:
                currentVersion++;
            case 31:
                currentVersion++;
            case 32:
                currentVersion++;
            case 33:
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
            case 49:
                // Delete simperium DB since we're removing Simperium from the app.
                ctx.deleteDatabase("simperium-store");
        }
        db.setVersion(DATABASE_VERSION);
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
        db.delete(MEDIA_TABLE, null, null);
        db.delete(CATEGORIES_TABLE, null, null);
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
        List<Map<String, Object>> blogs = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            id = c.getString(0);
            name = c.getString(2);
            if (id != null) {
                Map<String, Object> thisHash = new HashMap<>();

                thisHash.put("id", id);
                thisHash.put("name", name);
                blogs.add(thisHash);
            }
            c.moveToNext();
        }
        c.close();

        return blogs;
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
