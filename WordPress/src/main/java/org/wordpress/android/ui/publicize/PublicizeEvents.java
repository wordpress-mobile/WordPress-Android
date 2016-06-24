package org.wordpress.android.ui.publicize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

import java.net.URI;

/**
 * Publicize-related EventBus event classes
 */
public class PublicizeEvents {

    private PublicizeEvents() {
        throw new AssertionError();
    }

    public static class ConnectionsChanged {}

    public static class ActionCompleted {
        private final boolean mSucceeded;
        private final ConnectAction mAction;

        public ActionCompleted(boolean succeeded, ConnectAction action) {
            mSucceeded = succeeded;
            mAction = action;
        }

        public ConnectAction getAction() {
            return mAction;
        }

        public boolean didSucceed() {
            return mSucceeded;
        }
    }

    public static class ConnectionChooserRequired {
        private JSONObject mJsonObject;
        private Connection[] mConnections;

        public ConnectionChooserRequired(JSONObject jsonObject) {
            mJsonObject = jsonObject;
            try {
                mConnections = convertJsonToConnections();
            } catch (JSONException e) {
                mConnections = new Connection[0];
                e.printStackTrace();
            }
        }

        public Connection[] getConnections() {
            return mConnections;
        }

        private Connection[] convertJsonToConnections() throws JSONException {
            JSONArray jsonArray = mJsonObject.getJSONArray("connections");
            Connection[] connectionArray = new Connection[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject currentJsonConnection = jsonArray.getJSONObject(i);
                URI profilePictureUrl = URI.create(currentJsonConnection.getString("external_profile_picture"));
                String displayName = currentJsonConnection.getString("external_display");
                int keychainId = currentJsonConnection.getInt("ID");
                String connectionName = currentJsonConnection.getString("label");
                JSONArray jsonSitesArray = currentJsonConnection.getJSONArray("sites");
                int[] sitesArray = getSitesArrayFromJson(jsonSitesArray);

                Connection connection = new Connection(profilePictureUrl, displayName, keychainId, connectionName, sitesArray);
                connectionArray[i] = connection;
            }

            return connectionArray;
        }

        private int[] getSitesArrayFromJson(JSONArray jsonArray) throws JSONException {
            int[] sitesArray = new int[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                sitesArray[i] = jsonArray.getInt(i);
            }

            return sitesArray;
        }
    }

    public static class Connection {
        private URI profilePictureUrl;
        private String displayName;
        private int keychainId;
        private String serviceName;
        private int[] sites;

        public Connection(URI profilePictureUrl, String displayName, int keychainId, String serviceName, int[] sites) {
            this.profilePictureUrl = profilePictureUrl;
            this.displayName = displayName;
            this.keychainId = keychainId;
            this.serviceName = serviceName;
            this.sites = sites;
        }

        public URI getProfilePictureUrl() {
            return profilePictureUrl;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getKeychainId() {
            return keychainId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public int[] getSites() {
            return sites;
        }
    }
}
