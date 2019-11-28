package org.wordpress.android.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import org.wordpress.android.util.datasets.LogDatabase;
import org.wordpress.android.util.datasets.LogTable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;



import static java.lang.String.format;

/**
 * simple wrapper for Android log calls, enables recording and displaying log
 */
public class AppLog {
    // T for Tag
    public enum T {
        READER,
        EDITOR,
        MEDIA,
        NUX,
        API,
        STATS,
        UTILS,
        NOTIFS,
        DB,
        POSTS,
        PAGES,
        COMMENTS,
        THEMES,
        TESTS,
        PROFILING,
        SIMPERIUM,
        SUGGESTION,
        MAIN,
        SETTINGS,
        PLANS,
        PEOPLE,
        SHARING,
        PLUGINS,
        ACTIVITY_LOG,
        JETPACK_REMOTE_INSTALL,
        SUPPORT,
        SITE_CREATION,
        DOMAIN_REGISTRATION,
        APP_SESSION_END
    }

    public static final String TAG = "WordPress";
    public static final int HEADER_LINE_COUNT = 2;
    private static boolean mEnableRecording = false;
    private static List<AppLogListener> mListeners = new ArrayList<>(0);
    private static TimeZone mUtcTimeZone = TimeZone.getTimeZone("UTC");

    private static Context mContext;
    private static long mCurrentLogSessionId = -1;
    private static LogEntryList mLogEntries = new LogEntryList();

    private AppLog() {
        throw new AssertionError();
    }

    /**
     * Capture log so it can be displayed by AppLogViewerActivity
     * @param enable A boolean flag to capture log. Default is false, pass true to enable recording
     */
    public static void enableRecording(boolean enable, Context context) {
        mEnableRecording = enable;
        mContext = context;
        if (enable && mLogEntries.isEmpty()) {
            final SQLiteDatabase logDb = LogDatabase.getWritableDb(mContext);

            mCurrentLogSessionId = LogTable.getNewLogSessionId(logDb,
                    getAppInfoHeaderText(mContext),
                    getDeviceInfoHeaderText(mContext));

            ArrayList<LogTable.LogTableSessionData> dataList = LogTable.getData(logDb);
            for (LogTable.LogTableSessionData data : dataList) {
                addSessionData(data);
            }
        }
    }

    public static void addListener(@NonNull AppLogListener listener) {
        mListeners.add(listener);
    }

    public static void removeListeners() {
        mListeners.clear();
    }

    public interface AppLogListener {
        void onLog(T tag, LogLevel logLevel, String message);
    }

    /**
     * Sends a VERBOSE log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void v(T tag, String message) {
        message = StringUtils.notNullStr(message);
        Log.v(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.v, message);
    }

    /**
     * Sends a DEBUG log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void d(T tag, String message) {
        message = StringUtils.notNullStr(message);
        Log.d(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.d, message);
    }

    /**
     * Sends a INFO log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void i(T tag, String message) {
        message = StringUtils.notNullStr(message);
        Log.i(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.i, message);
    }

    /**
     * Sends a WARN log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void w(T tag, String message) {
        message = StringUtils.notNullStr(message);
        Log.w(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.w, message);
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void e(T tag, String message) {
        message = StringUtils.notNullStr(message);
        Log.e(TAG + "-" + tag.toString(), message);
        addEntry(tag, LogLevel.e, message);
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr An exception to log
     */
    public static void e(T tag, String message, Throwable tr) {
        message = StringUtils.notNullStr(message);
        Log.e(TAG + "-" + tag.toString(), message, tr);
        addEntry(tag, LogLevel.e, message + " - exception: " + tr.getMessage());
        addEntry(tag, LogLevel.e, "StackTrace: " + getStringStackTrace(tr));
    }

    /**
     * Sends a ERROR log message and the exception with StackTrace
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     *           log call occurs.
     * @param tr An exception to log to get StackTrace
     */
    public static void e(T tag, Throwable tr) {
        Log.e(TAG + "-" + tag.toString(), tr.getMessage(), tr);
        addEntry(tag, LogLevel.e, tr.getMessage());
        addEntry(tag, LogLevel.e, "StackTrace: " + getStringStackTrace(tr));
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     *           log call occurs.
     * @param volleyErrorMsg
     * @param statusCode
     */
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

    public enum LogLevel {
        v, d, i, w, e;

        private String toHtmlColor() {
            switch (this) {
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
        final LogLevel mLogLevel;
        final String mLogText;
        final java.util.Date mDate;
        final T mLogTag;

        LogEntry(LogLevel logLevel, String logText, T logTag) {
            this(logLevel, logText, logTag, new Date());
        }

        LogEntry(LogLevel logLevel, String logText, T logTag, java.util.Date date) {
            mLogLevel = logLevel;
            mDate = date;
            if (logText == null) {
                mLogText = "null";
            } else {
                mLogText = logText;
            }
            mLogTag = logTag;
        }

        private String formatLogDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM-dd kk:mm", Locale.US);
            sdf.setTimeZone(mUtcTimeZone);
            return sdf.format(mDate);
        }

        private String toHtml() {
            StringBuilder sb = new StringBuilder();
            sb.append("<font color=\"");
            sb.append(mLogLevel.toHtmlColor());
            sb.append("\">");
            sb.append("[");
            sb.append(formatLogDate()).append(" ");
            sb.append(mLogTag.name()).append(" ");
            sb.append(mLogLevel.name());
            sb.append("] ");
            sb.append(TextUtils.htmlEncode(mLogText).replace("\n", "<br />"));
            sb.append("</font>");
            return sb.toString();
        }
    }

    private static class LogEntryList extends ArrayList<LogEntry> {
        private synchronized boolean addEntry(LogEntry entry) {
            if (size() >= MAX_ENTRIES) {
                removeFirstEntry();
            }
            return add(entry);
        }

        private void removeFirstEntry() {
            Iterator<LogEntry> it = iterator();
            if (!it.hasNext()) {
                return;
            }
            try {
                remove(it.next());
            } catch (NoSuchElementException e) {
                // ignore
            }
        }
    }

    private static void addSessionData(final LogTable.LogTableSessionData tableLogSessionData) {
        final long sessionId = tableLogSessionData.mSessionId;
        final String appInfoHeader = tableLogSessionData.mAppInfoHeader;
        final String deviceInfoHeader = tableLogSessionData.mDeviceInfoHeader;

        java.util.Date date = new Date();
        for (LogTable.LogTableEntry logTableEntry : tableLogSessionData.mLogEntries) {
            date = logTableEntry.mDate;
            LogEntry entry = new LogEntry(LogLevel.valueOf(logTableEntry.mLogLevel),
                    logTableEntry.mLogText,
                    T.valueOf(logTableEntry.mLogTag),
                    logTableEntry.mDate);
            mLogEntries.addEntry(entry);
        }

        if (sessionId != mCurrentLogSessionId) {
            addAppSessionEndedLogEntry(sessionId, appInfoHeader, deviceInfoHeader, date);
        }
    }

    private static void addAppSessionEndedLogEntry(final long sessionId,
            final String appInfoHeader,
            final String deviceInfoHeader,
            final java.util.Date date) {
        StringBuilder sb = new StringBuilder();
        sb.append("Previous App Session Ended - ");
        sb.append(appInfoHeader).append(" - ");
        sb.append(deviceInfoHeader).append(" - ");
        sb.append("Session id: ").append(sessionId);

        LogEntry entry = new LogEntry(LogLevel.i, sb.toString(), T.APP_SESSION_END);
        mLogEntries.addEntry(entry);
    }

    private static void addEntry(T tag, LogLevel level, String text) {
        // Call our listeners if any
        for (AppLogListener listener : mListeners) {
            listener.onLog(tag, level, text);
        }
        // Record entry if enabled
        if (mEnableRecording) {
            LogEntry entry = new LogEntry(level, text, tag);
            mLogEntries.addEntry(entry);
            persistLogEntry(entry);
        }
    }

    private static void persistLogEntry(final LogEntry entry) {
        final SQLiteDatabase logDb = LogDatabase.getWritableDb(mContext);
        LogTable.addLogEntry(logDb,
                mCurrentLogSessionId,
                entry.mLogLevel.name(),
                entry.mLogTag.name(),
                entry.mLogText,
                entry.mDate);
    }

    private static String getStringStackTrace(Throwable throwable) {
        StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }


    private static String getAppInfoHeaderText(Context context) {
        StringBuilder sb = new StringBuilder();
        PackageManager packageManager = context.getPackageManager();
        PackageInfo pkInfo = PackageUtils.getPackageInfo(context);

        ApplicationInfo applicationInfo = pkInfo != null ? pkInfo.applicationInfo : null;
        String appName;
        if (applicationInfo != null && packageManager.getApplicationLabel(applicationInfo) != null) {
            appName = packageManager.getApplicationLabel(applicationInfo).toString();
        } else {
            appName = "Unknown";
        }
        sb.append(appName).append(" - ").append(PackageUtils.getVersionName(context))
          .append(" - Version code: ").append(PackageUtils.getVersionCode(context));
        return sb.toString();
    }

    private static String getDeviceInfoHeaderText(Context context) {
        return "Android device name: " + DeviceUtils.getInstance().getDeviceName(context);
    }

    /**
     * Returns entire log as html for display (see AppLogViewerActivity)
     * @return Arraylist of Strings containing log messages
     */
    public static ArrayList<String> toHtmlList() {
        ArrayList<String> items = new ArrayList<String>();

        // add version & device info - be sure to change HEADER_LINE_COUNT if additional lines are added
        items.add("<strong>" + getAppInfoHeaderText(mContext) + "</strong>");
        items.add("<strong>" + getDeviceInfoHeaderText(mContext) + "</strong>");

        Iterator<LogEntry> it = new ArrayList<>(mLogEntries).iterator();
        while (it.hasNext()) {
            items.add(it.next().toHtml());
        }
        return items;
    }

    /**
     * Converts the entire log to plain text
     * @return The log as plain text
     */
    public static synchronized String toPlainText() {
        StringBuilder sb = new StringBuilder();

        // add version & device info
        sb.append(getAppInfoHeaderText(mContext)).append("\n")
          .append(getDeviceInfoHeaderText(mContext)).append("\n\n");

        Iterator<LogEntry> it = new ArrayList<>(mLogEntries).iterator();
        int lineNum = 1;
        while (it.hasNext()) {
            LogEntry entry = it.next();
            sb.append(format(Locale.US, "%02d - ", lineNum))
              .append("[")
              .append(entry.formatLogDate()).append(" ")
              .append(entry.mLogTag.name())
              .append("] ")
              .append(entry.mLogText)
              .append("\n");
            lineNum++;
        }
        return sb.toString();
    }
}
