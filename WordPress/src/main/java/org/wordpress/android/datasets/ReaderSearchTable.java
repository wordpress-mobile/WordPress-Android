package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.Date;

/**
 * search suggestion table - populated by user's reader search history
 */
public class ReaderSearchTable {

    public static final String COL_ID    = "_id";
    public static final String COL_QUERY = "query_string";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_search_suggestions ("
                 + "    _id            INTEGER PRIMARY KEY AUTOINCREMENT,"
                 + "	query_string   TEXT NOT NULL COLLATE NOCASE,"
                 + "    date_used      TEXT)");
        db.execSQL("CREATE UNIQUE INDEX idx_search_suggestions_query ON tbl_search_suggestions(query_string)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_search_suggestions");
    }

    /*
     * adds the passed query string, updating the usage date
     */
    public static void addOrUpdateQueryString(@NonNull String query) {
        String date = DateTimeUtils.javaDateToIso8601(new Date());

        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(
                "INSERT OR REPLACE INTO tbl_search_suggestions (query_string, date_used) VALUES (?1,?2)");
        try {
            stmt.bindString(1, query);
            stmt.bindString(2, date);
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static void deleteQueryString(@NonNull String query) {
        String[]args = new String[]{query};
        ReaderDatabase.getWritableDb().delete("tbl_search_suggestions", "query_string=?", args);
    }

    public static void deleteAllQueries() {
        SqlUtils.deleteAllRowsInTable(ReaderDatabase.getWritableDb(), "tbl_search_suggestions");
    }

    /**
     * Returns a cursor containing query strings previously typed by the user
     * @param filter - filters the list using LIKE syntax (pass null for no filter)
     * @param max - limit the list to this many items (pass zero for no limit)
     */
    public static Cursor getQueryStringCursor(String filter, int max) {
        String sql;
        String[] args;
        if (TextUtils.isEmpty(filter)) {
            sql = "SELECT * FROM tbl_search_suggestions";
            args = null;
        } else {
            sql = "SELECT * FROM tbl_search_suggestions WHERE query_string LIKE ?";
            args = new String[]{filter + "%"};
        }

        sql += " ORDER BY date_used DESC";

        if (max > 0) {
            sql += " LIMIT " + max;
        }

        return ReaderDatabase.getReadableDb().rawQuery(sql, args);
    }
}
