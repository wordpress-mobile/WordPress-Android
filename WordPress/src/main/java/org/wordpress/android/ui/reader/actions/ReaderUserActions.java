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

    public static void updateCurrentUser() {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                setCurrentUser(ReaderUser.fromJson(jsonObject));
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
            }
        };

        WordPress.getRestClientUtils().get("me", listener, errorListener);
    }

    /*
     * set the passed user as the current user in both the local db and prefs
     */
    public static void setCurrentUser(JSONObject jsonUser) {
        if (jsonUser != null) {
            setCurrentUser(ReaderUser.fromJson(jsonUser));
        }
    }
    private static void setCurrentUser(ReaderUser user) {
        if (user != null) {
            ReaderUserTable.addOrUpdateUser(user);
            AppPrefs.setCurrentUserId(user.userId);
        }
    }
}