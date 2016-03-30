package org.wordpress.android.ui.people.utils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class PeopleUtils {

    public static void fetchUsers(String siteId, final PeopleUtils.Callback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null && callback != null) {
                    callback.onSuccess(jsonObject);
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
                if (callback != null) {
                    callback.onError(volleyError);
                }
            }
        };

        String path = String.format("sites/%s/users", siteId);
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    public interface Callback {
        void onSuccess(JSONObject jsonObject);

        void onError(VolleyError error);
    }
}
