package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.JetpackSettingsModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.HashMap;
import java.util.Map;

public final class SiteSettingsTable {
    private static final String ID_COLUMN_NAME = "id";
    private static final String JP_MONITOR_ACTIVE_COLUMN_NAME = "monitorActive";
    private static final String JP_MONITOR_EMAIL_NOTES_COLUMN_NAME = "jpEmailNotifications";
    private static final String JP_MONITOR_WP_NOTES_COLUMN_NAME = "jpWpNotifications";

    private static final String JP_SETTINGS_TABLE_NAME = "jp_site_settings";
    private static final String CREATE_JP_SETTINGS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    JP_SETTINGS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    JP_MONITOR_ACTIVE_COLUMN_NAME + " BOOLEAN, " +
                    JP_MONITOR_EMAIL_NOTES_COLUMN_NAME + " BOOLEAN, " +
                    JP_MONITOR_WP_NOTES_COLUMN_NAME + " BOOLEAN" +
                    ");";

    private static final String CATEGORIES_TABLE_NAME = "site_categories";

    private static final String CREATE_CATEGORIES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
            CATEGORIES_TABLE_NAME +
            " (" +
            CategoryModel.ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
            CategoryModel.NAME_COLUMN_NAME + " TEXT, " +
            CategoryModel.SLUG_COLUMN_NAME + " TEXT, " +
            CategoryModel.DESC_COLUMN_NAME + " TEXT, " +
            CategoryModel.PARENT_ID_COLUMN_NAME + " INTEGER, " +
            CategoryModel.POST_COUNT_COLUMN_NAME + " INTEGER" +
            ");";

    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(SiteSettingsModel.CREATE_SETTINGS_TABLE_SQL);
            db.execSQL(CREATE_JP_SETTINGS_TABLE_SQL);
            db.execSQL(CREATE_CATEGORIES_TABLE_SQL);
        }
    }

    public static void addSharingColumnsToSiteSettingsTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(SiteSettingsModel.ADD_SHARING_LABEL);
            db.execSQL(SiteSettingsModel.ADD_SHARING_BUTTON_STYLE);
            db.execSQL(SiteSettingsModel.ADD_ALLOW_REBLOG_BUTTON);
            db.execSQL(SiteSettingsModel.ADD_ALLOW_LIKE_BUTTON);
            db.execSQL(SiteSettingsModel.ADD_ALLOW_COMMENT_LIKES);
            db.execSQL(SiteSettingsModel.ADD_TWITTER_USERNAME);
        }
    }

    public static void deserializeJetpackDatabaseCursor(final @NonNull JetpackSettingsModel jpSettings, Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return;
        jpSettings.monitorActive = getBooleanFromCursor(cursor, JP_MONITOR_ACTIVE_COLUMN_NAME);
        jpSettings.emailNotifications = getBooleanFromCursor(cursor, JP_MONITOR_EMAIL_NOTES_COLUMN_NAME);
        jpSettings.wpNotifications = getBooleanFromCursor(cursor, JP_MONITOR_WP_NOTES_COLUMN_NAME);
    }

    public static ContentValues serializeJetpackSettingsToDatabase(final @NonNull JetpackSettingsModel jpSettings) {
        ContentValues values = new ContentValues();
        values.put(JP_MONITOR_ACTIVE_COLUMN_NAME, jpSettings.monitorActive);
        values.put(JP_MONITOR_EMAIL_NOTES_COLUMN_NAME, jpSettings.emailNotifications);
        values.put(JP_MONITOR_WP_NOTES_COLUMN_NAME, jpSettings.wpNotifications);
        return values;
    }

    public static Map<Integer, CategoryModel> getAllCategories() {
        String sqlCommand = sqlSelectAllCategories() + ";";
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);

        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return null;

        Map<Integer, CategoryModel> models = new HashMap<>();
        for (int i = 0; i < cursor.getCount(); ++i) {
            CategoryModel model = new CategoryModel();
            model.deserializeFromDatabase(cursor);
            models.put(model.id, model);
            cursor.moveToNext();
        }

        return models;
    }

    public static Cursor getCategory(long id) {
        if (id < 0) return null;

        String sqlCommand = sqlSelectAllCategories() + sqlWhere(CategoryModel.ID_COLUMN_NAME, Long.toString(id)) + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static Cursor getSettings(long id) {
        if (id < 0) return null;

        String whereClause = sqlWhere(SiteSettingsModel.ID_COLUMN_NAME, Long.toString(id));
        String sqlCommand = sqlSelectAllSettings() + whereClause + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static Cursor getJpSettings(long id) {
        if (id < 0) return null;

        String whereClause = sqlWhere(ID_COLUMN_NAME, Long.toString(id));
        String sqlCommand = sqlSelectAllJpSettings() + whereClause + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static void saveCategory(CategoryModel category) {
        if (category == null) return;

        ContentValues values = category.serializeToDatabase();
        category.isInLocalTable = WordPress.wpDB.getDatabase().insertWithOnConflict(
                CATEGORIES_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void saveCategories(CategoryModel[] categories) {
        if (categories == null) return;

        for (CategoryModel category : categories) {
            saveCategory(category);
        }
    }

    public static void saveJpSettings(JetpackSettingsModel settings) {
        if (settings == null) return;

        ContentValues values = serializeJetpackSettingsToDatabase(settings);
        WordPress.wpDB.getDatabase().insertWithOnConflict(
                JP_SETTINGS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void saveSettings(SiteSettingsModel settings) {
        if (settings == null) return;

        ContentValues values = settings.serializeToDatabase();
        settings.isInLocalTable = WordPress.wpDB.getDatabase().insertWithOnConflict(
                SiteSettingsModel.SETTINGS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;

        saveCategories(settings.categories);
    }

    private static String sqlSelectAllCategories() {
        return "SELECT * FROM " + CATEGORIES_TABLE_NAME + " ";
    }

    private static String sqlSelectAllSettings() {
        return "SELECT * FROM " + SiteSettingsModel.SETTINGS_TABLE_NAME + " ";
    }

    private static String sqlSelectAllJpSettings() {
        return "SELECT * FROM " + JP_SETTINGS_TABLE_NAME + " ";
    }

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=\"" + value + "\" ";
    }

    private static boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }

    public static boolean migrateMediaOptimizeSettings(SQLiteDatabase db) {
        Cursor cursor = null;
        try {
            String sqlCommand = "SELECT * FROM " + SiteSettingsModel.SETTINGS_TABLE_NAME + ";";
            cursor = db.rawQuery(sqlCommand, null);
            if (cursor == null || cursor.getCount() == 0 || !cursor.moveToFirst()) {
                return false;
            }
            int columnIndex = cursor.getColumnIndex("optimizedImage");
            if (columnIndex == -1) {
                // No old columns for media optimization settings
                return false;
            }
            // we're safe to read all the settings now since all the columns must be there
            int optimizeImageOldSettings = cursor.getInt(columnIndex);
            AppPrefs.setImageOptimize(optimizeImageOldSettings == 1);
            AppPrefs.setImageOptimizeWidth(
                    cursor.getInt(cursor.getColumnIndex("maxImageWidth")));
            AppPrefs.setImageOptimizeQuality(
                    cursor.getInt(cursor.getColumnIndex("imageEncoderQuality")));
            AppPrefs.setVideoOptimize(
                    cursor.getInt(cursor.getColumnIndex("optimizedVideo")) == 1
            );
            AppPrefs.setVideoOptimizeWidth(
                    cursor.getInt(cursor.getColumnIndex("maxVideoWidth")));
            AppPrefs.setVideoOptimizeQuality(
                    cursor.getInt(cursor.getColumnIndex("videoEncoderBitrate")));

            // Delete the old columns? --> cannot drop a specific column in SQLite 3 ;(

            return true;
        } catch (SQLException e) {
            AppLog.e(AppLog.T.DB, "Failed to copy media optimization settings", e);
        } finally {
            SqlUtils.closeCursor(cursor);
        }
        return false;
    }
}
