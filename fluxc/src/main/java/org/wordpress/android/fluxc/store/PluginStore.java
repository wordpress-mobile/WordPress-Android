package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class PluginStore extends Store {
    // Payloads
    public static class FetchedPluginsPayload extends Payload {
        public SiteModel site;
        public List<PluginModel> plugins;
        public FetchedPluginsPayload(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
            this.site = site;
            this.plugins = plugins;
        }
    }

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
        IAction actionType = action.getType();
        if (!(actionType instanceof PluginAction)) {
            return;
        }
        switch ((PluginAction) actionType) {
            case FETCH_PLUGINS:
                fetchPlugins((SiteModel) action.getPayload());
                break;
            case FETCHED_PLUGINS:
                fetchedPlugins((FetchedPluginsPayload) action.getPayload());
                break;
        }
    }

    private void fetchPlugins(SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mPluginRestClient.fetchPlugins(site);
        }
    }

    private void fetchedPlugins(FetchedPluginsPayload payload) {
        // TODO
    }
}
