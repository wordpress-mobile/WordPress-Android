package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.SiteSettingsModel;

public final class SiteSettingsTable {
    public static final String SETTINGS_TABLE_NAME = "site_settings";

    public static final String ID_COLUMN_NAME = "id";
    public static final String ADDRESS_COLUMN_NAME = "address";
    public static final String USERNAME_COLUMN_NAME = "username";
    public static final String PASSWORD_COLUMN_NAME = "password";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String TAGLINE_COLUMN_NAME = "tagline";
    public static final String LANGUAGE_COLUMN_NAME = "language";
    public static final String PRIVACY_COLUMN_NAME = "privacy";
    public static final String LOCATION_COLUMN_NAME = "location";

    private static final String CREATE_SETTINGS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
            SETTINGS_TABLE_NAME +
            " (" +
            ID_COLUMN_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            ADDRESS_COLUMN_NAME + " TEXT, " +
            USERNAME_COLUMN_NAME + " TEXT, " +
            PASSWORD_COLUMN_NAME + " TEXT, " +
            TITLE_COLUMN_NAME + " TEXT, " +
            TAGLINE_COLUMN_NAME + " TEXT, " +
            LANGUAGE_COLUMN_NAME + " INTEGER, " +
            PRIVACY_COLUMN_NAME + " INTEGER, " +
            LOCATION_COLUMN_NAME + " BOOLEAN" +
            ");";

    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(CREATE_SETTINGS_TABLE_SQL);
        }
    }

    public static Cursor getSettings(long id) {
        if (id < 0) return null;

        String sqlCommand = sqlSelectAllSettings() + sqlWhere(ID_COLUMN_NAME, Long.toString(id)) + ";";
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
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
        values.put(LOCATION_COLUMN_NAME, settings.location);

        settings.isInLocalTable = WordPress.wpDB.getDatabase().insertWithOnConflict(
                SETTINGS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void deleteSettings(SiteSettingsModel settings) {
        if (settings == null) return;

        String[] args = {Long.toString(settings.localTableId)};
        WordPress.wpDB.getDatabase().delete(SETTINGS_TABLE_NAME, ID_COLUMN_NAME + "=?", args);
    }

    private static String sqlSelectAllSettings() {
        return "SELECT * FROM " + SETTINGS_TABLE_NAME + " ";
    }

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=\"" + value + "\" ";
    }
}
