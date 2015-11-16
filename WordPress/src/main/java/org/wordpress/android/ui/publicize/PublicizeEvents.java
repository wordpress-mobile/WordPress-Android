package org.wordpress.android.ui.publicize;

import org.wordpress.android.ui.publicize.PublicizeConstants.ConnectAction;

/**
 * Publicize-related EventBus event classes
 */
public class PublicizeEvents {

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
}
