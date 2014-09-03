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
    public static void updateCurrentUser(final ReaderActions.UpdateResultListener resultListener) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                final ReaderActions.UpdateResult result;
                if (jsonObject == null) {
                    result = ReaderActions.UpdateResult.FAILED;
                } else {
                    final ReaderUser serverUser = ReaderUser.fromJson(jsonObject);
                    final ReaderUser localUser = ReaderUserTable.getCurrentUser();
                    if (serverUser == null) {
                        result = ReaderActions.UpdateResult.FAILED;
                    } else if (serverUser.isSameUser(localUser)) {
                        result = ReaderActions.UpdateResult.UNCHANGED;
                    } else {
                        setCurrentUser(serverUser);
                        result = ReaderActions.UpdateResult.CHANGED;
                    }
                }

                if (resultListener != null)
                    resultListener.onUpdateResult(result);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.READER, volleyError);
                if (resultListener != null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };

        WordPress.getRestClientUtils().get("me", listener, errorListener);
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