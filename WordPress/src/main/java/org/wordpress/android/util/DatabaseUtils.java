package org.wordpress.android.util;

import android.database.Cursor;
import android.support.annotation.NonNull;

import java.util.List;

public class DatabaseUtils {
    /**
     * Convenience method for getting a formatted SQL WHERE clause.
     *
     * @return
     *  formatted as "WHERE {@code column}=?"
     */
    public static String where(String column) {
        return "WHERE " + column + "=?";
    }

    /**
     * @return
     *  the integer value for the given column, -1 if the column name is not valid
     */
    public static int getIntFromCursor(@NonNull Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : -1;
    }
    /**
     * @return
     *  the long value for the given column, -1 if the column name is not valid
     */
    public static long getLongFromCursor(@NonNull Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getLong(columnIndex) : -1;
    }

    /**
     * @return
     *  the String value for the given column, "" if the column name is not valid
     */
    public static String getStringFromCursor(@NonNull Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }

    /**
     * @return
     *  the boolean value for the given column, false if the column name is not valid
     */
    public static boolean getBooleanFromCursor(@NonNull Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }

    /**
     * Generates a string list from a {@link List} of {@link Object}'s. Items are added via
     * {@link Object#toString()} and separated by {@code separator}.
     *
     * @param items
     *  a list of items to be added to the string list
     * @param separator
     *  a {@link String} used to separate each item
     * @return
     *  a {@link String} of the form "[items0][separator][items1][separator][items2]..."
     */
    public static @NonNull String separatedStringList(List<?> items, String separator) {
        if (items == null || items.size() == 0) return "";

        StringBuilder builder = new StringBuilder();
        builder.append(items.get(0));

        if (items.size() > 1) {
            for (int i = 1; i < items.size(); ++i) {
                builder.append(separator);
                builder.append(items.get(i));
            }
        }

        return builder.toString();
    }
}
