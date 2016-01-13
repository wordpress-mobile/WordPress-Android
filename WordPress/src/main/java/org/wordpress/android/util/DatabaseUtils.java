package org.wordpress.android.util;

import android.database.Cursor;

public class DatabaseUtils {
    /**
     * @return
     *  the integer value for the given column, -1 if the column name is not valid
     */
    public static int getIntFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : -1;
    }

    /**
     * @return
     *  the String value for the given column, "" if the column name is not valid
     */
    public static String getStringFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }

    /**
     * @return
     *  the boolean value for the given column, false if the column name is not valid
     */
    public static boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }
}
