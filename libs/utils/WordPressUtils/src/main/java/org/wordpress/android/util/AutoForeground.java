package org.wordpress.android.util;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
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

    private class LocalBinder extends Binder {}

    private final IBinder mBinder = new LocalBinder();

    private final Class<EventClass> mEventClass;

    protected abstract EventClass getCurrentStateEvent();
    protected abstract Notification getNotification();
    protected abstract boolean isInProgress();
    protected abstract boolean isError();

    protected AutoForeground(Class<EventClass> eventClass) {
        mEventClass = eventClass;
    }

    @Nullable
    @CallSuper
    @Override
    public IBinder onBind(Intent intent) {
        notifyState();

        return mBinder;
    }

    @CallSuper
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);

        background();
        notifyState();
    }

    @CallSuper
    @Override
    public boolean onUnbind(Intent intent) {
        if (!hasConnectedClients()) {
            promoteForeground();
        }

        return true; // call onRebind() if new clients connect
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
        }
    }

    private void background() {
        stopForeground(true);
    }

    @CallSuper
    protected void notifyState() {
        if (hasConnectedClients()) {
            // just send a message to the connected clients
            getEventBus().post(getCurrentStateEvent());
            return;
        }

        // ok, no connected clients so, update will be redirected to a notification

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
        NotificationManagerCompat.from(this).notify(
                isError() ? NOTIFICATION_ID_FAILURE : NOTIFICATION_ID_SUCCESS,
                getNotification());
    }
}
