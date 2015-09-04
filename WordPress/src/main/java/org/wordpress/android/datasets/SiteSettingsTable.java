package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;

public final class SiteSettingsTable {
    public static final String SETTINGS_TABLE_NAME = "site_settings";
    public static final String CATEGORIES_TABLE_NAME = "site_categories";
    public static final String POST_FORMATS_TABLE_NAME = "site_post_formats";

    // Categories table column names
    public static final String CAT_ID_COLUMN_NAME = "ID";
    public static final String CAT_NAME_COLUMN_NAME = "name";
    public static final String CAT_SLUG_COLUMN_NAME = "slug";
    public static final String CAT_DESC_COLUMN_NAME = "description";
    public static final String CAT_PARENT_ID_COLUMN_NAME = "parent";
    public static final String CAT_POST_COUNT_COLUMN_NAME = "post_count";

    // Post formats table column names

    // Settings table column names
    public static final String ID_COLUMN_NAME = "id";
    public static final String ADDRESS_COLUMN_NAME = "address";
    public static final String USERNAME_COLUMN_NAME = "username";
    public static final String PASSWORD_COLUMN_NAME = "password";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String TAGLINE_COLUMN_NAME = "tagline";
    public static final String LANGUAGE_COLUMN_NAME = "language";
    public static final String PRIVACY_COLUMN_NAME = "privacy";
    public static final String LOCATION_COLUMN_NAME = "location";
    public static final String DEF_CATEGORY_COLUMN_NAME = "defaultCategory";
    public static final String DEF_POST_FORMAT_COLUMN_NAME = "defaultPostFormat";
    public static final String CATEGORIES_COLUMN_NAME = "categories";
    public static final String POST_FORMATS_COLUMN_NAME = "postFormats";

    private static final String CREATE_CATEGORIES_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    CATEGORIES_TABLE_NAME +
                    " (" +
                    CAT_ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    CAT_NAME_COLUMN_NAME + " TEXT, " +
                    CAT_SLUG_COLUMN_NAME + " TEXT, " +
                    CAT_DESC_COLUMN_NAME + " TEXT, " +
                    CAT_PARENT_ID_COLUMN_NAME + " INTEGER, " +
                    CAT_POST_COUNT_COLUMN_NAME + " INTEGER" +
                    ");";

    private static final String CREATE_SETTINGS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
            SETTINGS_TABLE_NAME +
            " (" +
            ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
            ADDRESS_COLUMN_NAME + " TEXT, " +
            USERNAME_COLUMN_NAME + " TEXT, " +
            PASSWORD_COLUMN_NAME + " TEXT, " +
            TITLE_COLUMN_NAME + " TEXT, " +
            TAGLINE_COLUMN_NAME + " TEXT, " +
            LANGUAGE_COLUMN_NAME + " INTEGER, " +
            PRIVACY_COLUMN_NAME + " INTEGER, " +
            LOCATION_COLUMN_NAME + " BOOLEAN, " +
            DEF_CATEGORY_COLUMN_NAME + " TEXT, " +
            DEF_POST_FORMAT_COLUMN_NAME + " TEXT, " +
            CATEGORIES_COLUMN_NAME + " TEXT, " +
            POST_FORMATS_COLUMN_NAME + " TEXT" +
            ");";

    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(CREATE_SETTINGS_TABLE_SQL);
            db.execSQL(CREATE_CATEGORIES_TABLE_SQL);
        }
    }

    public static Cursor getCategory(long id) {
        if (id < 0) return null;

        String sqlCommand = sqlSelectAllCategories() + sqlWhere(CAT_ID_COLUMN_NAME, Long.toString(id)) + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static Cursor getSettings(long id) {
        if (id < 0) return null;

        String sqlCommand = sqlSelectAllSettings() + sqlWhere(ID_COLUMN_NAME, Long.toString(id)) + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static void saveCategory(CategoryModel category) {
        if (category == null) return;

        ContentValues values = new ContentValues();
        values.put(CAT_ID_COLUMN_NAME, category.id);
        values.put(CAT_NAME_COLUMN_NAME, category.name);
        values.put(CAT_SLUG_COLUMN_NAME, category.slug);
        values.put(CAT_DESC_COLUMN_NAME, category.description);
        values.put(CAT_PARENT_ID_COLUMN_NAME, category.parentId);
        values.put(CAT_POST_COUNT_COLUMN_NAME, category.postCount);

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

        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, settings.localTableId);
        values.put(ADDRESS_COLUMN_NAME, settings.address);
        values.put(USERNAME_COLUMN_NAME, settings.username);
        values.put(PASSWORD_COLUMN_NAME, settings.password);
        values.put(TITLE_COLUMN_NAME, settings.title);
        values.put(TAGLINE_COLUMN_NAME, settings.tagline);
        values.put(PRIVACY_COLUMN_NAME, settings.privacy);
        values.put(LANGUAGE_COLUMN_NAME, settings.languageId);
        values.put(DEF_CATEGORY_COLUMN_NAME, settings.defaultCategory);
        values.put(CATEGORIES_COLUMN_NAME, commaSeparatedElements(settings.categories));
        values.put(DEF_POST_FORMAT_COLUMN_NAME, settings.defaultPostFormat);
//        values.put(POST_FORMATS_COLUMN_NAME, commaSeparatedElements(settings.postFormats));

        settings.isInLocalTable = WordPress.wpDB.getDatabase().insertWithOnConflict(
                SETTINGS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void deleteSettings(SiteSettingsModel settings) {
        if (settings == null) return;

        String[] args = {Long.toString(settings.localTableId)};
        WordPress.wpDB.getDatabase().delete(SETTINGS_TABLE_NAME, ID_COLUMN_NAME + "=?", args);
    }

    private static String sqlSelectAllCategories() {
        return "SELECT * FROM " + CATEGORIES_TABLE_NAME + " ";
    }

    private static String sqlSelectAllSettings() {
        return "SELECT * FROM " + SETTINGS_TABLE_NAME + " ";
    }

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=\"" + value + "\" ";
    }

    private static String commaSeparatedElements(CategoryModel[] elements) {
        if (elements == null) return "";

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < elements.length - 1; ++i) {
            builder.append(Integer.toString(elements[i].id)).append(",");
        }
        builder.append(Integer.toString(elements[elements.length - 1].id));

        return builder.toString();
    }
}
