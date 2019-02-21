package org.wordpress.android.ui.publicize;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PublicizeTable;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.models.PublicizeService;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;
import org.wordpress.android.ui.publicize.PublicizeEvents.ActionCompleted;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.JSONUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * API calls to connect/disconnect publicize services
 */
public class PublicizeActions {
    public interface OnPublicizeActionListener {
        void onRequestConnect(PublicizeService service);

        void onRequestDisconnect(PublicizeConnection connection);

        void onRequestReconnect(PublicizeService service, PublicizeConnection connection);
    }

    /*
     * disconnect a currently connected publicize service
     */
    public static void disconnect(@NonNull final PublicizeConnection connection) {
        String path = String.format(Locale.ROOT,
                "sites/%d/publicize-connections/%d/delete", connection.siteId, connection.connectionId);

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.SHARING, "disconnect succeeded");
                EventBus.getDefault().post(new ActionCompleted(true, ConnectAction.DISCONNECT,
                        connection.getService()));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                PublicizeTable.addOrUpdateConnection(connection);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.DISCONNECT,
                        connection.getService()));
            }
        };

        // delete connection immediately - will be restored upon failure
        PublicizeTable.deleteConnection(connection.connectionId);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    public static void reconnect(@NonNull final PublicizeConnection connection) {
        String path = String.format(Locale.ROOT,
                "sites/%d/publicize-connections/%d/delete", connection.siteId, connection.connectionId);

        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.SHARING, "disconnect succeeded");
                EventBus.getDefault().post(new ActionCompleted(true, ConnectAction.RECONNECT,
                        connection.getService()));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                PublicizeTable.addOrUpdateConnection(connection);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.RECONNECT,
                        connection.getService()));
            }
        };

        // delete connection immediately - will be restored upon failure
        PublicizeTable.deleteConnection(connection.connectionId);
        WordPress.getRestClientUtilsV1_1().post(path, listener, errorListener);
    }

    /*
     * create a new publicize service connection for a specific site
     */
    public static void connect(long siteId, String serviceId, long currentUserId) {
        if (TextUtils.isEmpty(serviceId)) {
            AppLog.w(AppLog.T.SHARING, "cannot connect without service");
            EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT, serviceId));
            return;
        }

        connectStepOne(siteId, serviceId, currentUserId);
    }

    /*
     * step one in creating a publicize connection: request the list of keyring connections
     * and find the one for the passed service
     */
    private static void connectStepOne(final long siteId, final String serviceId, final long currentUserId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                if (shouldShowChooserDialog(siteId, serviceId, jsonObject)) {
                    // show dialog showing multiple options
                    EventBus.getDefault()
                            .post(new PublicizeEvents.ActionRequestChooseAccount(siteId, serviceId, jsonObject));
                } else {
                    long keyringConnectionId = parseServiceKeyringId(serviceId, currentUserId, jsonObject);
                    connectStepTwo(siteId, keyringConnectionId, serviceId, "");
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT, serviceId));
            }
        };

        String path = "/me/keyring-connections";
        WordPress.getRestClientUtilsV1_1().get(path, listener, errorListener);
    }

    /*
     * step two in creating a publicize connection: now that we have the keyring connection id,
     * create the actual connection
     */
    public static void connectStepTwo(final long siteId, long keyringConnectionId,
                                      final String serviceId, final String externalUserId) {
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                AppLog.d(AppLog.T.SHARING, "connect succeeded");
                PublicizeConnection connection = PublicizeConnection.fromJson(jsonObject);
                PublicizeTable.addOrUpdateConnection(connection);
                EventBus.getDefault().post(new ActionCompleted(true, ConnectAction.CONNECT, serviceId));
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.SHARING, volleyError);
                EventBus.getDefault().post(new ActionCompleted(false, ConnectAction.CONNECT, serviceId));
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("keyring_connection_ID", Long.toString(keyringConnectionId));
        if (!externalUserId.isEmpty()) {
            params.put("external_user_ID", externalUserId);
        }
        String path = String.format(Locale.ROOT, "/sites/%d/publicize-connections/new", siteId);
        WordPress.getRestClientUtilsV1_1().post(path, params, null, listener, errorListener);
    }

    private static boolean shouldShowChooserDialog(long siteId, String serviceId, JSONObject jsonObject) {
        JSONArray jsonConnectionList = jsonObject.optJSONArray("connections");

        if (jsonConnectionList == null || jsonConnectionList.length() <= 1) {
            return false;
        }

        int totalAccounts = 0;
        int totalExternalAccounts = 0;
        try {
            for (int i = 0; i < jsonConnectionList.length(); i++) {
                JSONObject connectionObject = jsonConnectionList.getJSONObject(i);
                PublicizeConnection publicizeConnection = PublicizeConnection.fromJson(connectionObject);
                if (publicizeConnection.getService().equals(serviceId) && !publicizeConnection.isInSite(siteId)) {
                    totalAccounts++;
                    JSONArray externalJsonArray = connectionObject.getJSONArray("additional_external_users");
                    for (int j = 0; j < externalJsonArray.length(); j++) {
                        totalExternalAccounts++;
                    }
                }
            }

            if (PublicizeTable.onlyExternalConnections(serviceId)) {
                return totalExternalAccounts > 0;
            } else {
                return totalAccounts > 0 || totalExternalAccounts > 0;
            }
        } catch (JSONException e) {
            return false;
        }
    }

    /*
     * extract the keyring connection for the passed service from the response
     * to /me/keyring-connections
     */
    private static long parseServiceKeyringId(String serviceId, long currentUserId, JSONObject json) {
        JSONArray jsonConnectionList = json.optJSONArray("connections");
        if (jsonConnectionList == null) {
            return 0;
        }

        for (int i = 0; i < jsonConnectionList.length(); i++) {
            JSONObject jsonConnection = jsonConnectionList.optJSONObject(i);
            String service = JSONUtils.getString(jsonConnection, "service");
            if (serviceId.equals(service)) {
                // make sure userId matches the current user, or is zero (shared)
                long userId = jsonConnection.optLong("user_ID");
                if (userId == 0 || userId == currentUserId) {
                    return jsonConnection.optLong("ID");
                }
            }
        }

        return 0;
    }
}
