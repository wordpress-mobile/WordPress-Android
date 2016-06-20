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
        private final JSONObject mJsonObject;

        public ActionCompleted(boolean succeeded, ConnectAction action) {
            mSucceeded = succeeded;
            mAction = action;
            mJsonObject = new JSONObject();
        }

        public ActionCompleted(JSONObject jsonObject, ConnectAction action) {
            mSucceeded = false;
            mJsonObject = jsonObject;
            mAction = action;
        }

        public JSONObject getJsonObject() {
            return mJsonObject;
        }

        public ConnectAction getAction() {
            return mAction;
        }

        public boolean didSucceed() {
            return mSucceeded;
        }
    }
}
