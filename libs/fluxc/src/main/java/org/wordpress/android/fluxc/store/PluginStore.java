package org.wordpress.android.fluxc.store;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

public class PluginStore extends Store {
    private final PluginRestClient mPluginRestClient;

    @Inject
    public PluginStore(Dispatcher dispatcher, PluginRestClient pluginRestClient) {
        super(dispatcher);
        mPluginRestClient = pluginRestClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PluginStore onRegister");
    }

    @Override
    public void onAction(Action action) {

    }
}
