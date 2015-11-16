package org.wordpress.android.ui.publicize;

import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.PublicizeEvents.ActionCompleted;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * API calls to connect/disconnect/reconnect publicize services
 */
class PublicizeActions {

    public static void disconnect(final PublicizeConnection connection) {
        String path = String.format(
                "sites/%d/publicize-connections/%d/delete", connection.siteId, connection.connectionId);

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.SHARING, "disconnect succeeded");
                PublicizeTable.deleteConnection(connection.connectionId);
                EventBus.getDefault().post(new ActionCompleted(true, ConnectAction.DISCONNECT));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.DISCONNECT));
            }
        };

        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    public static void connect(int siteId, String serviceId){
        if (TextUtils.isEmpty(serviceId)) {
            AppLog.w(AppLog.T.SHARING, "cannot connect without service");
            EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT));
            return;
        }

        connectStepOne(siteId, serviceId);
    }

    /*
     * step one in creating a publicize connection: request the list of keyring connections
     * and find the one for the passed service
     */
    private static void connectStepOne(final int siteId, final String serviceId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                int keyringConnectionId = parseServiceKeyringId(serviceId, jsonObject);
                connectStepTwo(siteId, keyringConnectionId);
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT));
            }
        };

        String path = "/me/keyring-connections";
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    /*
     * step two in creating a publicize connection: now that we have the keyring connection id,
     * create the actual connection
     */
    private static void connectStepTwo(int siteId, int keyringConnectionId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.SHARING, "connect succeeded");
                PublicizeConnection connection = PublicizeConnection.fromJson(jsonObject);
                PublicizeTable.addOrUpdateConnection(connection);
                EventBus.getDefault().post(new ActionCompleted(true, ConnectAction.CONNECT));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT));
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("keyring_connection_ID", Integer.toString(keyringConnectionId));
        String path = String.format("/sites/%d/publicize-connections/new", siteId);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    /*
     * extract the keyring connection for the passed service from the response
     * to /me/keyring-connections
     */
    private static int parseServiceKeyringId(String serviceId, JSONObject json) {
        JSONArray jsonConnectionList = json.optJSONArray("connections");
        if (jsonConnectionList == null) {
            return 0;
        }

        long currentUserId = AccountHelper.getDefaultAccount().getUserId();
        for (int i = 0; i < jsonConnectionList.length(); i++) {
            JSONObject jsonConnection = jsonConnectionList.optJSONObject(i);
            String service = JSONUtils.getString(jsonConnection, "service");
            if (serviceId.equals(service)) {
                // make sure userId matches the current user, or is zero (shared)
                long userId = jsonConnection.optLong("user_ID");
                if (userId == 0 || userId == currentUserId) {
                    int keyringId = jsonConnection.optInt("ID");
                    return keyringId;
                }
            }
        }

        return 0;
    }
}
