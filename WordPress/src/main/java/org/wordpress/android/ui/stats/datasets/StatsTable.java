package org.wordpress.android.ui.stats.datasets;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.service.StatsService.StatsEndpointsEnum;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

public class StatsTable {

    private static final String TABLE_NAME = "tbl_stats";
    public static final int CACHE_TTL_MINUTES = 10;

    static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " ("
                + " id              INTEGER PRIMARY KEY ASC," // Also alias for the built-in rowid:  "rowid", "oid", or "_rowid_"
                + " blogID          INTEGER NOT NULL,"        // The local blog_id as stored in the WPDB
                + " type            INTEGER DEFAULT 0,"       // The type of the stats. TopPost, followers, etc..
                + " timeframe       INTEGER DEFAULT 0,"       // This could be days, week, years - It's an enum
                + " date            TEXT NOT NULL,"
                + " jsonData        TEXT NOT NULL,"
                + " maxResult       INTEGER DEFAULT 0,"
                + " page            INTEGER DEFAULT 0,"
                + " timestamp       INTEGER NOT NULL,"        // The unix timestamp of the response
                + " UNIQUE (blogID, type, timeframe, date) ON CONFLICT REPLACE"
                + ")");
    }

    static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }


    public static String getStats(final Context ctx, final int blogId, final StatsTimeframe timeframe, final String date,
                                  final StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested, final int pageRequested) {
        if (ctx == null) {
            AppLog.e(AppLog.T.STATS, "Cannot insert a null stats since the passed context is null. Context is required " +
                    "to access the DB.");
            return null;
        }

        String sql = "SELECT *  FROM " + TABLE_NAME + " WHERE blogID = ? "
                + " AND type=?"
                + " AND timeframe=?"
                + " AND date=?"
                + " AND page=?"
                + " AND maxResult >=?"
                + " ORDER BY timestamp DESC"
                + " LIMIT 1";

        String[] args = {
                Integer.toString(blogId),
                Integer.toString(sectionToUpdate.ordinal()),
                Integer.toString(timeframe.ordinal()),
                date,
                Integer.toString(pageRequested),
                Integer.toString(maxResultsRequested),
        };

        Cursor cursor = StatsDatabaseHelper.getReadableDb(ctx).rawQuery(sql, args);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                long timestamp  = cursor.getLong(cursor.getColumnIndex("timestamp"));
                long currentTime = System.currentTimeMillis();
                long deltaMS = currentTime - timestamp;
                if (deltaMS < 0) {
                    // current date is in the past respect to stats date?? Uhhh!
                    return null;
                }

                deltaMS = deltaMS / 1000; // seconds
                // check if the cache is fresh
                if ((deltaMS / 60) > CACHE_TTL_MINUTES) {
                    return null; // cache is expired
                }

                return cursor.getString(cursor.getColumnIndex("jsonData"));
            } else {
                return null;
            }
        } catch (IllegalStateException e) {
            AppLog.e(AppLog.T.STATS, e);
        } finally {
            SqlUtils.closeCursor(cursor);
        }

        return null;
    }

    public static void insertStats(final Context ctx, final int blogId, final StatsTimeframe timeframe, final String date,
                                   final StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested, final int pageRequested,
                                   final String jsonResponse, final long responseTimestamp) {

        if (ctx == null) {
            AppLog.e(AppLog.T.STATS, "Cannot insert a null stats since the passed context is null. Context is required " +
                    "to access the DB.");
            return;
        }

        SQLiteDatabase db = StatsDatabaseHelper.getWritableDb(ctx);
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO " + TABLE_NAME + " (blogID, type, timeframe, date, " +
                "jsonData, maxResult, page, timestamp) VALUES (?1,?2,?3,?4,?5,?6,?7,?8)");
        try {
            stmt.bindLong(1, blogId);
            stmt.bindLong(2, sectionToUpdate.ordinal());
            stmt.bindLong(3, timeframe.ordinal());
            stmt.bindString(4, date);
            stmt.bindString(5, jsonResponse);
            stmt.bindLong(6, maxResultsRequested);
            stmt.bindLong(7, pageRequested);
            stmt.bindLong(8, responseTimestamp);
            stmt.execute();

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    /**
     *  Delete expired Stats data from StatsDB
     */
    public static boolean deleteOldStats(final Context ctx, final long timestamp) {
        if (ctx == null) {
            AppLog.e(AppLog.T.STATS, "Cannot delete stats since the passed context is null. Context is required " +
                    "to access the DB.");
            return false;
        }

        SQLiteDatabase db = StatsDatabaseHelper.getWritableDb(ctx);
        try {
            db.beginTransaction();
            int rowDeleted = db.delete(TABLE_NAME, "timestamp <= ?", new String[] { Long.toString(timestamp) });
            db.setTransactionSuccessful();
            AppLog.d(AppLog.T.STATS, "Number of old stats deleted : " + rowDeleted);
            return rowDeleted > 1;
        } finally {
            db.endTransaction();
        }
    }

    public static boolean deleteStatsForBlog(final Context ctx, final int blogId) {
        if (ctx == null) {
            AppLog.e(AppLog.T.STATS, "Cannot delete stats since the passed context is null. Context is required " +
                    "to access the DB.");
            return false;
        }

        SQLiteDatabase db = StatsDatabaseHelper.getWritableDb(ctx);
        try {
            db.beginTransaction();
            int rowDeleted = db.delete(TABLE_NAME, "blogID=?", new String[] {Integer.toString(blogId)});
            db.setTransactionSuccessful();
            AppLog.d(AppLog.T.STATS, "Stats deleted for localBlogID " + blogId);
            return rowDeleted > 1;
        } finally {
            db.endTransaction();
        }
    }

    public static void purgeAll(Context ctx) {
        if (ctx == null) {
            AppLog.e(AppLog.T.STATS, "Cannot purgeAll stats since the passed context is null. Context is required " +
                    "to access the DB.");
            return;
        }
        SQLiteDatabase db = StatsDatabaseHelper.getWritableDb(ctx);
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM " + TABLE_NAME);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
