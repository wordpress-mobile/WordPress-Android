package org.wordpress.android.fluxc.annotations.action;

public class Action<T> {
    private final IAction mActionType;
    private final T mPayload;

    public Action(IAction actionType, T payload) {
        mActionType = actionType;
        mPayload = payload;
    }

    public IAction getType() {
        return mActionType;
    }

    public T getPayload() {
        return mPayload;
    }
}
