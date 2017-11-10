package org.wordpress.android.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;

public class ServiceEventConnection {
    private final ServiceConnection mServiceConnection;

    public ServiceEventConnection(Context context, Class<? extends AutoForeground> clazz, Object client) {
        EventBus.getDefault().register(client);

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                // nothing here
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                // nothing here
            }
        };

        context.bindService(new Intent(context, clazz), mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect(Context context, Object client) {
        context.unbindService(mServiceConnection);
        EventBus.getDefault().unregister(client);
    }
}
