package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.prefs.SiteSettings;

public final class SiteSettingsTable {
    public static final String SETTINGS_TABLE_NAME = "site-settings";

    public static final String ID_COLUMN_NAME = "id";
    public static final String ADDRESS_COLUMN_NAME = "address";
    public static final String USERNAME_COLUMN_NAME = "username";
    public static final String PASSWORD_COLUMN_NAME = "password";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String TAGLINE_COLUMN_NAME = "tagline";
    public static final String LANGUAGE_COLUMN_NAME = "language";
    public static final String PRIVACY_COLUMN_NAME = "privacy";

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
            PRIVACY_COLUMN_NAME + " INTEGER" +
            ");";

    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(CREATE_SETTINGS_TABLE_SQL);
        }
    }

    public static Cursor getSettings(String address) {
        if (TextUtils.isEmpty(address)) return null;

        String sqlCommand = sqlSelectAllSettings() + sqlWhere(ADDRESS_COLUMN_NAME, address);
        return WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null);
    }

    public static void saveSettings(SiteSettings.SettingsContainer settings) {
    }

    private static String sqlSelectAllSettings() {
        return "SELECT * FROM " + SETTINGS_TABLE_NAME;
    }

    private static String sqlWhere(String variable, String value) {
        return "WHERE " + variable + "=" + value;
    }
}
