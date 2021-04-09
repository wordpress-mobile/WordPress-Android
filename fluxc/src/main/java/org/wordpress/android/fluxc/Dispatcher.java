package org.wordpress.android.fluxc;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.store.Store;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class Dispatcher {
    private final EventBus mBus;

    @Inject public Dispatcher() {
        mBus = EventBus.builder()
                .logNoSubscriberMessages(true)
                .sendNoSubscriberEvent(true)
                .throwSubscriberException(true)
                .build();
    }

    public void register(final Object object) {
        mBus.register(object);
        if (object instanceof Store) {
            ((Store) object).onRegister();
        }
    }

    public void unregister(final Object object) {
        mBus.unregister(object);
    }

    public void dispatch(Action action) {
        AppLog.d(T.API, "Dispatching action: " + action.getType().getClass().getSimpleName()
                + "-" + action.getType().toString());
        post(action);
    }

    public void emitChange(final Object changeEvent) {
        mBus.post(changeEvent);
    }

    private void post(final Object event) {
        mBus.post(event);
    }
}
