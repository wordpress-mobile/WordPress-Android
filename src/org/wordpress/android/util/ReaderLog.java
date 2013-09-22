package org.wordpress.android.util;

import android.util.Log;

import com.android.volley.VolleyError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by nbradbury on 6/21/13.
 * simple wrapper for Android log calls, currently used only by the native reader
 * enables recording & displaying log
 */
public class ReaderLog {

    public static final String TAG = "WordPress.Reader";
    private static boolean mEnableRecording = false;

    private ReaderLog() {
        throw new AssertionError();
    }

    /*
     * defaults to false, pass true to capture log so it can be displayed by ReaderLogViewerActivity
     */
    public static void enableRecording(boolean enable) {
        mEnableRecording = enable;
    }

    public static void d(String message) {
        Log.d(TAG, message);
        addEntry(LogLevel.d, message);
    }

    public static void i(String message) {
        Log.i(TAG, message);
        addEntry(LogLevel.i, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
        addEntry(LogLevel.w, message);
    }

    public static void e(Exception e) {
        Log.e(TAG, e.getMessage(), e);
        addEntry(LogLevel.e, e.getMessage());
    }
    public static void e(VolleyError volleyError) {
        if (volleyError==null)
            return;
        String logText;
        if (volleyError.networkResponse==null) {
            logText = volleyError.getMessage();
        } else {
            logText = volleyError.getMessage() + ", status " + volleyError.networkResponse.statusCode + " - " + volleyError.networkResponse.toString();
        }
        Log.e(TAG, logText, volleyError);
        addEntry(LogLevel.w, logText);
    }

    // --------------------------------------------------------------------------------------------------------

    private static final int MAX_ENTRIES = 99;

    private enum LogLevel {
        d, i, w, e;
        private String toHtmlColor() {
            switch(this) {
                case i  : return "black";
                case w  : return "purple";
                case e  : return "red";
                default : return "teal";
            }
        }
    }

    private static class LogEntry {
        LogLevel logLevel;
        String logText;

        private String toHtml() {
            StringBuilder sb = new StringBuilder()
                    .append("<font color='")
                    .append(logLevel.toHtmlColor())
                    .append("'>")
                    .append(logLevel.name())
                    .append(": ")
                    .append(logText)
                    .append("</font>");
            return sb.toString();
        }
    }

    private static class LogEntryList extends ArrayList<LogEntry> {
        private synchronized boolean addEntry(LogEntry entry) {
            if (size() >= MAX_ENTRIES)
                removeFirstEntry();
            return add(entry);
        }
        private void removeFirstEntry() {
            Iterator<LogEntry> it = iterator();
            if (!it.hasNext())
                return;
            try {
                remove(it.next());
            } catch (NoSuchElementException e) {
                // ignore
            }
        }
    }

    private static LogEntryList mLogEntries = new LogEntryList();

    private static void addEntry(LogLevel level, String text) {
        // skip if recording is disabled (default)
        if (!mEnableRecording)
            return;
        LogEntry entry = new LogEntry();
        entry.logLevel = level;
        entry.logText = text;
        mLogEntries.addEntry(entry);
    }

    /*
     * returns entire log as html for display (see ReaderLogViewerActivity)
     */
    public static String toHtml() {
        StringBuilder sb = new StringBuilder();
        Iterator<LogEntry> it = mLogEntries.iterator();
        int lineNum = 1;
        while (it.hasNext()) {
            sb.append("<font color='silver'>")
              .append(String.format("%02d", lineNum))
              .append("</font> ")
              .append(it.next().toHtml())
              .append("<br />");
            lineNum++;
        }
        return sb.toString();
    }
}
