package org.wordpress.android.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import de.greenrobot.event.EventBus;

public class ConnectionChangeReceiver extends BroadcastReceiver {

    public static class ConnectionChangeEvent {
        // EventBus event used to signify connection change
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        EventBus.getDefault().post(new ConnectionChangeEvent());
    }
}
