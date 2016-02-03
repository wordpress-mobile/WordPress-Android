package org.wordpress.android.ui.prefs;

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
}
