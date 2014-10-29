package org.wordpress.android.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * simple wrapper for Android log calls, enables recording & displaying log
 */
public class AppLog {
    // T for Tag
    public enum T {READER, EDITOR, MEDIA, NUX, API, STATS, UTILS, NOTIFS, DB, POSTS, COMMENTS, THEMES, TESTS, PROFILING, SIMPERIUM, SUGGESTION}
    public static final String TAG = "WordPress";
    public static final int HEADER_LINE_COUNT = 2;

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
        addEntry(tag, LogLevel.e, "StackTrace: " + getHTMLStringStackTrace(tr));
    }

    public static void e(T tag, Throwable tr) {
        Log.e(TAG + "-" + tag.toString(), tr.getMessage(), tr);
        addEntry(tag, LogLevel.e, tr.getMessage());
        addEntry(tag, LogLevel.e, "StackTrace: " + getHTMLStringStackTrace(tr));
    }

    public static void e(T tag, String volleyErrorMsg, int statusCode) {
        if (TextUtils.isEmpty(volleyErrorMsg)) {
            return;
        }
        String logText;
        if (statusCode == -1) {
            logText = volleyErrorMsg;
        } else {
            logText = volleyErrorMsg + ", status " + statusCode;
        }
        Log.e(TAG + "-" + tag.toString(), logText);
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

    private static String getStringStackTrace(Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    private static String getHTMLStringStackTrace(Throwable throwable) {
        return getStringStackTrace(throwable).replace("\n", "<br/>");
    }

    /*
     * returns entire log as html for display (see AppLogViewerActivity)
     */
    public static ArrayList<String> toHtmlList(Context context) {
        ArrayList<String> items = new ArrayList<String>();

        // add version & device info - be sure to change HEADER_LINE_COUNT if additional lines are added
        items.add("<strong>WordPress Android version: " + PackageUtils.getVersionName(context) + "</strong>");
        items.add("<strong>Android device name: " + DeviceUtils.getInstance().getDeviceName(context) + "</strong>");

        Iterator<LogEntry> it = mLogEntries.iterator();
        while (it.hasNext()) {
            items.add(it.next().toHtml());
        }
        return items;
    }


    /*
     * returns entire log as plain text
     */
    public static String toPlainText(Context context) {
        StringBuilder sb = new StringBuilder();

        // add version & device info
        sb.append("WordPress Android version: " + PackageUtils.getVersionName(context)).append("\n")
          .append("Android device name: " + DeviceUtils.getInstance().getDeviceName(context)).append("\n\n");

        Iterator<LogEntry> it = mLogEntries.iterator();
        int lineNum = 1;
        while (it.hasNext()) {
              sb.append(String.format("%02d - ", lineNum))
              .append(it.next().logText)
              .append("\n");
            lineNum++;
        }
        return sb.toString();
    }
}
