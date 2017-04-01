package org.wordpress.android.networking;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;

import de.greenrobot.event.EventBus;

/*
 * global network connection change receiver - declared in the manifest to monitor
 * android.net.conn.CONNECTIVITY_CHANGE
 */
public class ConnectionChangeReceiver extends BroadcastReceiver {
    private static boolean mFirstReceiveEh = true;
    private static boolean mConnectedEh = true;
    private static boolean mEnabledEh = false; // this value must be synchronized with the ConnectionChangeReceiver
                                               // state in our AndroidManifest

    public static class ConnectionChangeEvent {
        private final boolean mConnectedEh;
        public ConnectionChangeEvent(boolean connectedEh) {
            mConnectedEh = connectedEh;
        }
        public boolean connectedEh() {
            return mConnectedEh;
        }
    }

    /*
     * note that onReceive occurs when anything about the connection has changed, not just
     * when the connection has been lost or restated, so it can happen quite often when the
     * user is on the move. for this reason we only fire the event the first time onReceive
     * is called, and afterwards only when we know connection availability has changed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean connectedEh = NetworkUtils.networkAvailableEh(context);
        if (mFirstReceiveEh || connectedEh != mConnectedEh) {
            postConnectionChangeEvent(connectedEh);
        }
    }

    private static void postConnectionChangeEvent(boolean connectedEh) {
        AppLog.i(T.UTILS, "Connection status changed, connectedEh=" + connectedEh);
        mConnectedEh = connectedEh;
        mFirstReceiveEh = false;
        EventBus.getDefault().post(new ConnectionChangeEvent(connectedEh));
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (mEnabledEh == enabled) {
            return;
        }
        mEnabledEh = enabled;
        AppLog.i(T.UTILS, "ConnectionChangeReceiver.setEnabled " + enabled);
        int flag = (enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                              PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
        ComponentName component = new ComponentName(context, ConnectionChangeReceiver.class);
        context.getPackageManager().setComponentEnabledSetting(component, flag, PackageManager.DONT_KILL_APP);
        if (mEnabledEh) {
            postConnectionChangeEvent(NetworkUtils.networkAvailableEh(context));
        }
    }
}
