package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.WordPress;

/**
 * Created by nbradbury on 6/21/13.
 */
public class UserPrefs {
    private static final String PREFKEY_USER_ID    = "wp_userid";       // id of the current user
    private static final String PREFKEY_READER_TAG = "reader_tag";    // last selected tag in the reader

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    /*
     * remove all reader-related preferences
     */
    public static void reset() {
        SharedPreferences.Editor editor = prefs().edit();
        editor.remove(PREFKEY_USER_ID);
        editor.remove(PREFKEY_READER_TAG);
        editor.commit();
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
    public static boolean hasCurrentUserId() {
        return (getCurrentUserId() != 0);
    }

    public static String getReaderTag() {
        return getString(PREFKEY_READER_TAG);
    }
    public static void setReaderTag(String tagName) {
        setString(PREFKEY_READER_TAG, tagName);
    }
}
