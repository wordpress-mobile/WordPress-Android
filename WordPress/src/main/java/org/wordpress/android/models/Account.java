package org.wordpress.android.models;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.AccountTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.xmlrpc.android.ApiHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for managing logged in user informations.
 */
public class Account extends AccountModel {
    public void fetchAccountDetails() {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    updateFromRestResponse(jsonObject);
                    save();

                    ReaderUserTable.addOrUpdateUser(ReaderUser.fromJson(jsonObject));
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
            }
        };

        WordPress.getRestClientUtilsV1_1().get("me", listener, errorListener);
    }

    public void fetchAccountSettings(final ApiHelper.GenericCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    updateAccountSettingsFromRestResponse(jsonObject);
                    save();
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
            }
        };

        WordPress.getRestClientUtilsV1_1().get("me/settings", listener, errorListener);
    }

    public void postAccountSettings(Map<String, String> params, final ApiHelper.GenericCallback callback) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    updateAccountSettingsFromRestResponse(jsonObject);
                    save();
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }
            }
        };

        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.API, volleyError);
            }
        };

        WordPress.getRestClientUtilsV1_1().post("me/settings", params, null, listener, errorListener);
    }

    public void signout() {
        init();
        save();
    }

    public void save() {
        AccountTable.save(this);
    }

    public enum RestParam {
        FIRST_NAME,
        LAST_NAME,
        DISPLAY_NAME,
        ABOUT_ME;

        public static String toString(RestParam param) {
            switch (param) {
                case FIRST_NAME:
                    return "first_name";
                case LAST_NAME:
                    return "last_name";
                case DISPLAY_NAME:
                    return "display_name";
                case ABOUT_ME:
                    return "description";
            }
        }
    }
}
