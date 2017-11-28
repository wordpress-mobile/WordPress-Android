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

import org.wordpress.android.util.AutoForeground.ServiceEvent;
import org.wordpress.android.util.AutoForeground.ServicePhase;

import java.util.HashMap;
import java.util.Map;

public abstract class AutoForeground<PhaseClass extends ServicePhase, EventClass extends ServiceEvent<PhaseClass>>
        extends Service {

    public static final int NOTIFICATION_ID_PROGRESS = 1;
    public static final int NOTIFICATION_ID_SUCCESS = 2;
    public static final int NOTIFICATION_ID_FAILURE = 3;

    public interface ServicePhase {
        boolean isIdle();
        boolean isInProgress();
        boolean isError();
        boolean isTerminal();
        String name();
    }

    public interface ServiceEvent<T> {
        T getState();
    }

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
    private final PhaseClass mInitialPhase;

    private boolean mIsForeground;

    protected abstract void registerDispatcher();
    protected abstract void unregisterDispatcher();

    protected abstract EventClass getStateEvent(PhaseClass phase);
    protected abstract Notification getNotification(PhaseClass phase);
    protected abstract void trackPhaseUpdate(Map<String, ?> props);

    protected AutoForeground(PhaseClass initialPhase, Class<EventClass> eventClass) {
        mEventClass = eventClass;
        mInitialPhase = initialPhase;
    }

    public boolean isForeground() {
        return mIsForeground;
    }

    protected PhaseClass getPhase() {
        return EventBus.getDefault().getStickyEvent(mEventClass).getState();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        notifyState(mInitialPhase);
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
        final PhaseClass phase = getPhase();
        if (phase.isInProgress()) {
            startForeground(NOTIFICATION_ID_PROGRESS, getNotification(phase));
            mIsForeground = true;
        }
    }

    private void background() {
        stopForeground(true);
        mIsForeground = false;
    }

    @CallSuper
    protected void setState(PhaseClass newPhase) {
        if (!getPhase().isInProgress() && newPhase.isInProgress()) {
            registerDispatcher();
        }

        track(newPhase);
        notifyState(newPhase);

        if (newPhase.isTerminal()) {
            unregisterDispatcher();
            stopSelf();
        }
    }

    private void track(ServicePhase phase) {
        Map<String, Object> props = new HashMap<>();
        props.put("login_phase", phase == null ? "null" : phase.name());
        props.put("login_service_is_foreground", isForeground());
        trackPhaseUpdate(props);
    }

    protected static <T> void clearServiceState(T klass) {
        EventBus.getDefault().removeStickyEvent(klass);
    }

    @CallSuper
    protected void notifyState(PhaseClass phase) {
        // sticky emit the state. The stickiness serves as a state keeping mechanism for clients to re-read upon connect
        getEventBus().postSticky(getStateEvent(phase));

        if (hasConnectedClients()) {
            // there are connected clients so, nothing more to do here
            return;
        }

        // ok, no connected clients so, update might need to be delivered to a notification as well

        if (phase.isIdle()) {
            // no need to have a notification when idle
            return;
        }

        if (phase.isInProgress()) {
            // operation still is progress so, update the notification
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_PROGRESS, getNotification(phase));
            return;
        }

        // operation has ended so, demote the Service to a background one
        background();

        // dismiss the sticky notification
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_PROGRESS);

        // put out a simple success/failure notification
        NotificationManagerCompat.from(this).notify(phase.isError() ? NOTIFICATION_ID_FAILURE : NOTIFICATION_ID_SUCCESS,
                getNotification(phase));
    }
}
