package org.wordpress.android.ui.publicize;

import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

/**
 * Publicize-related EventBus event classes
 */
public class PublicizeEvents {
    private PublicizeEvents() {
        throw new AssertionError();
    }

    public static class ConnectionsChanged {
    }

    public static class ActionCompleted {
        private final boolean mSucceeded;
        private final ConnectAction mAction;
        /**
         * The reason for why {@link #mSucceeded} is false.
         */
        @Nullable private final Integer mReasonResId;
        private String mService;

        public ActionCompleted(boolean succeeded, ConnectAction action, String service) {
            this(succeeded, action, service, null);
        }

        ActionCompleted(boolean succeeded, ConnectAction action, String service, @Nullable Integer reasonResId) {
            mSucceeded = succeeded;
            mAction = action;
            mService = service;
            mReasonResId = reasonResId;
        }

        public ConnectAction getAction() {
            return mAction;
        }

        public boolean didSucceed() {
            return mSucceeded;
        }

        public String getService() {
            return mService;
        }

        @Nullable public Integer getReasonResId() {
            return mReasonResId;
        }
    }

    public static class ActionAccountChosen {
        private long mSiteId;
        private int mKeychainId;
        private String mService;
        private String mExternalUserId;

        public ActionAccountChosen(long siteId, int keychainId, String service, String externalUserId) {
            mSiteId = siteId;
            mKeychainId = keychainId;
            mService = service;
            mExternalUserId = externalUserId;
        }

        public long getSiteId() {
            return mSiteId;
        }

        public int getKeychainId() {
            return mKeychainId;
        }

        public String getService() {
            return mService;
        }

        public String getExternalUserId() {
            return mExternalUserId;
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
