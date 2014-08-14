package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;

public class UserPrefs {
    // id of the current user
    private static final String PREFKEY_USER_ID = "wp_userid";

    // name of last shown activity
    private static final String PREFKEY_LAST_ACTIVITY_STR = "wp_pref_last_activity_string";

    // last selected tag in the reader
    private static final String PREFKEY_READER_TAG_NAME = "reader_tag_name";
    private static final String PREFKEY_READER_TAG_TYPE = "reader_tag_type";

    // title of the last active page in ReaderSubsActivity
    private static final String PREFKEY_READER_SUBS_PAGE_TITLE = "reader_subs_page_title";

    // offset when showing recommended blogs
    private static final String PREFKEY_READER_RECOMMENDED_OFFSET = "reader_recommended_offset";

    private static SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
    }

    /*
     * remove all reader-related preferences
     */
    public static void reset() {
        prefs().edit()
               .remove(PREFKEY_USER_ID)
               .remove(PREFKEY_READER_TAG_NAME)
               .remove(PREFKEY_READER_TAG_TYPE)
               .remove(PREFKEY_READER_RECOMMENDED_OFFSET)
               .remove(PREFKEY_READER_SUBS_PAGE_TITLE)
               .apply();
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
        editor.apply();
    }

    private static long getLong(String key) {
        try {
            String value = getString(key);
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private static void setLong(String key, long value) {
        setString(key, Long.toString(value));
    }

    private static int getInt(String key) {
        try {
            String value = getString(key);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private static void setInt(String key, int value) {
        setString(key, Integer.toString(value));
    }

    private static void remove(String key) {
        prefs().edit().remove(key).apply();
    }

    public static long getCurrentUserId() {
        return getLong(PREFKEY_USER_ID);
    }
    public static void setCurrentUserId(long userId) {
        if (userId == 0) {
            remove(PREFKEY_USER_ID);
        } else {
            setLong(PREFKEY_USER_ID, userId);
        }
    }

    public static ReaderTag getReaderTag() {
        String tagName = getString(PREFKEY_READER_TAG_NAME);
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }
        int tagType = getInt(PREFKEY_READER_TAG_TYPE);
        return new ReaderTag(tagName, ReaderTagType.fromInt(tagType));
    }
    public static void setReaderTag(ReaderTag tag) {
        if (tag != null && !TextUtils.isEmpty(tag.getTagName())) {
            setString(PREFKEY_READER_TAG_NAME, tag.getTagName());
            setInt(PREFKEY_READER_TAG_TYPE, tag.tagType.toInt());
        } else {
            prefs().edit()
                   .remove(PREFKEY_READER_TAG_NAME)
                   .remove(PREFKEY_READER_TAG_TYPE)
                   .apply();
        }
    }

    /*
     * offset used along with a SQL LIMIT to enable user to page through recommended blogs
     */
    public static int getReaderRecommendedBlogOffset() {
        return getInt(PREFKEY_READER_RECOMMENDED_OFFSET);
    }
    public static void setReaderRecommendedBlogOffset(int offset) {
        if (offset == 0) {
            remove(PREFKEY_READER_RECOMMENDED_OFFSET);
        } else {
            setInt(PREFKEY_READER_RECOMMENDED_OFFSET, offset);
        }
    }

    /*
     * title of the last active page in ReaderSubsActivity - this is stored rather than
     * the index of the page so we can re-order pages without affecting this value
     */
    public static String getReaderSubsPageTitle() {
        return getString(PREFKEY_READER_SUBS_PAGE_TITLE);
    }
    public static void setReaderSubsPageTitle(String pageTitle) {
        setString(PREFKEY_READER_SUBS_PAGE_TITLE, pageTitle);
    }

    /*
     * name of the last shown activity - used at startup to restore the previously selected
     * activity, also used by analytics tracker
     */
    public static String getLastActivityStr() {
        return getString(PREFKEY_LAST_ACTIVITY_STR, ActivityId.UNKNOWN.name());
    }
    public static void setLastActivityStr(String value) {
        setString(PREFKEY_LAST_ACTIVITY_STR, value);
    }
    public static void resetLastActivityStr() {
        remove(PREFKEY_LAST_ACTIVITY_STR);
    }
}
