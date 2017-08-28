package org.wordpress.android.fluxc.store;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;


public class ThemeStore extends Store {
    @Inject
    public ThemeStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "ThemeStore onRegister");
    }
}
