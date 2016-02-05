package org.wordpress.android.ui.prefs;

import android.preference.ListPreference;

import com.android.volley.VolleyError;

public class PrefsEvents {
    public static class AccountSettingsFetchSuccess {}
    public static class AccountSettingsPostSuccess {}
    public static class AccountSettingsFetchError {
        public final VolleyError mVolleyError;
        public AccountSettingsFetchError(VolleyError error) {
            mVolleyError = error;
        }
    }
    public static class AccountSettingsPostError {
        public final VolleyError mVolleyError;
        public AccountSettingsPostError(VolleyError error) {
            mVolleyError = error;
        }
    }
    public static class ListPreferenceDialogClosed {
        public final ListPreference preference;
        public final boolean positiveResult;
        public final String startingValue;
        public final String newValue;
        public ListPreferenceDialogClosed(ListPreference preference, boolean positiveResult, String startingValue, String newValue) {
            this.preference = preference;
            this.positiveResult = positiveResult;
            this.startingValue = startingValue;
            this.newValue = newValue;
        }
    }
}
