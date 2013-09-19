package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.WordPress;

/**
 * Created by nbradbury on 6/21/13.
 */
public class ReaderPrefs {
    private static final String PREFKEY_USER_ID      = "wp_userid";       // id of the current user
    private static final String PREFKEY_READER_TOPIC = "reader_topic";    // last selected topic in the reader

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    private static String getString(String key) {
        return getString(key, "");
    }
    private static String getString(String key, String defaultValue) {
        return prefs().getString(key, defaultValue);
    }
    private static void setString(String key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        if (TextUtils.isEmpty(value)) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.commit();
    }

    private static void remove(String key) {
        prefs().edit().remove(key).commit();
    }

    // TODO: WPAndroid should store the current user's ID at login and clear it at logout
    public static long getCurrentUserId() {
        try {
            String value = getString(PREFKEY_USER_ID);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static void setCurrentUserId(long userId) {
        if (userId==0) {
            remove(PREFKEY_USER_ID);
        } else {
            setString(PREFKEY_USER_ID, Long.toString(userId));
        }
    }
    public static void clearCurrentUserId() {
        remove(PREFKEY_USER_ID);
    }

    public static String getReaderTopic() {
        return getString(PREFKEY_READER_TOPIC);
    }
    public static void setReaderTopic(String topicName) {
        setString(PREFKEY_READER_TOPIC, topicName);
    }
}
