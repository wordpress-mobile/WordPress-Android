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

import org.wordpress.android.util.AutoForeground.ServiceState;

import java.util.HashMap;
import java.util.Map;

public abstract class AutoForeground<StateClass extends ServiceState>
        extends Service {

    public static final int NOTIFICATION_ID_PROGRESS = 1;
    public static final int NOTIFICATION_ID_SUCCESS = 2;
    public static final int NOTIFICATION_ID_FAILURE = 3;

    public interface ServiceState {
        boolean isIdle();
        boolean isInProgress();
        boolean isError();
        boolean isTerminal();
        String getStepName();
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

    private final Class<StateClass> mStateClass;

    private boolean mIsForeground;

    protected abstract void onProgressStart();
    protected abstract void onProgressEnd();

    protected abstract Notification getNotification(StateClass state);
    protected abstract void trackStateUpdate(Map<String, ?> props);

    @SuppressWarnings("unchecked")
    protected AutoForeground(StateClass initialState) {
        mStateClass = (Class<StateClass>) initialState.getClass();

        // initialize the sticky phase if it hasn't already
        if (EventBus.getDefault().getStickyEvent(mStateClass) == null) {
            notifyState(initialState);
        }
    }

    public boolean isForeground() {
        return mIsForeground;
    }

    @Nullable
    private StateClass getState() {
        return getState(mStateClass);
    }

    @Nullable
    protected static <StateClass> StateClass getState(Class<StateClass> stateClass) {
        return EventBus.getDefault().getStickyEvent(stateClass);
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
        return getEventBus().hasSubscriberForEvent(mStateClass);
    }

    private void promoteForeground() {
        final StateClass state = getState();
        if (state.isInProgress()) {
            startForeground(NOTIFICATION_ID_PROGRESS, getNotification(state));
            mIsForeground = true;
        }
    }

    private void background() {
        stopForeground(true);
        mIsForeground = false;
    }

    @CallSuper
    protected void setState(StateClass newState) {
        StateClass currentState = getState();
        if ((currentState == null || !currentState.isInProgress()) && newState.isInProgress()) {
            onProgressStart();
        }

        track(newState);
        notifyState(newState);

        if (newState.isTerminal()) {
            onProgressEnd();
            stopSelf();
        }
    }

    private void track(ServiceState state) {
        Map<String, Object> props = new HashMap<>();
        props.put("login_phase", state == null ? "null" : state.getStepName());
        props.put("login_service_is_foreground", isForeground());
        trackStateUpdate(props);
    }

    protected static <T> void clearServiceState(Class<T> klass) {
        EventBus.getDefault().removeStickyEvent(klass);
    }

    @CallSuper
    protected void notifyState(StateClass state) {
        // sticky emit the state. The stickiness serves as a state keeping mechanism for clients to re-read upon connect
        getEventBus().postSticky(state);

        if (hasConnectedClients()) {
            // there are connected clients so, nothing more to do here
            return;
        }

        // ok, no connected clients so, update might need to be delivered to a notification as well

        if (state.isIdle()) {
            // no need to have a notification when idle
            return;
        }

        if (state.isInProgress()) {
            // operation still is progress so, update the notification
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID_PROGRESS, getNotification(state));
            return;
        }

        // operation has ended so, demote the Service to a background one
        background();

        // dismiss the sticky notification
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID_PROGRESS);

        // put out a simple success/failure notification
        NotificationManagerCompat.from(this).notify(
                state.isError() ? NOTIFICATION_ID_FAILURE : NOTIFICATION_ID_SUCCESS,
                getNotification(state));
    }
}
