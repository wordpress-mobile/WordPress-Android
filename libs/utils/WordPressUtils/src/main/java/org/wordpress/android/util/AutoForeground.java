package org.wordpress.android.util;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.CallSuper;
import android.support.v4.app.NotificationManagerCompat;

import java.util.ArrayList;

public class AutoForeground<State> implements WeakHandler.MessageListener {

    public static final int NOTIFICATION_ID_PROGRESS = 1;
    public static final int NOTIFICATION_ID_SUCCESS = 2;
    public static final int NOTIFICATION_ID_FAILURE = 3;

    public static final int MSG_REGISTER_CLIENT = 1;
    public static final int MSG_UNREGISTER_CLIENT = 2;

    public static final int MSG_CURRENT_STATE = 4;

    public static class ServiceClient {
        private ServiceConnection mServiceConnection;
        private final Messenger mClient;
        private Messenger mService;

        public ServiceClient(Context context, Class<? extends AutoForegroundListener<?>> clazz,
                WeakHandler.MessageListener clientListener) {
            mClient = new Messenger(new WeakHandler(clientListener));
            connect(context, clazz);
        }

        private void connect(Context context, Class<? extends AutoForegroundListener<?>> clazz) {
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    mService = new Messenger(iBinder);
                    registerClient();
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    // nothing here
                }
            };

            context.bindService(new Intent(context, clazz), mServiceConnection, Context.BIND_AUTO_CREATE);
        }

        public void disconnect(Context context) {
            unregisterClient();

            context.unbindService(mServiceConnection);
        }

        private void registerClient() {
            Message msg = Message.obtain(null, AutoForeground.MSG_REGISTER_CLIENT);
            msg.replyTo = mClient;
            try {
                mService.send(msg);
                requestCurrentState();
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything special here.
                e.printStackTrace();
            }
        }

        private void unregisterClient() {
            Message msg = Message.obtain(null, AutoForeground.MSG_UNREGISTER_CLIENT);
            msg.replyTo = mClient;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything special here.
                e.printStackTrace();
            }
        }

        public void requestCurrentState() {
            Message msg = Message.obtain(null, AutoForeground.MSG_CURRENT_STATE);
            msg.replyTo = mClient;
            try {
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public interface AutoForegroundListener<State> {
        State getCurrentState();
        Notification getNotification(State state);
        boolean isInProgress(State state);
        boolean isError(State state);
    }

    private final AutoForegroundListener<State> mAutoForegroundListener;
    private final Service mService;

    private ArrayList<Messenger> mConnectedClients = new ArrayList<>();

    private final Messenger mMessenger = new Messenger(new WeakHandler(this));

    public <T extends Service & AutoForegroundListener> AutoForeground(T service) {
        mAutoForegroundListener = service;
        mService = service;
    }

    @Override
    @CallSuper
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mConnectedClients.add(msg.replyTo);
                background();
                break;
            case MSG_UNREGISTER_CLIENT:
                mConnectedClients.remove(msg.replyTo);
                if (mConnectedClients.size() == 0) {
                    promoteForeground();
                }
                break;
            case MSG_CURRENT_STATE:
                notifyState(mAutoForegroundListener.getCurrentState());
                break;
            default:
                return false;
        }

        return true;
    }

    private boolean notifyConnectedClients(State state) {
        int validClients = 0;

        for (int i = mConnectedClients.size() - 1; i >= 0; i--) {
            try {
                mConnectedClients.get(i).send(Message.obtain(null, MSG_CURRENT_STATE, state));
                validClients++;
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mConnectedClients.remove(i);
            }
        }

        return validClients > 0;
    }

    public IBinder getBinder() {
        return mMessenger.getBinder();
    }

    private void promoteForeground() {
        State state = mAutoForegroundListener.getCurrentState();
        if (mAutoForegroundListener.isInProgress(state)) {
            mService.startForeground(NOTIFICATION_ID_PROGRESS, mAutoForegroundListener.getNotification(state));
        }
    }

    private void background() {
        mService.stopForeground(true);
    }

    @CallSuper
    public void notifyState(State state) {
        boolean hasValidClients = false;
        if (mConnectedClients.size() > 0) {
            hasValidClients = notifyConnectedClients(state);
        }

        if (!hasValidClients) {
            if (mAutoForegroundListener.isInProgress(state)) {
                NotificationManagerCompat.from(mService).notify(NOTIFICATION_ID_PROGRESS,
                        mAutoForegroundListener.getNotification(state));
            } else {
                background();
                NotificationManagerCompat.from(mService).cancel(NOTIFICATION_ID_PROGRESS);

                NotificationManagerCompat.from(mService).notify(
                        mAutoForegroundListener.isError(state) ? NOTIFICATION_ID_FAILURE : NOTIFICATION_ID_SUCCESS,
                        mAutoForegroundListener.getNotification(state));
            }
        }
    }
}
