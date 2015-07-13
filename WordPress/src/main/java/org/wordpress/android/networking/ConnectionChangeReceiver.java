package org.wordpress.android.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.wordpress.android.util.NetworkUtils;

import de.greenrobot.event.EventBus;

/*
 * global network connection change receiver - declared in the manifest to monitor
 * android.net.conn.CONNECTIVITY_CHANGE
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {

    private static boolean mWasConnected = true;

    public static class ConnectionChangeEvent {
        private final boolean mIsConnected;
        public ConnectionChangeEvent(boolean isConnected) {
            mIsConnected = isConnected;
        }
        public boolean isConnected() {
            return mIsConnected;
        }
    }

    /*
     * note that onReceive() may occur quite often if the user is on the move, but we only
     * fire the event when the connection availability changes
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isConnected = NetworkUtils.isNetworkAvailable(context);
        if (isConnected != mWasConnected) {
            mWasConnected = isConnected;
            EventBus.getDefault().post(new ConnectionChangeEvent(isConnected));
        }
    }
}
