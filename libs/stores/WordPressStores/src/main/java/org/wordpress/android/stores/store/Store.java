package org.wordpress.android.stores.store;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.Action;

public abstract class Store {
    protected final Dispatcher mDispatcher;

    Store(Dispatcher dispatcher) {
        mDispatcher = dispatcher;
    }
    public class OnChanged {}
    public abstract void onAction(Action action);
    public abstract void onRegister();

    protected void emitChange(OnChanged onChangedEvent) {
        mDispatcher.emitChange(onChangedEvent);
    }
}
