package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.util.SparseArrayCompat;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

public final class SiteSettingsTable {
    private static final String CATEGORIES_TABLE_NAME = "site_categories";
    private static final String CREATE_CATEGORIES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS "
            + CATEGORIES_TABLE_NAME
            + " ("
            + CategoryModel.ID_COLUMN_NAME + " INTEGER PRIMARY KEY, "
            + CategoryModel.NAME_COLUMN_NAME + " TEXT, "
            + CategoryModel.SLUG_COLUMN_NAME + " TEXT, "
            + CategoryModel.DESC_COLUMN_NAME + " TEXT, "
            + CategoryModel.PARENT_ID_COLUMN_NAME + " INTEGER, "
            + CategoryModel.POST_COUNT_COLUMN_NAME + " INTEGER"
            + ");";

    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(SiteSettingsModel.CREATE_SETTINGS_TABLE_SQL);
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

    public static SparseArrayCompat<CategoryModel> getAllCategories() {
        String sqlCommand = sqlSelectAllCategories() + ";";
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);

        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) {
            return null;
        }

        SparseArrayCompat<CategoryModel> models = new SparseArrayCompat<>();
        for (int i = 0; i < cursor.getCount(); ++i) {
            CategoryModel model = new CategoryModel();
            model.deserializeFromDatabase(cursor);
            models.put(model.id, model);
            cursor.moveToNext();
        }

        return models;
    }

    public static Cursor getCategory(long id) {
        if (id < 0) {
            return null;
        }

        String sqlCommand = sqlSelectAllCategories() + sqlWhere(CategoryModel.ID_COLUMN_NAME, Long.toString(id)) + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static Cursor getSettings(long id) {
        if (id < 0) {
            return null;
        }

        String whereClause = sqlWhere(SiteSettingsModel.ID_COLUMN_NAME, Long.toString(id));
        String sqlCommand = sqlSelectAllSettings() + whereClause + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static void saveCategory(CategoryModel category) {
        if (category == null) {
            return;
        }

        ContentValues values = category.serializeToDatabase();
        category.isInLocalTable = WordPress.wpDB.getDatabase().insertWithOnConflict(
                CATEGORIES_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void saveCategories(CategoryModel[] categories) {
        if (categories == null) {
            return;
        }

        for (CategoryModel category : categories) {
            saveCategory(category);
        }
    }

    public static void saveSettings(SiteSettingsModel settings) {
        if (settings == null) {
            return;
        }

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

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=\"" + value + "\" ";
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
            AppPrefs.setImageOptimizeMaxSize(
                    cursor.getInt(cursor.getColumnIndex("maxImageWidth")));
            AppPrefs.setImageOptimizeQuality(
                    cursor.getInt(cursor.getColumnIndex("imageEncoderQuality")));
            AppPrefs.setVideoOptimize(
                    cursor.getInt(cursor.getColumnIndex("optimizedVideo")) == 1);
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
