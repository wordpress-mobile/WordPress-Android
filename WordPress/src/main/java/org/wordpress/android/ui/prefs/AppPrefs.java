package org.wordpress.android.ui.prefs;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.stats.StatsTimeframe;

public class AppPrefs {
    public enum PrefKey {
        // name of last shown activity
        LAST_ACTIVITY_STR,

        // last selected tag in the reader
        READER_TAG_NAME,
        READER_TAG_TYPE,

        // title of the last active page in ReaderSubsActivity
        READER_SUBS_PAGE_TITLE,

        // email retrieved and attached to mixpanel profile
        MIXPANEL_EMAIL_ADDRESS,

        // index of the last active tab in main activity
        MAIN_TAB_INDEX,

        // index of the last active item in Stats activity
        STATS_ITEM_INDEX,
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

    /**
     * title of the last active page in ReaderSubsActivity - this is stored rather than
     * the index of the page so we can re-order pages without affecting this value
     */
    public static String getReaderSubsPageTitle() {
        return getString(PrefKey.READER_SUBS_PAGE_TITLE);
    }
    public static void setReaderSubsPageTitle(String pageTitle) {
        setString(PrefKey.READER_SUBS_PAGE_TITLE, pageTitle);
    }

    public static StatsTimeframe getStatsTimeframe() {
        int idx = getInt(PrefKey.STATS_ITEM_INDEX);
        StatsTimeframe[] timeframeValues = StatsTimeframe.values();
        if (timeframeValues.length < idx) {
            return timeframeValues[0];
        } else {
            return timeframeValues[idx];
        }
    }
    public static void setStatsTimeframe(StatsTimeframe timeframe) {
        if (timeframe != null) {
            setInt(PrefKey.STATS_ITEM_INDEX, timeframe.ordinal());
        } else {
            prefs().edit()
                    .remove(PrefKey.STATS_ITEM_INDEX.name())
                    .apply();
        }
    }


    /**
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

    public static String getMixpanelUserEmail() {
        return getString(PrefKey.MIXPANEL_EMAIL_ADDRESS, null);
    }

    public static void setMixpanelUserEmail(String email) {
        setString(PrefKey.MIXPANEL_EMAIL_ADDRESS, email);
    }

    public static int getMainTabIndex() {
        return getInt(PrefKey.MAIN_TAB_INDEX);
    }
    public static void setMainTabIndex(int index) {
        setInt(PrefKey.MAIN_TAB_INDEX, index);
    }
}
