package org.wordpress.android.ui.publicize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.models.PublicizeConnection;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

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

    public static class ActionAccountChosen {
        private int mSiteId;
        private int mKeychainId;

        public ActionAccountChosen(int siteId, int keychainId) {
            mSiteId = siteId;
            mKeychainId = keychainId;
        }

        public int getSiteId() {
            return mSiteId;
        }

        public int getKeychainId() {
            return mKeychainId;
        }
    }

    public static class ConnectionChooserRequired {
        private JSONObject mJsonObject;
        private PublicizeConnection[] mPublicizeConnections;

        public ConnectionChooserRequired(JSONObject jsonObject) {
            mJsonObject = jsonObject;
            try {
                mPublicizeConnections = convertJsonToConnections();
            } catch (JSONException e) {
                mPublicizeConnections = new PublicizeConnection[0];
                e.printStackTrace();
            }
        }

        public PublicizeConnection[] getConnections() {
            return mPublicizeConnections;
        }

        private PublicizeConnection[] convertJsonToConnections() throws JSONException {
            JSONArray jsonArray = mJsonObject.getJSONArray("connections");
            PublicizeConnection[] publicizeConnectionArray = new PublicizeConnection[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                publicizeConnectionArray[i] = PublicizeConnection.fromJson(jsonArray.getJSONObject(i));
            }

            return publicizeConnectionArray;
        }

        private int[] getSitesArrayFromJson(JSONArray jsonArray) throws JSONException {
            int[] sitesArray = new int[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                sitesArray[i] = jsonArray.getInt(i);
            }

            return sitesArray;
        }
    }
}
