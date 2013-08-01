
package org.wordpress.android.datasets;

import java.util.Map;

import android.database.sqlite.SQLiteDatabase;

public abstract class SQLTable {

    public abstract String getName();

    protected abstract String getUniqueConstraint();

    protected abstract Map<String, String> getColumnMapping();

    protected static class BaseColumns {
        protected final static String _ID = "_id";
    }

    public String toCreateString() {
        String createStr = "CREATE TABLE IF NOT EXISTS " + getName() + " (";

        Map<String, String> columns = getColumnMapping();

        for (String column : columns.keySet()) {
            createStr += column + " " + columns.get(column) + ", ";
        }

        createStr += getUniqueConstraint() + ");";

        return createStr;
    }

    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    
}
