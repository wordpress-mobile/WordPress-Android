package org.wordpress.android.util;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationManagerCompat;

import org.greenrobot.eventbus.EventBus;

public abstract class AutoForeground<EventClass> extends Service {

    public static final int NOTIFICATION_ID_PROGRESS = 1;
    public static final int NOTIFICATION_ID_SUCCESS = 2;
    public static final int NOTIFICATION_ID_FAILURE = 3;

    public static class ServiceEventConnection {
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

    private class LocalBinder extends Binder {}

    private final IBinder mBinder = new LocalBinder();

    private final Class<EventClass> mEventClass;

    protected abstract EventClass getCurrentStateEvent();
    protected abstract Notification getNotification();

    private boolean mIsForeground;

    protected abstract boolean isIdle();
    protected abstract boolean isInProgress();
    protected abstract boolean isError();

    protected AutoForeground(Class<EventClass> eventClass) {
        mEventClass = eventClass;
    }

    public boolean isForeground() {
        return mIsForeground;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notifyState();
    }

    @Nullable
    @CallSuper
    @Override
    public IBinder onBind(Intent intent) {
        clearAllNotifications();
        return mBinder;
    }

    @CallSuper
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);

        clearAllNotifications();
        background();
    }

    @CallSuper
    @Override
    public boolean onUnbind(Intent intent) {
        if (!hasConnectedClients()) {
            promoteForeground();
        }

        return true; // call onRebind() if new clients connect
    }

    protected void clearAllNotifications() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_PROGRESS);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_SUCCESS);
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_FAILURE);
    }

    private EventBus getEventBus() {
        return EventBus.getDefault();
    }

    private boolean hasConnectedClients() {
        return getEventBus().hasSubscriberForEvent(mEventClass);
    }

    private void promoteForeground() {
        if (isInProgress()) {
            startForeground(NOTIFICATION_ID_PROGRESS, getNotification());
            mIsForeground = true;
        }
    }

    private void background() {
        stopForeground(true);
        mIsForeground = false;
    }

    @CallSuper
    protected void notifyState() {
        // sticky emit the state. The stickiness serves as a state keeping mechanism for clients to re-read upon connect
        getEventBus().postSticky(getCurrentStateEvent());

        if (hasConnectedClients()) {
            // there are connected clients so, nothing more to do here
            return;
        }

        // ok, no connected clients so, update might need to be delivered to a notification as well

        if (isIdle()) {
            // no need to have a notification when idle
            return;
        }

        if (isInProgress()) {
            // operation still is progress so, update the notification
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_PROGRESS, getNotification());
            return;
        }

        // operation has ended so, demote the Service to a background one
        background();

        // dismiss the sticky notification
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_PROGRESS);

        // put out a simple success/failure notification
        NotificationManagerCompat.from(this).notify(isError() ? NOTIFICATION_ID_FAILURE : NOTIFICATION_ID_SUCCESS,
                getNotification());
    }
}
