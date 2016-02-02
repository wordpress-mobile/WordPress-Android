package org.wordpress.android.ui.prefs;

import com.android.volley.VolleyError;

public class PrefsEvents {
    public static class MyProfileDetailsChanged {}
    public static class MyProfileDataLoadSaveError {
        public final VolleyError mVolleyError;
        public MyProfileDataLoadSaveError(VolleyError error) {
            mVolleyError = error;
        }
    }
}
