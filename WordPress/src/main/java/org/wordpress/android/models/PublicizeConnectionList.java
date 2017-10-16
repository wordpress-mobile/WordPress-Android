package org.wordpress.android.models;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class PublicizeConnectionList extends ArrayList<PublicizeConnection> {

    private int indexOfConnection(PublicizeConnection connection) {
        if (connection == null) return -1;

        for (int i = 0; i < this.size(); i++) {
            if (connection.connectionId == this.get(i).connectionId) {
                return i;
            }
        }

        return -1;
    }

    public boolean isSameAs(PublicizeConnectionList otherList) {
        if (otherList == null || otherList.size() != this.size()) {
            return false;
        }

        for (PublicizeConnection otherConnection: otherList) {
            int i = this.indexOfConnection(otherConnection);
            if (i == -1) {
                return false;
            } else if (!otherConnection.isSameAs(this.get(i))) {
                return false;
            }
        }

        return true;
    }

    public PublicizeConnectionList getServiceConnectionsForUser(long userId, String serviceId) {
        PublicizeConnectionList connections = new PublicizeConnectionList();
        if (TextUtils.isEmpty(serviceId)) return connections;

        for (PublicizeConnection connection: this) {
            if (connection.getService().equalsIgnoreCase(serviceId)) {
                // shared connections are available to all users, otherwise the service userId
                // must match the current userId to be considered connected
                if (connection.isShared || connection.userId == userId) {
                    connections.add(connection);
                }
            }
        }
        return connections;
    }

    public boolean isServiceConnectedForUser(long userId, PublicizeService service) {
        if (service == null) return false;

        for (PublicizeConnection connection: this) {
            if (connection.getService().equalsIgnoreCase(service.getId())) {
                if (connection.isShared || connection.userId == userId) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * returns comma-separated string of user display names
     */
    public String getUserDisplayNames() {
        StringBuilder users = new StringBuilder();
        for (PublicizeConnection connection: this) {
            if (users.length() > 0) {
                users.append(", ");
            }
            users.append(connection.getExternalDisplayName());
        }
        return users.toString();
    }

    /*
     * passed JSON is the response from sites/%d/publicize-connections
     *   {"connections":[
     *      {"ID":12783250,
     *       "site_ID":52451176,
     *       "user_ID":5399133,
     *       ...
     */
    public static PublicizeConnectionList fromJson(JSONObject json) {
        PublicizeConnectionList connectionList = new PublicizeConnectionList();
        if (json == null) return connectionList;

        JSONArray jsonConnectionList = json.optJSONArray("connections");
        if (jsonConnectionList == null) return connectionList;

        for (int i = 0; i < jsonConnectionList.length(); i++) {
            PublicizeConnection connection = PublicizeConnection.fromJson(jsonConnectionList.optJSONObject(i));
            connectionList.add(connection);
        }

        return connectionList;
    }
}
