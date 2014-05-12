package org.wordpress.android.datasets;

import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * A class to represent an database table.
 */

public abstract class SQLTable {
    public abstract String getName();

    protected abstract String getUniqueConstraint();

    protected abstract Map<String, String> getColumnMapping();

    protected static class BaseColumns {
        protected final static String _ID = "_id";
    }

    public String toCreateQuery() {
        String createQuery = "CREATE TABLE IF NOT EXISTS " + getName() + " (";

        Map<String, String> columns = getColumnMapping();

        for (String column : columns.keySet()) {
            createQuery += column + " " + columns.get(column) + ", ";
        }

        createQuery += getUniqueConstraint() + ");";

        return createQuery;
    }

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public Cursor query(final SQLiteDatabase database, final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
        return database.query(getName(), projection, selection, selectionArgs, null, null, sortOrder);
    }

    public long insert(final SQLiteDatabase database, final Uri uri, final ContentValues values) {
        return database.insert(getName(), null, values);
    }

    public long insert(final SQLiteDatabase database, final ContentValues values) {
        return insert(database, null, values);
    }

    public int update(final SQLiteDatabase database, final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
        return database.update(getName(), values, selection, selectionArgs);
    }

    public int update(final SQLiteDatabase database, final ContentValues values, final String selection, final String[] selectionArgs) {
        return update(database, null, values, selection, selectionArgs);
    }

    public int delete(final SQLiteDatabase database, final Uri uri, final String selection, final String[] selectionArgs) {
        return database.delete(getName(), selection, selectionArgs);
    }

    public int delete(final SQLiteDatabase database, final String selection, final String[] selectionArgs) {
        return delete(database, null, selection, selectionArgs);
    }
}
