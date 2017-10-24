package org.wordpress.android.util;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Class to wrap a Handler with a WeakReference so we don't leak a context
 */
public class WeakHandler extends Handler {
    public interface MessageListener {
        boolean handleMessage(Message msg);
    }

    private final WeakReference<MessageListener> mWeakMessageListener;

    public WeakHandler(MessageListener messageListener) {
        mWeakMessageListener = new WeakReference<>(messageListener);
    }

    @Override
    public void handleMessage(Message msg) {
        final MessageListener messageListener = mWeakMessageListener.get();
        if (messageListener == null) {
            return;
        }

        if (!messageListener.handleMessage(msg)) {
            super.handleMessage(msg);
        }
    }
}
