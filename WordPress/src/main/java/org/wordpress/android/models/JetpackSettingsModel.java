package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

public class JetpackSettingsModel {
    public static final String ID_COLUMN_NAME = "id";
    private static final String JP_MONITOR_ACTIVE_COLUMN_NAME = "monitorActive";
    private static final String JP_MONITOR_EMAIL_NOTES_COLUMN_NAME = "jpEmailNotifications";
    private static final String JP_MONITOR_WP_NOTES_COLUMN_NAME = "jpWpNotifications";

    public static final String JP_SETTINGS_TABLE_NAME = "jp_site_settings";
    public static final String CREATE_JP_SETTINGS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    JP_SETTINGS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    JP_MONITOR_ACTIVE_COLUMN_NAME + " BOOLEAN, " +
                    JP_MONITOR_EMAIL_NOTES_COLUMN_NAME + " BOOLEAN, " +
                    JP_MONITOR_WP_NOTES_COLUMN_NAME + " BOOLEAN" +
                    ");";

    public long localTableId;
    public boolean monitorActive;
    public boolean emailNotifications;
    public boolean wpNotifications;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof JetpackSettingsModel)) return false;
        JetpackSettingsModel otherModel = (JetpackSettingsModel) other;
        return monitorActive == otherModel.monitorActive &&
                emailNotifications == otherModel.emailNotifications &&
                wpNotifications == otherModel.wpNotifications;
    }

    public void deserializeOptionsDatabaseCursor(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return;
        monitorActive = getBooleanFromCursor(cursor, JP_MONITOR_ACTIVE_COLUMN_NAME);
        emailNotifications = getBooleanFromCursor(cursor, JP_MONITOR_EMAIL_NOTES_COLUMN_NAME);
        wpNotifications = getBooleanFromCursor(cursor, JP_MONITOR_WP_NOTES_COLUMN_NAME);
    }

    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(JP_MONITOR_ACTIVE_COLUMN_NAME, monitorActive);
        values.put(JP_MONITOR_EMAIL_NOTES_COLUMN_NAME, emailNotifications);
        values.put(JP_MONITOR_WP_NOTES_COLUMN_NAME, wpNotifications);
        return values;
    }

    private boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }
}
