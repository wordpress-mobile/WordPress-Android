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
 * user's reader search history
 */
public class ReaderSearchTable {

    public static final String COL_QUERY = "query_string";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_search_history ("
                 + "    _id            INTEGER PRIMARY KEY AUTOINCREMENT,"
                 + "	query_string   TEXT NOT NULL COLLATE NOCASE,"
                 + "    date_used      TEXT,"
                 + "    counter        INTEGER DEFAULT 1)");
        db.execSQL("CREATE UNIQUE INDEX idx_search_query ON tbl_search_history(query_string)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_search_history");
    }

    /*
     * adds the passed query string, updating the usage counter and date
     */
    public static void addOrUpdateQueryString(@NonNull String query) {
        String date = DateTimeUtils.javaDateToIso8601(new Date());
        int counter = getCounterForQueryString(query) + 1;

        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(
                "INSERT OR REPLACE INTO tbl_search_history (query_string, date_used, counter) VALUES (?1,?2,?3)");
        try {
            stmt.bindString(1, query);
            stmt.bindString(2, date);
            stmt.bindLong  (3, counter);
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    private static int getCounterForQueryString(@NonNull String query) {
        String[] args = {query};
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT counter FROM tbl_search_history WHERE query_string=?", args);
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
            sql = "SELECT * FROM tbl_search_history";
            args = null;
        } else {
            sql = "SELECT * FROM tbl_search_history WHERE query_string LIKE ?";
            args = new String[]{filter + "%"};
        }

        sql += " ORDER BY date_used DESC";

        if (max > 0) {
            sql += " LIMIT " + max;
        }

        return ReaderDatabase.getReadableDb().rawQuery(sql, args);
    }
}
