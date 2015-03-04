package org.wordpress.android.ui.reader.actions;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class ReaderUserActions {
    /*
     * request the current user's info, update locally if different than existing local
     */
    public static void updateCurrentUser() {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    final ReaderUser serverUser = ReaderUser.fromJson(jsonObject);
                    final ReaderUser localUser = ReaderUserTable.getCurrentUser();
                    if (serverUser != null && !serverUser.isSameUser(localUser)) {
                        setCurrentUser(serverUser);
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };

        WordPress.getRestClientUtilsV1_1().get("me", listener, errorListener);
    }

    /*
     * set the passed user as the current user in both the local db and prefs
     */
    public static void setCurrentUser(JSONObject jsonUser) {
        if (jsonUser == null)
            return;
        setCurrentUser(ReaderUser.fromJson(jsonUser));
    }
    private static void setCurrentUser(ReaderUser user) {
        if (user == null)
            return;
        ReaderUserTable.addOrUpdateUser(user);
        AppPrefs.setCurrentUserId(user.userId);
    }
}