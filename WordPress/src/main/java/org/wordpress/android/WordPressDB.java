package org.wordpress.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.datasets.NotificationsTable;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.datasets.SuggestionTable;
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
    private static final int DATABASE_VERSION = 64;


    // Warning if you rename DATABASE_NAME, that could break previous App backups (see: xml/backup_scheme.xml)
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
        SuggestionTable.createTables(mDb);
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
                // ver 26 "virtually" remove columns 'lastCommentId' and 'runService' from the DB
                // SQLite supports a limited subset of ALTER TABLE.
                // The ALTER TABLE command in SQLite allows the user to rename a table or to add a new column to
                // an existing table. It is not possible to rename a column, remove a column, or add or remove
                // constraints from a table.
                currentVersion++;
            case 26:
                // Drop the notes table, no longer needed with Simperium.
                mDb.execSQL(DROP_TABLE_PREFIX + NOTES_TABLE);
                currentVersion++;
            case 27:
                currentVersion++;
            case 28:
                // Remove WordPress.com credentials
                // NOPE: removeWPComCredentials();
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
                PeopleTable.createTables(mDb);
                currentVersion++;
            case 45:
                currentVersion++;
            case 46:
                AppPrefs.setVisualEditorAvailable(true);
                AppPrefs.setVisualEditorEnabled(true);
                currentVersion++;
            case 47:
                PeopleTable.reset(mDb);
                currentVersion++;
            case 48:
                PeopleTable.createViewersTable(mDb);
                currentVersion++;
            case 49:
                // Delete simperium DB since we're removing Simperium from the app.
                ctx.deleteDatabase("simperium-store");
                currentVersion++;
            case 50:
                // fix #5373 - no op
                currentVersion++;
            case 51:
                // no op - was SiteSettingsTable.addOptimizedImageToSiteSettingsTable(db);
                currentVersion++;
            case 52:
                // no op - was used for old image optimization settings
                currentVersion++;
            case 53:
                // Clean up empty cache files caused by #5417
                clearEmptyCacheFiles(ctx);
                currentVersion++;
            case 54:
                // no op - was used for old image optimization settings
                currentVersion++;
            case 55:
                SiteSettingsTable.addSharingColumnsToSiteSettingsTable(mDb);
                currentVersion++;
            case 56:
                // no op - was used for old video optimization settings
                currentVersion++;
            case 57:
                // Migrate media optimization settings
                SiteSettingsTable.migrateMediaOptimizeSettings(mDb);
                currentVersion++;
            case 58:
                // ThemeStore merged, remove deprecated themes tables
                mDb.execSQL(DROP_TABLE_PREFIX + THEMES_TABLE);
                currentVersion++;
            case 59:
                // Enable Aztec for all users
                AppPrefs.setVisualEditorEnabled(false);
                AppPrefs.setAztecEditorEnabled(true);
                currentVersion++;
            case 60:
                // add Start of Week site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_START_OF_WEEK);
                currentVersion++;
            case 61:
                // add date & time format site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_TIME_FORMAT);
                mDb.execSQL(SiteSettingsModel.ADD_DATE_FORMAT);
                currentVersion++;
            case 62:
                // add timezone and posts per page site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_TIMEZONE);
                mDb.execSQL(SiteSettingsModel.ADD_POSTS_PER_PAGE);
                currentVersion++;
            case 63:
                // add AMP site setting as part of #betterjetpackxp
                mDb.execSQL(SiteSettingsModel.ADD_AMP_SUPPORTED);
                mDb.execSQL(SiteSettingsModel.ADD_AMP_ENABLED);
                currentVersion++;
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
