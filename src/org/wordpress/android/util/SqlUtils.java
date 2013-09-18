package org.wordpress.android.util;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteStatement;

/**
 * Created by nbradbury on 6/22/13.
 */
public class SqlUtils {

    private SqlUtils() {
        throw new AssertionError();
    }

    /*
     * SQlite doesn't have a boolean datatype, so booleans are stored as 0=false, 1=true
     */
    public static long boolToSql(boolean value) {
        return (value ? 1 : 0);
    }
    public static boolean sqlToBool(int value) {
        return (value != 0);
    }

    public static void closeStatement(SQLiteStatement stmt) {
        if (stmt!=null)
            stmt.close();
    }

    public static void closeCursor(Cursor c) {
        if (c!=null && !c.isClosed())
            c.close();
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
     * wrapper for DatabaseUtils.stringForQuery() which returns "" if query returns no rows
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
}
