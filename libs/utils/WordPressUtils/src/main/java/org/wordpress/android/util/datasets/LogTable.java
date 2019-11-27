package org.wordpress.android.util.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.Date;


public class LogTable {

    public static class LogTableEntry {
        public final String mLogLevel;
        public final String mLogTag;
        public final String mLogText;
        public final java.util.Date mDate;

        LogTableEntry(final String logLevel, final String logTag, final String logText, final java.util.Date date) {
            mLogLevel = logLevel;
            mLogTag = logTag;
            mLogText = logText;
            mDate = date;
        }
    }

    public static class LogTableSessionData
    {
        public final long mSessionId;
        public final String mAppInfoHeader;
        public final String mDeviceInfoHeader;
        public ArrayList<LogTableEntry> mLogEntries;

        LogTableSessionData(final long sessionId, final String appInfoHeader, final String deviceInfoHeader) {
            mSessionId = sessionId;
            mAppInfoHeader = appInfoHeader;
            mDeviceInfoHeader = deviceInfoHeader;
            mLogEntries = new ArrayList<LogTableEntry>();
        }
    }

    private static final int MAX_ENTRIES = 99;

    private static final String LOG_INFO_TABLE = "tbl_log_info";
    private static final String LOG_ENTRY_TABLE = "tbl_log_entry";

    private static final String COLUMN_LOG_SESSION_ID = "log_session_id";
    private static final String COLUMN_APP_INFO_HEADER = "app_info_header";
    private static final String COLUMN_DEVICE_INFO_HEADER = "device_info_header";
    private static final String COLUMN_LOG_ID = "log_id";
    private static final String COLUMN_LOG_LEVEL = "log_level";
    private static final String COLUMN_LOG_TAG = "log_tag";
    private static final String COLUMN_LOG_TEXT = "log_text";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public static void createTables(final SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + LOG_INFO_TABLE + " ("
                + COLUMN_LOG_SESSION_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_APP_INFO_HEADER + " TEXT,"
                + COLUMN_DEVICE_INFO_HEADER + " TEXT"
                + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + LOG_ENTRY_TABLE + " ("
                + COLUMN_LOG_ID + " INTEGER NOT NULL PRIMARY KEY,"
                + COLUMN_LOG_SESSION_ID + " INTEGER,"
                + COLUMN_LOG_LEVEL + " TEXT,"
                + COLUMN_LOG_TAG + " TEXT,"
                + COLUMN_LOG_TEXT + " TEXT,"
                + COLUMN_TIMESTAMP + " INTEGER"
                + ")");
    }

    public static void dropTables(final SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + LOG_INFO_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + LOG_ENTRY_TABLE);
    }

    public static void reset(final SQLiteDatabase db) {
        db.beginTransaction();
        try {
            dropTables(db);
            createTables(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static long getNewLogSessionId(final SQLiteDatabase db, final String appInfoHeader, final String deviceInfoHeader) {
        db.beginTransaction();
        long sessionId = -1;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_APP_INFO_HEADER, appInfoHeader);
            values.put(COLUMN_DEVICE_INFO_HEADER, deviceInfoHeader);
            sessionId = db.insert(LOG_INFO_TABLE, null, values);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            return sessionId;
        }
    }

    public static boolean addLogEntry(final SQLiteDatabase db, final long sessionId, final String logLevel, final String logTag, final String logText, final java.util.Date date) {
        db.beginTransaction();
        boolean success = false;
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_LOG_SESSION_ID, sessionId);
            values.put(COLUMN_LOG_LEVEL, logLevel);
            values.put(COLUMN_LOG_TAG, logTag);
            values.put(COLUMN_LOG_TEXT, logText);
            values.put(COLUMN_TIMESTAMP, date.getTime() / 1000); // store as epoch in seconds
            db.insert(LOG_ENTRY_TABLE, null, values);
            deleteOldEntries(db);
            db.setTransactionSuccessful();
            success = true;
        } finally {
            db.endTransaction();
            return success;
        }
    }

    public static ArrayList<LogTableSessionData> getData(final SQLiteDatabase db) {
        ArrayList<LogTableSessionData> logSessionDataList = new ArrayList<LogTableSessionData>();

        Cursor cursorLogInfo = db.rawQuery("SELECT " +
                COLUMN_LOG_SESSION_ID + ", " +
                COLUMN_APP_INFO_HEADER + ", " +
                COLUMN_DEVICE_INFO_HEADER + " FROM " +
                LOG_INFO_TABLE, null);

        if (cursorLogInfo.moveToFirst()) {
            do {
                final LogTableSessionData sessionData = getSessionDataFromCursor(cursorLogInfo);
                logSessionDataList.add(sessionData);
            } while (cursorLogInfo.moveToNext());
        }

        for (LogTableSessionData sessionData : logSessionDataList) {
            String[] selectionArgs = new String[] {String.valueOf(sessionData.mSessionId)};
            Cursor cursorLogEntry = db.rawQuery("SELECT " +
                COLUMN_LOG_LEVEL + ", " +
                COLUMN_LOG_TAG + ", " +
                COLUMN_LOG_TEXT + ", " +
                COLUMN_TIMESTAMP + " FROM " +
                LOG_ENTRY_TABLE + " WHERE " +
                COLUMN_LOG_SESSION_ID + "=? " +
                "ORDER BY " + COLUMN_TIMESTAMP + " ASC",
                selectionArgs);

            // raven start here. get log data out of the cursor, store it in session data, add session data to output array list
            sessionData.mLogEntries = new ArrayList<LogTableEntry>();
            if (cursorLogEntry.moveToFirst()) {
                do {
                    sessionData.mLogEntries.add(getLogEntryDataFromCursor(cursorLogEntry));
                } while (cursorLogEntry.moveToNext());
            }
        }

        return logSessionDataList;
    }

    private static void deleteOldEntries(final SQLiteDatabase db) {
        final String deleteOldEntriesQuery = "DELETE FROM " + LOG_ENTRY_TABLE +
                " WHERE " + COLUMN_LOG_ID + " NOT IN " +
                "(SELECT " + COLUMN_LOG_ID + " FROM " +
                "(SELECT " + COLUMN_LOG_ID + ", " + COLUMN_TIMESTAMP + " FROM " +
                LOG_ENTRY_TABLE + " ORDER BY " + COLUMN_TIMESTAMP + " DESC " +
                "LIMIT " + String.valueOf(MAX_ENTRIES) + "))";
        db.execSQL(deleteOldEntriesQuery);

        db.execSQL("DELETE FROM " + LOG_INFO_TABLE +
                " WHERE " + COLUMN_LOG_SESSION_ID + " NOT IN " +
                "(SELECT " + COLUMN_LOG_SESSION_ID + " FROM " +
                LOG_ENTRY_TABLE + ")");
    }

    private static LogTableSessionData getSessionDataFromCursor(final Cursor cursor) {
        return new LogTableSessionData(cursor.getLong(cursor.getColumnIndex(COLUMN_LOG_SESSION_ID)),
                cursor.getString(cursor.getColumnIndex(COLUMN_APP_INFO_HEADER)),
                cursor.getString(cursor.getColumnIndex(COLUMN_DEVICE_INFO_HEADER)));
    }

    private static LogTableEntry getLogEntryDataFromCursor(final Cursor cursor) {
        String logLevel = cursor.getString(cursor.getColumnIndex(COLUMN_LOG_LEVEL));
        String logTag = cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TAG));
        String logText = cursor.getString(cursor.getColumnIndex(COLUMN_LOG_TEXT));
        java.util.Date date = new Date(cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP)) * 1000);
        return new LogTableEntry(logLevel, logTag, logText, date);
    }
}
