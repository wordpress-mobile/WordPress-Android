package org.wordpress.android.util;

import android.content.Context;
import android.util.Log;

import com.android.volley.VolleyError;

import org.wordpress.android.WordPress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by nbradbury on 6/21/13.
 * simple wrapper for Android log calls, enables recording & displaying log
 */
public class AppLog {
    // T for Tag
    public enum T {READER, EDITOR, MEDIA, NUX, API, STATS, UTILS, NOTIFS, DB, POSTS, COMMENTS, THEMES, TESTS}
    public static final String TAG = WordPress.TAG;
    private static boolean mEnableRecording = false;

    private AppLog() {
        throw new AssertionError();
    }

    /*
     * defaults to false, pass true to capture log so it can be displayed by AppLogViewerActivity
     */
    public static void enableRecording(boolean enable) {
        mEnableRecording = enable;
    }

    public static void v(T tag, String message) {
        Log.v(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.v, message);
    }

    public static void d(T tag, String message) {
        Log.d(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.d, message);
    }

    public static void i(T tag, String message) {
        Log.i(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.i, message);
    }

    public static void w(T tag, String message) {
        Log.w(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.w, message);
    }

    public static void e(T tag, String message) {
        Log.e(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.e, message);
    }

    public static void e(T tag, String message, Throwable tr) {
        Log.e(TAG + "-" + tag.toString(), message, tr);
        addEntry(tag, LogLevel.e, message + " - exception: " + tr.getMessage());
    }

    public static void e(T tag, Throwable tr) {
        Log.e(TAG + "-" + tag.toString(), tr.getMessage(), tr);
        addEntry(tag, LogLevel.e, tr.getMessage());
    }

    public static void e(T tag, VolleyError volleyError) {
        if (volleyError==null)
            return;
        String logText;
        if (volleyError.networkResponse==null) {
            logText = volleyError.getMessage();
        } else {
            logText = volleyError.getMessage() + ", status "
                    + volleyError.networkResponse.statusCode
                    + " - " + volleyError.networkResponse.toString();
        }
        Log.e(TAG + "-" + tag.toString(), logText, volleyError);
        addEntry(tag, LogLevel.w, logText);
    }

    // --------------------------------------------------------------------------------------------------------

    private static final int MAX_ENTRIES = 99;

    private enum LogLevel {
        v, d, i, w, e;
        private String toHtmlColor() {
            switch(this) {
                case v:
                    return "grey";
                case i:
                    return "black";
                case w:
                    return "purple";
                case e:
                    return "red";
                case d:
                default:
                    return "teal";
            }
        }
    }

    private static class LogEntry {
        LogLevel logLevel;
        String logText;
        T logTag;

        private String toHtml() {
            StringBuilder sb = new StringBuilder()
                    .append("<font color='")
                    .append(logLevel.toHtmlColor())
                    .append("'>")
                    .append("[")
                    .append(logTag.name())
                    .append("] ")
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

    private static void addEntry(T tag, LogLevel level, String text) {
        // skip if recording is disabled (default)
        if (!mEnableRecording)
            return;
        LogEntry entry = new LogEntry();
        entry.logLevel = level;
        entry.logText = text;
        entry.logTag = tag;
        mLogEntries.addEntry(entry);
    }

    /*
     * returns entire log as html for display (see AppLogViewerActivity)
     */
    public static String toHtml(Context context) {
        StringBuilder sb = new StringBuilder();

        // add version & device info
        sb.append("WordPress Android version: " + WordPress.getVersionName(context)).append("<br />")
          .append("Android device name: " + DeviceUtils.getInstance().getDeviceName(context)).append("<br />");

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
