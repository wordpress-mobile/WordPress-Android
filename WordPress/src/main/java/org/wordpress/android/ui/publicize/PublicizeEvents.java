package org.wordpress.android.ui.publicize;

import org.json.JSONObject;
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
        private long mSiteId;
        private int mKeychainId;

        public ActionAccountChosen(long siteId, int keychainId) {
            mSiteId = siteId;
            mKeychainId = keychainId;
        }

        public long getSiteId() {
            return mSiteId;
        }

        public int getKeychainId() {
            return mKeychainId;
        }
    }

    public static class ActionRequestChooseAccount {
        private long mSiteId;
        private String mServiceId;
        private JSONObject mJSONObject;

        public ActionRequestChooseAccount(long siteId, String serviceId, JSONObject jsonObject) {
            mSiteId = siteId;
            mServiceId = serviceId;
            mJSONObject = jsonObject;
        }

        public JSONObject getJSONObject() {
            return mJSONObject;
        }

        public long getSiteId() {
            return mSiteId;
        }

        public String getServiceId() {
            return mServiceId;
        }
    }
}
