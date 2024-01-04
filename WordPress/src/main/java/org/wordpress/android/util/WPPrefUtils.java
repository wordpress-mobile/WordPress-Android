package org.wordpress.android.util;

import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

/**
 * Design guidelines for Calypso-styled Site Settings (and likely other screens)
 */

public class WPPrefUtils {
    /**
     * Gets a preference and sets the {@link android.preference.Preference.OnPreferenceChangeListener}.
     */
    public static Preference getPrefAndSetClickListener(PreferenceFragment prefFrag,
                                                        int id,
                                                        Preference.OnPreferenceClickListener listener) {
        Preference pref = prefFrag.findPreference(prefFrag.getString(id));
        if (pref != null) {
            pref.setOnPreferenceClickListener(listener);
        }
        return pref;
    }

    /**
     * Gets a preference and sets the {@link android.preference.Preference.OnPreferenceChangeListener}.
     */
    public static Preference getPrefAndSetChangeListener(PreferenceFragment prefFrag,
                                                         int id,
                                                         Preference.OnPreferenceChangeListener listener) {
        Preference pref = prefFrag.findPreference(prefFrag.getString(id));
        if (pref != null) {
            pref.setOnPreferenceChangeListener(listener);
        }
        return pref;
    }

    /**
     * Removes a {@link Preference} from the {@link PreferenceCategory} with the given key.
     */
    public static void removePreference(PreferenceFragment prefFrag, int parentKey, int prefKey) {
        String parentName = prefFrag.getString(parentKey);
        String prefName = prefFrag.getString(prefKey);
        PreferenceGroup parent = (PreferenceGroup) prefFrag.findPreference(parentName);
        Preference child = prefFrag.findPreference(prefName);

        if (parent != null && child != null) {
            parent.removePreference(child);
        }
    }
}
