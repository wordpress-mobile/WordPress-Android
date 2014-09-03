package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;

public class AppPrefs {
    public enum PrefKey {
        // id of the current user
        USER_ID,

        // name of last shown activity
        LAST_ACTIVITY_STR,

        // last selected tag in the reader
        READER_TAG_NAME,
        READER_TAG_TYPE,

        // title of the last active page in ReaderSubsActivity
        READER_SUBS_PAGE_TITLE,

        // offset when showing recommended blogs
        READER_RECOMMENDED_OFFSET,

        // email retrieved and attached to mixpanel profile
        MIXPANEL_EMAIL_ADDRESS_RETRIEVED,
    }

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    private static String getString(PrefKey key) {
        return getString(key, "");
    }
    private static String getString(PrefKey key, String defaultValue) {
        return prefs().getString(key.name(), defaultValue);
    }
    private static void setString(PrefKey key, String value) {
        SharedPreferences.Editor editor = prefs().edit();
        if (TextUtils.isEmpty(value)) {
            editor.remove(key.name());
        } else {
            editor.putString(key.name(), value);
        }
        editor.apply();
    }

    private static long getLong(PrefKey key) {
        try {
            String value = getString(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private static void setLong(PrefKey key, long value) {
        setString(key, Long.toString(value));
    }

    private static int getInt(PrefKey key) {
        try {
            String value = getString(key);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private static void setInt(PrefKey key, int value) {
        setString(key, Integer.toString(value));
    }

    private static boolean getBoolean(PrefKey key, boolean def) {
         String value = getString(key, Boolean.toString(def));
         return Boolean.parseBoolean(value);
    }

    private static void setBoolean(PrefKey key, boolean value) {
        setString(key, Boolean.toString(value));
    }

    private static void remove(PrefKey key) {
        prefs().edit().remove(key.name()).apply();
    }

    // Exposed methods

    /**
     * remove all user-related preferences
     */
    public static void reset() {
        SharedPreferences.Editor editor = prefs().edit();
        for (PrefKey key : PrefKey.values()) {
            editor.remove(key.name());
        }
        editor.apply();
    }

    public static long getCurrentUserId() {
        return getLong(PrefKey.USER_ID);
    }
    public static void setCurrentUserId(long userId) {
        if (userId == 0) {
            remove(PrefKey.USER_ID);
        } else {
            setLong(PrefKey.USER_ID, userId);
        }
    }

    public static ReaderTag getReaderTag() {
        String tagName = getString(PrefKey.READER_TAG_NAME);
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }
        int tagType = getInt(PrefKey.READER_TAG_TYPE);
        return new ReaderTag(tagName, ReaderTagType.fromInt(tagType));
    }
    public static void setReaderTag(ReaderTag tag) {
        if (tag != null && !TextUtils.isEmpty(tag.getTagName())) {
            setString(PrefKey.READER_TAG_NAME, tag.getTagName());
            setInt(PrefKey.READER_TAG_TYPE, tag.tagType.toInt());
        } else {
            prefs().edit()
                   .remove(PrefKey.READER_TAG_NAME.name())
                   .remove(PrefKey.READER_TAG_TYPE.name())
                   .apply();
        }
    }

    /*
     * offset used along with a SQL LIMIT to enable user to page through recommended blogs
     */
    public static int getReaderRecommendedBlogOffset() {
        return getInt(PrefKey.READER_RECOMMENDED_OFFSET);
    }
    public static void setReaderRecommendedBlogOffset(int offset) {
        if (offset == 0) {
            remove(PrefKey.READER_RECOMMENDED_OFFSET);
        } else {
            setInt(PrefKey.READER_RECOMMENDED_OFFSET, offset);
        }
    }

    /*
     * title of the last active page in ReaderSubsActivity - this is stored rather than
     * the index of the page so we can re-order pages without affecting this value
     */
    public static String getReaderSubsPageTitle() {
        return getString(PrefKey.READER_SUBS_PAGE_TITLE);
    }
    public static void setReaderSubsPageTitle(String pageTitle) {
        setString(PrefKey.READER_SUBS_PAGE_TITLE, pageTitle);
    }

    /*
     * name of the last shown activity - used at startup to restore the previously selected
     * activity, also used by analytics tracker
     */
    public static String getLastActivityStr() {
        return getString(PrefKey.LAST_ACTIVITY_STR, ActivityId.UNKNOWN.name());
    }
    public static void setLastActivityStr(String value) {
        setString(PrefKey.LAST_ACTIVITY_STR, value);
    }
    public static void resetLastActivityStr() {
        remove(PrefKey.LAST_ACTIVITY_STR);
    }


    // Mixpanel email retrieval check

    public static Boolean getMixpanelEmailRetrievalCheck() {
        return getBoolean(PrefKey.MIXPANEL_EMAIL_ADDRESS_RETRIEVED, false);
    }

    public static void setMixpanelEmailRetrievalCheck(boolean b) {
        setBoolean(PrefKey.MIXPANEL_EMAIL_ADDRESS_RETRIEVED, b);
    }
}
