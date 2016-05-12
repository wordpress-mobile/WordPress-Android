package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * user's reader search history
 */
public class ReaderSearchTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_search_history ("
                 + "	query_string   TEXT NOT NULL COLLATE NOCASE PRIMARY KEY,"
                 + "    date_used      TEXT,"
                 + "    counter        INTEGER DEFAULT 1)");
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

    public static List<String> getQueryStrings() {
        return getQueryStrings(null);
    }
    public static List<String> getQueryStrings(String filter) {
        List<String> queries = new ArrayList<>();
        Cursor cursor;
        if (TextUtils.isEmpty(filter)) {
            cursor = ReaderDatabase.getReadableDb().rawQuery(
                    "SELECT query_string FROM tbl_search_history ORDER BY date_used DESC", null);
        } else {
            String likeFilter = filter + "%";
            cursor = ReaderDatabase.getReadableDb().rawQuery(
                    "SELECT query_string FROM tbl_search_history WHERE query_string LIKE ? ORDER BY date_used DESC", new String[]{likeFilter});
        }

        try {
            while (cursor.moveToNext()) {
                queries.add(cursor.getString(0));
            }
            return queries;
        } finally {
            SqlUtils.closeCursor(cursor);
        }
    }
}
