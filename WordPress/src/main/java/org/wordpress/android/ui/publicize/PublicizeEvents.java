package org.wordpress.android.ui.publicize;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;
import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

import java.net.URL;

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
        @Nullable private final Reason mReason;
        private String mService;

        public ActionCompleted(boolean succeeded, ConnectAction action, String service) {
            this(succeeded, action, service, null);
        }

        ActionCompleted(boolean succeeded, ConnectAction action, String service, @Nullable Reason reason) {
            mSucceeded = succeeded;
            mAction = action;
            mService = service;
            mReason = reason;
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

        @Nullable public Reason getReason() {
            return mReason;
        }

        public static class Reason {
            private final int mMessageResId;
            /**
             * A resource Id containing a URL which points to a page, explaining more about {@link #mMessageResId}.
             */
            private final int mExplanationURLResId;

            Reason(int messageResId, int explanationURLResId) {
                mMessageResId = messageResId;
                mExplanationURLResId = explanationURLResId;
            }

            public int getMessageResId() {
                return mMessageResId;
            }

            public int getExplanationURLResId() {
                return mExplanationURLResId;
            }
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
