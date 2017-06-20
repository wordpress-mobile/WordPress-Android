package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WordPressDB {
    private static final String COLUMN_NAME_ID = "_id";

    private static final int DATABASE_VERSION = 56;

    // Warning if you rename DATABASE_NAME, that could break previous App backups (see: xml/backup_scheme.xml)
    private static final String DATABASE_NAME = "wordpress";
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

    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS = "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";

    private SQLiteDatabase db;

    public WordPressDB(Context ctx) {
        db = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
        db.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
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
                currentVersion++;
            case 18:
                currentVersion++;
            case 19:
                // revision 20: create table "notes"
                currentVersion++;
            case 20:
                currentVersion++;
            case 21:
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
                currentVersion++;
            case 50:
                // fix #5373 - no op
                currentVersion++;
            case 51:
                SiteSettingsTable.addOptimizedImageToSiteSettingsTable(db);
                currentVersion++;
            case 52:
                // fix #5373 for users who already upgraded to 52 but missed the first migration
                try {
                    SiteSettingsTable.addOptimizedImageToSiteSettingsTable(db);
                } catch(SQLiteException e) {
                    // ignore "duplicate column" exception
                }
                currentVersion++;
            case 53:
                // Clean up empty cache files caused by #5417
                clearEmptyCacheFiles(ctx);
                currentVersion++;
            case 54:
                SiteSettingsTable.addImageResizeWidthAndQualityToSiteSettingsTable(db);
                currentVersion++;
            case 55:
                SiteSettingsTable.addSharingColumnsToSiteSettingsTable(db);
                currentVersion++;
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

    private void clearEmptyCacheFiles(Context context) {
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            File imageCacheDir = new File(android.os.Environment.getExternalStorageDirectory() + "/WordPress/images");
            File videoCacheDir = new File(android.os.Environment.getExternalStorageDirectory() + "/WordPress/video");

            deleteEmptyFilesInDirectory(imageCacheDir);
            deleteEmptyFilesInDirectory(videoCacheDir);
        } else {
            File cacheDir = context.getApplicationContext().getCacheDir();
            deleteEmptyFilesInDirectory(cacheDir);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteEmptyFilesInDirectory(File directory) {
        if (directory == null || !directory.exists() || directory.listFiles() == null) {
            return;
        }

        for (File file : directory.listFiles()) {
            if (file != null && file.length() == 0) {
                file.delete();
            }
        }
    }
}
