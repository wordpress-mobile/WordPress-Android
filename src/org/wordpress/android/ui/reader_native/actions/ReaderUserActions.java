package org.wordpress.android.ui.reader_native.actions;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.wordpress.android.util.ReaderLog;

/**
 * Created by nbradbury on 8/25/13.
 */
public class ReaderUserActions {

    public static void updateCurrentUser(final ReaderActions.UpdateResultListener resultListener) {
        final ReaderUser localUser = ReaderUserTable.getCurrentUser();

        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                final ReaderActions.UpdateResult result;
                ReaderUser serverUser = ReaderUser.fromJson(jsonObject);
                if (serverUser!=null) {
                    if (serverUser.isSameUser(localUser)) {
                        result = ReaderActions.UpdateResult.UNCHANGED;
                    } else {
                        // add logged in user to user table and store the userId in prefs
                        ReaderUserTable.addOrUpdateUser(serverUser);
                        ReaderPrefs.setCurrentUserId(serverUser.userId);
                        result = ReaderActions.UpdateResult.CHANGED;
                    }
                } else {
                    result = ReaderActions.UpdateResult.FAILED;
                }
                if (resultListener!=null)
                    resultListener.onUpdateResult(result);
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                ReaderLog.e(volleyError);
                if (resultListener!=null)
                    resultListener.onUpdateResult(ReaderActions.UpdateResult.FAILED);
            }
        };

        WordPress.restClient.get("me", listener, errorListener);
    }
}
