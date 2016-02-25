package org.wordpress.android.util;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;

public class SqlUtils {
    private SqlUtils() {
        throw new AssertionError();
    }

    /*
     * SQLite doesn't have a boolean datatype, so booleans are stored as 0=false, 1=true
     */
    public static long boolToSql(boolean value) {
        return (value ? 1 : 0);
    }
    public static boolean sqlToBool(int value) {
        return (value != 0);
    }

    public static void closeStatement(SQLiteStatement stmt) {
        if (stmt != null) {
            stmt.close();
        }
    }

    public static void closeCursor(Cursor c) {
        if (c != null && !c.isClosed()) {
            c.close();
        }
    }

    /*
     * wrapper for DatabaseUtils.longForQuery() which returns 0 if query returns no rows
     */
    public static long longForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try {
            return DatabaseUtils.longForQuery(db, query, selectionArgs);
        } catch (SQLiteDoneException e) {
            return 0;
        }
    }

    public static int intForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        long value = longForQuery(db, query, selectionArgs);
        return (int)value;
    }

    public static boolean boolForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        long value = longForQuery(db, query, selectionArgs);
        return sqlToBool((int) value);
    }

    /*
     * wrapper for DatabaseUtils.stringForQuery(), returns "" if query returns no rows
     */
    public static String stringForQuery(SQLiteDatabase db, String query, String[] selectionArgs) {
        try {
            return DatabaseUtils.stringForQuery(db, query, selectionArgs);
        } catch (SQLiteDoneException e) {
            return "";
        }
    }

    /*
     * returns the number of rows in the passed table
     */
    public static long getRowCount(SQLiteDatabase db, String tableName) {
        return DatabaseUtils.queryNumEntries(db, tableName);
    }

    /*
     * removes all rows from the passed table
     */
    public static void deleteAllRowsInTable(SQLiteDatabase db, String tableName) {
        db.delete(tableName, null, null);
    }

    /*
     * drop all tables from the passed SQLiteDatabase - make sure to pass a
     * writable database
     */
    public static boolean dropAllTables(SQLiteDatabase db) throws SQLiteException {
        if (db == null) {
            return false;
        }

        if (db.isReadOnly()) {
            throw new SQLiteException("can't drop tables from a read-only database");
        }

        List<String> tableNames = new ArrayList<String>();
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        if (cursor.moveToFirst()) {
            do {
                String tableName = cursor.getString(0);
                if (!tableName.equals("android_metadata") && !tableName.equals("sqlite_sequence")) {
                    tableNames.add(tableName);
                }
            } while (cursor.moveToNext());
        }

        db.beginTransaction();
        try {
            for (String tableName: tableNames) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }
            db.setTransactionSuccessful();
            return true;
        } finally {
            db.endTransaction();
            closeCursor(cursor);
        }
    }

    /*
     * Android's CursorWindow has a max size of 2MB per row which can be exceeded
     * with a very large text column, causing an IllegalStateException when the
     * row is read - prevent this by limiting the amount of text that's stored in
     * the text column.
     * https://github.com/android/platform_frameworks_base/blob/b77bc869241644a662f7e615b0b00ecb5aee373d/core/res/res/values/config.xml#L1268
     * https://github.com/android/platform_frameworks_base/blob/3bdbf644d61f46b531838558fabbd5b990fc4913/core/java/android/database/CursorWindow.java#L103
     */
    // Max 512K characters (a UTF-8 char is 4 bytes max, so a 512K characters string is always < 2Mb)
    private static final int MAX_TEXT_LEN = 1024 * 1024 / 2;
    public static String maxSQLiteText(final String text) {
        if (text.length() <= MAX_TEXT_LEN) {
            return text;
        }
        AppLog.w(T.UTILS, "sqlite > max text exceeded, storing truncated text");
        return text.substring(0, MAX_TEXT_LEN);
    }

    /**
     * Convenience method for getting a formatted SQL WHERE clause.
     *
     * @return
     *  formatted as "WHERE {@code column}=?"
     */
    public static String where(String column) {
        return "WHERE " + column + "=?";
    }

    public static String where(String column, String value) {
        return "WHERE " + column + "='" + value + "'";
    }

    public static boolean doesTableExist(SQLiteDatabase db, String tableName) {
        if (db == null || TextUtils.isEmpty(tableName)) return false;

        Cursor cursor = db.rawQuery("SELECT * FROM SQLITE_MASTER " + where("TBL_NAME", tableName), null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
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
