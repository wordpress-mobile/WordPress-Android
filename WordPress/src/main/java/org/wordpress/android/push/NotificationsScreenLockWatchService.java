package org.wordpress.android.push;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import org.wordpress.android.util.AppLog;

public class NotificationsScreenLockWatchService extends Service {

    BroadcastReceiver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(AppLog.T.NOTIFS, "notifications screen lock watch service > created");

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                GCMMessageService.rebuildAndUpdateNotifsOnSystemBarForRemainingNote(context);

                //restart the service
                Intent i = new Intent(context, NotificationsScreenLockWatchService.class);
                context.startService(i);
            }
        };
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        AppLog.i(AppLog.T.NOTIFS, "notifications screen lock watch service > destroyed");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //no op
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
