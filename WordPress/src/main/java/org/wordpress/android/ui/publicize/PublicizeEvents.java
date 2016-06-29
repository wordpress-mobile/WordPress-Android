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

    public static class ActionRequestChooseAccount {
        private JSONObject mJSONObject;

        public ActionRequestChooseAccount(JSONObject jsonObject) {
            mJSONObject = jsonObject;
        }

        public JSONObject getJSONObject() {
            return mJSONObject;
        }
    }
}
