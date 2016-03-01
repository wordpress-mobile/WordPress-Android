package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;

import java.util.HashMap;
import java.util.Map;

public final class SiteSettingsTable {
    public static final String CATEGORIES_TABLE_NAME = "site_categories";

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
            db.execSQL(CREATE_CATEGORIES_TABLE_SQL);
        }
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

        String sqlCommand = sqlSelectAllSettings() + sqlWhere(SiteSettingsModel.ID_COLUMN_NAME, Long.toString(id)) + ";";
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

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=\"" + value + "\" ";
    }
}
