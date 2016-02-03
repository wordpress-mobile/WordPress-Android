package org.wordpress.android.ui.prefs;

import com.android.volley.VolleyError;

public class PrefsEvents {
    public static class AccountSettingsChanged {}
    public static class AccountSettingsDataLoadSaveError {
        public final VolleyError mVolleyError;
        public AccountSettingsDataLoadSaveError(VolleyError error) {
            mVolleyError = error;
        }
    }
}
