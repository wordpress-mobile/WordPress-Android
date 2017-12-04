package org.wordpress.android.fluxc.store;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginStore extends Store {
    // Request payloads
    public static class DeleteSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public PluginModel plugin;

        public DeleteSitePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstallSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;

        public InstallSitePluginPayload(SiteModel site, String pluginName) {
            this.site = site;
            this.pluginName = pluginName;
        }
    }

    public static class UpdateSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public PluginModel plugin;

        public UpdateSitePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    public static class UpdateSitePluginVersionPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public PluginModel plugin;

        public UpdateSitePluginVersionPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    // Response payloads

    public static class DeletedSitePluginPayload extends Payload<DeleteSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;

        public DeletedSitePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public DeletedSitePluginPayload(SiteModel site, DeleteSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchedPluginInfoPayload extends Payload<FetchPluginInfoError> {
        public PluginInfoModel pluginInfo;

        public FetchedPluginInfoPayload(FetchPluginInfoError error) {
            this.error = error;
        }

        public FetchedPluginInfoPayload(PluginInfoModel pluginInfo) {
            this.pluginInfo = pluginInfo;
        }
    }

    public static class FetchedSitePluginsPayload extends Payload<FetchSitePluginsError> {
        public SiteModel site;
        public List<PluginModel> plugins;

        public FetchedSitePluginsPayload(FetchSitePluginsError error) {
            this.error = error;
        }

        public FetchedSitePluginsPayload(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
            this.site = site;
            this.plugins = plugins;
        }
    }

    public static class InstalledSitePluginPayload extends Payload<InstallSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;

        public InstalledSitePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public InstalledSitePluginPayload(SiteModel site, InstallSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class UpdatedSitePluginPayload extends Payload<UpdateSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;

        public UpdatedSitePluginPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedSitePluginPayload(SiteModel site, UpdateSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class UpdatedSitePluginVersionPayload extends Payload<UpdateSitePluginVersionError> {
        public SiteModel site;
        public PluginModel plugin;

        public UpdatedSitePluginVersionPayload(SiteModel site, PluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedSitePluginVersionPayload(SiteModel site, UpdateSitePluginVersionError error) {
            this.site = site;
            this.error = error;
        }
    }

    // Errors

    public static class DeleteSitePluginError implements OnChangedError {
        public DeleteSitePluginErrorType type;
        @Nullable public String message;

        DeleteSitePluginError(DeleteSitePluginErrorType type) {
            this.type = type;
        }

        public DeleteSitePluginError(String type, @Nullable String message) {
            this.type = DeleteSitePluginErrorType.fromString(type);
            this.message = message;
        }
    }

    public static class FetchPluginInfoError implements OnChangedError {
        public FetchPluginInfoErrorType type;

        public FetchPluginInfoError(FetchPluginInfoErrorType type) {
            this.type = type;
        }
    }

    public static class FetchSitePluginsError implements OnChangedError {
        public FetchSitePluginsErrorType type;
        @Nullable public String message;

        FetchSitePluginsError(FetchSitePluginsErrorType type) {
            this.type = type;
        }

        public FetchSitePluginsError(String type, @Nullable String message) {
            this.type = FetchSitePluginsErrorType.fromString(type);
            this.message = message;
        }
    }

    public static class InstallSitePluginError implements OnChangedError {
        public InstallSitePluginErrorType type;
        @Nullable public String message;

        InstallSitePluginError(InstallSitePluginErrorType type) {
            this.type = type;
        }

        public InstallSitePluginError(String type, @Nullable String message) {
            this.type = InstallSitePluginErrorType.fromString(type);
            this.message = message;
        }
    }

    public static class UpdateSitePluginError implements OnChangedError {
        public UpdateSitePluginErrorType type;
        @Nullable public String message;

        UpdateSitePluginError(UpdateSitePluginErrorType type) {
            this.type = type;
        }

        public UpdateSitePluginError(String type, @Nullable String message) {
            this.type = UpdateSitePluginErrorType.fromString(type);
            this.message = message;
        }
    }

    public static class UpdateSitePluginVersionError implements OnChangedError {
        public UpdateSitePluginVersionErrorType type;
        @Nullable public String message;

        UpdateSitePluginVersionError(UpdateSitePluginVersionErrorType type) {
            this.type = type;
        }

        public UpdateSitePluginVersionError(String type, @Nullable String message) {
            this.type = UpdateSitePluginVersionErrorType.fromString(type);
            this.message = message;
        }
    }

    // Error types

    public enum DeleteSitePluginErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        DELETE_PLUGIN_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites
        UNKNOWN_PLUGIN;

        public static DeleteSitePluginErrorType fromString(String string) {
            if (string != null) {
                for (DeleteSitePluginErrorType v : DeleteSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum FetchPluginInfoErrorType {
        EMPTY_RESPONSE,
        GENERIC_ERROR
    }

    public enum FetchSitePluginsErrorType {
        GENERIC_ERROR,
        UNAUTHORIZED,
        NOT_AVAILABLE; // Return for non-jetpack sites

        public static FetchSitePluginsErrorType fromString(String string) {
            if (string != null) {
                for (FetchSitePluginsErrorType v : FetchSitePluginsErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum InstallSitePluginErrorType {
        GENERIC_ERROR,
        INSTALL_FAILURE,
        LOCAL_FILE_DOES_NOT_EXIST,
        NO_PACKAGE,
        NO_PLUGIN_INSTALLED,
        NOT_AVAILABLE, // Return for non-jetpack sites
        PLUGIN_ALREADY_INSTALLED,
        UNAUTHORIZED;

        public static InstallSitePluginErrorType fromString(String string) {
            if (string != null) {
                if (string.equalsIgnoreCase("local-file-does-not-exist")) {
                    return LOCAL_FILE_DOES_NOT_EXIST;
                }
                for (InstallSitePluginErrorType v : InstallSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum UpdateSitePluginErrorType {
        GENERIC_ERROR,
        ACTIVATION_ERROR,
        DEACTIVATION_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites
        UNAUTHORIZED,
        UNKNOWN_PLUGIN;

        public static UpdateSitePluginErrorType fromString(String string) {
            if (string != null) {
                for (UpdateSitePluginErrorType v : UpdateSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum UpdateSitePluginVersionErrorType {
        GENERIC_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites
        UPDATE_FAIL;

        public static UpdateSitePluginVersionErrorType fromString(String string) {
            if (string != null) {
                for (UpdateSitePluginVersionErrorType v : UpdateSitePluginVersionErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    // OnChanged Events

    @SuppressWarnings("WeakerAccess")
    public static class OnPluginInfoChanged extends OnChanged<FetchPluginInfoError> {
        public PluginInfoModel pluginInfo;
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginDeleted extends OnChanged<DeleteSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnSitePluginDeleted(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginConfigured extends OnChanged<UpdateSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnSitePluginConfigured(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginVersionUpdated extends OnChanged<UpdateSitePluginVersionError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnSitePluginVersionUpdated(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginsFetched extends OnChanged<FetchSitePluginsError> {
        public SiteModel site;
        public OnSitePluginsFetched(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginInstalled extends OnChanged<InstallSitePluginError> {
        public SiteModel site;
        public PluginModel plugin;
        public OnSitePluginInstalled(SiteModel site) {
            this.site = site;
        }
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
            // Remote actions
            case CONFIGURE_SITE_PLUGIN:
                updateSitePlugin((UpdateSitePluginPayload) action.getPayload());
                break;
            case FETCH_SITE_PLUGINS:
                fetchSitePlugins((SiteModel) action.getPayload());
                break;
            case FETCH_PLUGIN_INFO:
                fetchPluginInfo((String) action.getPayload());
                break;
            case UPDATE_SITE_PLUGIN_VERSION:
                updateSitePluginVersion((UpdateSitePluginVersionPayload) action.getPayload());
                break;
            case DELETE_SITE_PLUGIN:
                deleteSitePlugin((DeleteSitePluginPayload) action.getPayload());
                break;
            case INSTALL_SITE_PLUGIN:
                installSitePlugin((InstallSitePluginPayload) action.getPayload());
                break;
            // Network callbacks
            case CONFIGURED_SITE_PLUGIN:
                updatedSitePlugin((UpdatedSitePluginPayload) action.getPayload());
                break;
            case FETCHED_SITE_PLUGINS:
                fetchedSitePlugins((FetchedSitePluginsPayload) action.getPayload());
                break;
            case FETCHED_PLUGIN_INFO:
                fetchedPluginInfo((FetchedPluginInfoPayload) action.getPayload());
                break;
            case UPDATED_SITE_PLUGIN_VERSION:
                updatedSitePluginVersion((UpdatedSitePluginVersionPayload) action.getPayload());
                break;
            case DELETED_SITE_PLUGIN:
                deletedSitePlugin((DeletedSitePluginPayload) action.getPayload());
                break;
            case INSTALLED_SITE_PLUGIN:
                installedSitePlugin((InstalledSitePluginPayload) action.getPayload());
                break;
        }
    }

    public List<PluginModel> getSitePlugins(SiteModel site) {
        return PluginSqlUtils.getSitePlugins(site);
    }

    public PluginModel getSitePluginByName(SiteModel site, String name) {
        return PluginSqlUtils.getSitePluginByName(site, name);
    }

    public PluginInfoModel getPluginInfoBySlug(String slug) {
        return PluginSqlUtils.getPluginInfoBySlug(slug);
    }

    private void fetchSitePlugins(SiteModel site) {
        if (site.isUsingWpComRestApi() && site.isJetpackConnected()) {
            mPluginRestClient.fetchSitePlugins(site);
        } else {
            FetchSitePluginsError error = new FetchSitePluginsError(FetchSitePluginsErrorType.NOT_AVAILABLE);
            FetchedSitePluginsPayload payload = new FetchedSitePluginsPayload(error);
            fetchedSitePlugins(payload);
        }
    }

    private void fetchPluginInfo(String plugin) {
        mPluginWPOrgClient.fetchPluginInfo(plugin);
    }

    private void updateSitePlugin(UpdateSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updateSitePlugin(payload.site, payload.plugin);
        } else {
            UpdateSitePluginError error = new UpdateSitePluginError(UpdateSitePluginErrorType.NOT_AVAILABLE);
            UpdatedSitePluginPayload errorPayload = new UpdatedSitePluginPayload(payload.site, error);
            updatedSitePlugin(errorPayload);
        }
    }

    private void updateSitePluginVersion(UpdateSitePluginVersionPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updateSitePluginVersion(payload.site, payload.plugin);
        } else {
            UpdateSitePluginVersionError error = new UpdateSitePluginVersionError(
                    UpdateSitePluginVersionErrorType.NOT_AVAILABLE);
            UpdatedSitePluginVersionPayload errorPayload = new UpdatedSitePluginVersionPayload(payload.site, error);
            updatedSitePluginVersion(errorPayload);
        }
    }

    private void deleteSitePlugin(DeleteSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.deleteSitePlugin(payload.site, payload.plugin);
        } else {
            DeleteSitePluginError error = new DeleteSitePluginError(DeleteSitePluginErrorType.NOT_AVAILABLE);
            DeletedSitePluginPayload errorPayload = new DeletedSitePluginPayload(payload.site, error);
            deletedSitePlugin(errorPayload);
        }
    }

    private void installSitePlugin(InstallSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.installSitePlugin(payload.site, payload.pluginName);
        } else {
            InstallSitePluginError error = new InstallSitePluginError(InstallSitePluginErrorType.NOT_AVAILABLE);
            InstalledSitePluginPayload errorPayload = new InstalledSitePluginPayload(payload.site, error);
            installedSitePlugin(errorPayload);
        }
    }

    private void fetchedSitePlugins(FetchedSitePluginsPayload payload) {
        OnSitePluginsFetched event = new OnSitePluginsFetched(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrReplaceSitePlugins(payload.site, payload.plugins);
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

    private void updatedSitePlugin(UpdatedSitePluginPayload payload) {
        OnSitePluginConfigured event = new OnSitePluginConfigured(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            payload.plugin.setLocalSiteId(payload.site.getId());
            event.plugin = payload.plugin;
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.plugin);
        }
        emitChange(event);
    }

    private void updatedSitePluginVersion(UpdatedSitePluginVersionPayload payload) {
        OnSitePluginVersionUpdated event = new OnSitePluginVersionUpdated(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.plugin = payload.plugin;
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.plugin);
        }
        emitChange(event);
    }

    private void deletedSitePlugin(DeletedSitePluginPayload payload) {
        OnSitePluginDeleted event = new OnSitePluginDeleted(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.plugin = payload.plugin;
            PluginSqlUtils.deleteSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    private void installedSitePlugin(InstalledSitePluginPayload payload) {
        OnSitePluginInstalled event = new OnSitePluginInstalled(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.plugin = payload.plugin;
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.plugin);
        }
        emitChange(event);
    }
}
