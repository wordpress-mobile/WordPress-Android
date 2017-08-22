package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;

public class PluginStore extends Store {
    // Payloads
    public static class UpdatePluginPayload extends Payload {
        public SiteModel site;
        public PluginModel plugin;

        public UpdatePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    public static class FetchedPluginsPayload extends Payload {
        public SiteModel site;
        public List<PluginModel> plugins;
        public FetchPluginsError error;

        public FetchedPluginsPayload(FetchPluginsError error) {
            this.error = error;
        }

        public FetchedPluginsPayload(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
            this.site = site;
            this.plugins = plugins;
        }
    }

    public static class FetchedPluginInfoPayload extends Payload {
        public PluginInfoModel pluginInfo;
        public FetchPluginInfoError error;

        public FetchedPluginInfoPayload(FetchPluginInfoError error) {
            this.error = error;
        }

        public FetchedPluginInfoPayload(PluginInfoModel pluginInfo) {
            this.pluginInfo = pluginInfo;
        }
    }

    public static class UpdatedPluginPayload extends Payload {
        public SiteModel site;
        public PluginModel plugin;
        public UpdatePluginError error;

        public UpdatedPluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedPluginPayload(SiteModel site, UpdatePluginError error) {
            this.error = error;
        }
    }

    public static class FetchPluginsError implements OnChangedError {
        public FetchPluginsErrorType type;
        public String message;
        public FetchPluginsError(FetchPluginsErrorType type) {
            this(type, "");
        }

        FetchPluginsError(FetchPluginsErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class FetchPluginInfoError implements OnChangedError {
        public FetchPluginInfoErrorType type;

        public FetchPluginInfoError(FetchPluginInfoErrorType type) {
            this.type = type;
        }
    }

    public static class UpdatePluginError implements OnChangedError {
        public UpdatePluginErrorType type;
        public String message;

        public UpdatePluginError(UpdatePluginErrorType type) {
            this.type = type;
        }
    }

    public enum FetchPluginsErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public enum FetchPluginInfoErrorType {
        GENERIC_ERROR
    }

    public enum UpdatePluginErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE // Return for non-jetpack sites
    }

    public static class OnPluginsChanged extends OnChanged<FetchPluginsError> {
        public SiteModel site;
        public OnPluginsChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnPluginInfoChanged extends OnChanged<FetchPluginInfoError> {
        public PluginInfoModel pluginInfo;
    }

    private final PluginRestClient mPluginRestClient;
    private final PluginWPOrgClient mPluginWPOrgClient;

    @Inject
    public PluginStore(Dispatcher dispatcher, PluginRestClient pluginRestClient, PluginWPOrgClient pluginWPOrgClient) {
        super(dispatcher);
        mPluginRestClient = pluginRestClient;
        mPluginWPOrgClient = pluginWPOrgClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(AppLog.T.API, "PluginStore onRegister");
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
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
            case FETCH_PLUGIN_INFO:
                fetchPluginInfo((String) action.getPayload());
                break;
            case UPDATE_PLUGIN:
                updatePlugin((UpdatePluginPayload) action.getPayload());
                break;
            case FETCHED_PLUGINS:
                fetchedPlugins((FetchedPluginsPayload) action.getPayload());
                break;
            case FETCHED_PLUGIN_INFO:
                fetchedPluginInfo((FetchedPluginInfoPayload) action.getPayload());
                break;
            case UPDATED_PLUGIN:
                updatedPlugin((UpdatedPluginPayload) action.getPayload());
                break;
        }
    }

    public List<PluginModel> getPlugins(SiteModel site) {
        return PluginSqlUtils.getPlugins(site);
    }

    public PluginInfoModel getPluginInfoBySlug(String slug) {
        return PluginSqlUtils.getPluginInfoBySlug(slug);
    }

    private void fetchPlugins(SiteModel site) {
        if (site.isUsingWpComRestApi() && site.isJetpackConnected()) {
            mPluginRestClient.fetchPlugins(site);
        } else {
            FetchPluginsError error = new FetchPluginsError(FetchPluginsErrorType.NOT_AVAILABLE);
            FetchedPluginsPayload payload = new FetchedPluginsPayload(error);
            fetchedPlugins(payload);
        }
    }

    private void fetchPluginInfo(String plugin) {
        mPluginWPOrgClient.fetchPluginInfo(plugin);
    }

    private void updatePlugin(UpdatePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updatePlugin(payload.site, payload.plugin);
        } else {
            UpdatePluginError error = new UpdatePluginError(UpdatePluginErrorType.NOT_AVAILABLE);
            UpdatedPluginPayload errorPayload = new UpdatedPluginPayload(payload.site, error);
            updatedPlugin(errorPayload);
        }
    }

    private void fetchedPlugins(FetchedPluginsPayload payload) {
        OnPluginsChanged event = new OnPluginsChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrReplacePlugins(payload.site, payload.plugins);
        }
        emitChange(event);
    }

    private void fetchedPluginInfo(FetchedPluginInfoPayload payload) {
        OnPluginInfoChanged event = new OnPluginInfoChanged();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.pluginInfo = payload.pluginInfo;
            PluginSqlUtils.insertOrUpdatePluginInfo(payload.pluginInfo);
        }
        emitChange(event);
    }

    private void updatedPlugin(UpdatedPluginPayload payload) {
    }
}
