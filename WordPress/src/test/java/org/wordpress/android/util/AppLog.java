package org.wordpress.android.util;

import android.support.annotation.NonNull;

/**
 * simple wrapper for Android log calls, enables recording and displaying log
 *
 * Testing version:
 * - replaces Log calls with System.out, since the unit tests don't have access to Android Framework classes.
 * - removes methods with Context parameter
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
        SITE_CREATION
    }

    public static final String TAG = "WordPress";

    private AppLog() {
        throw new AssertionError();
    }

    /**
     * Capture log so it can be displayed by AppLogViewerActivity
     *
     * @param enable A boolean flag to capture log. Default is false, pass true to enable recording
     */
    public static void enableRecording(boolean enable) {
    }

    public static void addListener(@NonNull AppLogListener listener) {
    }

    public static void removeListeners() {
    }

    public interface AppLogListener {
        void onLog(T tag, LogLevel logLevel, String message);
    }

    /**
     * Sends a VERBOSE log message
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void v(T tag, String message) {
        message = StringUtils.notNullStr(message);
        System.out.println("v - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Sends a DEBUG log message
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void d(T tag, String message) {
        message = StringUtils.notNullStr(message);
        System.out.println("d - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Sends a INFO log message
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void i(T tag, String message) {
        message = StringUtils.notNullStr(message);
        System.out.println("i - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Sends a WARN log message
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void w(T tag, String message) {
        message = StringUtils.notNullStr(message);
        System.out.println("w - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Sends a ERROR log message
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    public static void e(T tag, String message) {
        message = StringUtils.notNullStr(message);
        System.out.println("e - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Send a ERROR log message and log the exception.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr      An exception to log
     */
    public static void e(T tag, String message, Throwable tr) {
        message = StringUtils.notNullStr(message);
        System.out.println("e - " + TAG + " - " + tag.toString() + " - " + message);
    }

    /**
     * Sends a ERROR log message and the exception with StackTrace
     *
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     *            log call occurs.
     * @param tr  An exception to log to get StackTrace
     */
    public static void e(T tag, Throwable tr) {
        System.out.println("e - " + TAG + " - " + tag.toString() + " - " + tr.getMessage());
    }

    /**
     * Sends a ERROR log message
     *
     * @param tag            Used to identify the source of a log message. It usually identifies the class or
     *                       activity where the
     *                       log call occurs.
     */
    public static void e(T tag, String volleyErrorMsg, int statusCode) {
        if (volleyErrorMsg == null || "".equals(volleyErrorMsg)) {
            return;
        }
        String logText;
        if (statusCode == -1) {
            logText = volleyErrorMsg;
        } else {
            logText = volleyErrorMsg + ", status " + statusCode;
        }
        System.out.println("e - " + TAG + " - " + tag.toString() + " - " + logText);
    }

    // --------------------------------------------------------------------------------------------------------


    public enum LogLevel {
        v, d, i, w, e
    }
}
