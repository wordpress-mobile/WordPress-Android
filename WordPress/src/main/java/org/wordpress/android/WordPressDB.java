package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.UserSuggestionTable;
import org.wordpress.android.models.SiteSettingsModel;
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
    private static final int DATABASE_VERSION = 67;


    // Warning renaming DATABASE_NAME could break previous App backups (see: xml/backup_scheme.xml)
    private static final String DATABASE_NAME = "wordpress";
    private static final String NOTES_TABLE = "notes";
    private static final String THEMES_TABLE = "themes";

    // add new table for QuickPress homescreen shortcuts
    private static final String CREATE_TABLE_QUICKPRESS_SHORTCUTS =
            "create table if not exists quickpress_shortcuts (id integer primary key autoincrement, "
            + "accountId text, name text);";
    private static final String QUICKPRESS_SHORTCUTS_TABLE = "quickpress_shortcuts";

    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";

    private SQLiteDatabase mDb;

    @SuppressWarnings({"FallThrough"})
    public WordPressDB(Context ctx) {
        mDb = ctx.openOrCreateDatabase(DATABASE_NAME, 0, null);

        // Create tables if they don't exist
        mDb.execSQL(CREATE_TABLE_QUICKPRESS_SHORTCUTS);
        SiteSettingsTable.createTable(mDb);
        UserSuggestionTable.createTables(mDb);
        NotificationsTable.createTables(mDb);

        // Update tables for new installs and app updates
        int currentVersion = mDb.getVersion();
        boolean isNewInstall = (currentVersion == 0);

        if (!isNewInstall && currentVersion != DATABASE_VERSION) {
            AppLog.d(T.DB, "upgrading database from version " + currentVersion + " to " + DATABASE_VERSION);
        }

        switch (currentVersion) {
            case 0:
                // New install
            case 1:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                // No longer used (preferences migration)
            case 16:
            case 17:
            case 18:
            case 19:
                // revision 20: create table "notes"
            case 20:
            case 21:
                // version 23 added CommentTable.java, version 24 changed the comment table schema
            case 22:
            case 23:
            case 24:
            case 25:
                // ver 26 "virtually" remove columns 'lastCommentId' and 'runService' from the DB
                // SQLite supports a limited subset of ALTER TABLE.
                // The ALTER TABLE command in SQLite allows the user to rename a table or to add a new column to
                // an existing table. It is not possible to rename a column, remove a column, or add or remove
                // constraints from a table.
            case 26:
                // Drop the notes table, no longer needed with Simperium.
                mDb.execSQL(DROP_TABLE_PREFIX + NOTES_TABLE);
            case 27:
            case 28:
                // Remove WordPress.com credentials
                // NOPE: removeWPComCredentials();
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                // Delete simperium DB - from 4.6 to 4.6.1
                // Fix an issue when note id > MAX_INT
                ctx.deleteDatabase("simperium-store");
            case 36:
                // Delete simperium DB again - from 4.6.1 to 4.7
                // Fix a sync issue happening for users who have both wpios and wpandroid active clients
                ctx.deleteDatabase("simperium-store");
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
                PeopleTable.createTables(mDb);
            case 45:
            case 46:
            case 47:
                PeopleTable.reset(mDb);
            case 48:
                PeopleTable.createViewersTable(mDb);
            case 49:
                // Delete simperium DB since we're removing Simperium from the app.
                ctx.deleteDatabase("simperium-store");
            case 50:
                // fix #5373 - no op
            case 51:
                // no op - was SiteSettingsTable.addOptimizedImageToSiteSettingsTable(db);
            case 52:
                // no op - was used for old image optimization settings
            case 53:
                // Clean up empty cache files caused by #5417
                clearEmptyCacheFiles(ctx);
            case 54:
                // no op - was used for old image optimization settings
            case 55:
                SiteSettingsTable.addSharingColumnsToSiteSettingsTable(mDb);
            case 56:
                // no op - was used for old video optimization settings
            case 57:
                // Migrate media optimization settings
                SiteSettingsTable.migrateMediaOptimizeSettings(mDb);
            case 58:
                // ThemeStore merged, remove deprecated themes tables
                mDb.execSQL(DROP_TABLE_PREFIX + THEMES_TABLE);
            case 59:
                // Enable Aztec for all users
                AppPrefs.setAztecEditorEnabled(true);
            case 60:
                // add Start of Week site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_START_OF_WEEK);
            case 61:
                // add date & time format site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_TIME_FORMAT);
                mDb.execSQL(SiteSettingsModel.ADD_DATE_FORMAT);
            case 62:
                // add timezone and posts per page site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_TIMEZONE);
                mDb.execSQL(SiteSettingsModel.ADD_POSTS_PER_PAGE);
            case 63:
                // add AMP site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_AMP_SUPPORTED);
                mDb.execSQL(SiteSettingsModel.ADD_AMP_ENABLED);
            case 64:
                // add site icon
                mDb.execSQL(SiteSettingsModel.ADD_SITE_ICON);
            case 65:
                // add external users only to publicize services table
                PublicizeTable.resetServicesTable(mDb);
            case 66:
                // add Jetpack search site setting
                mDb.execSQL(SiteSettingsModel.ADD_JETPACK_SEARCH_SUPPORTED);
                mDb.execSQL(SiteSettingsModel.ADD_JETPACK_SEARCH_ENABLED);
        }
        mDb.setVersion(DATABASE_VERSION);
    }

    public SQLiteDatabase getDatabase() {
        return mDb;
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
            returnValue = mDb.insert(QUICKPRESS_SHORTCUTS_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    /*
     * used during development to copy database to SD card so we can access it via DDMS
     */
    protected void copyDatabase() {
        String copyFrom = mDb.getPath();
        String copyTo =
                WordPress.getContext().getExternalFilesDir(null).getAbsolutePath() + "/" + DATABASE_NAME + ".db";

        try {
            InputStream input = new FileInputStream(copyFrom);
            OutputStream output = new FileOutputStream(copyTo);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

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
