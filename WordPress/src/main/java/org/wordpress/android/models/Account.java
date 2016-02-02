package org.wordpress.android.models;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.AccountTable;
import org.wordpress.android.datasets.ReaderUserTable;
import org.wordpress.android.ui.prefs.PrefsEvents;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Map;

import de.greenrobot.event.EventBus;

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

    public void fetchAccountSettings() {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    updateAccountSettingsFromRestResponse(jsonObject);
                    save();
                    EventBus.getDefault().post(new PrefsEvents.AccountSettingsChanged());
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

    public void postAccountSettings(Map<String, String> params) {
        com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (jsonObject != null) {
                    updateAccountSettingsFromRestResponse(jsonObject);
                    save();
                    EventBus.getDefault().post(new PrefsEvents.AccountSettingsChanged());
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
        FIRST_NAME("first_name"),
        LAST_NAME("last_name"),
        DISPLAY_NAME("display_name"),
        ABOUT_ME("description"),
        EMAIL("user_email"),
        NEW_EMAIL("new_user_email"),
        EMAIL_CHANGE_PENDING("user_email_change_pending"),
        PRIMARY_BLOG("primary_site_ID"),
        WEB_ADDRESS("user_URL");

        private String description;

        RestParam(String description) {
            this.description = description;
        }

        public static String toString(RestParam param) {
            return param.description;
        }
    }
}
