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
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginRestClient;
import org.wordpress.android.fluxc.network.wporg.plugin.PluginWPOrgClient;
import org.wordpress.android.fluxc.persistence.PluginSqlUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginStore extends Store {
    // Request payloads
    public static class ConfigureSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public ConfigureSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    public static class DeleteSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public DeleteSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchPluginDirectoryPayload extends Payload<BaseNetworkError> {
        public PluginDirectoryType type;
        public boolean loadMore;

        public FetchPluginDirectoryPayload(PluginDirectoryType type, boolean loadMore) {
            this.type = type;
            this.loadMore = loadMore;
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

    @SuppressWarnings("WeakerAccess")
    public static class SearchPluginDirectoryPayload extends Payload<BaseNetworkError> {
        public String searchTerm;
        public int offset;

        public SearchPluginDirectoryPayload(String searchTerm, int offset) {
            this.searchTerm = searchTerm;
            this.offset = offset;
        }
    }

    public static class UpdateSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public UpdateSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }
    }

    // Response payloads

    public static class ConfiguredSitePluginPayload extends Payload<ConfigureSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public ConfiguredSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public ConfiguredSitePluginPayload(SiteModel site, ConfigureSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class DeletedSitePluginPayload extends Payload<DeleteSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public DeletedSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public DeletedSitePluginPayload(SiteModel site, SitePluginModel plugin, DeleteSitePluginError error) {
            this.site = site;
            this.plugin = plugin;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchedPluginDirectoryPayload extends Payload<PluginDirectoryError> {
        public PluginDirectoryType type;
        public boolean loadMore;
        public boolean canLoadMore;
        public List<WPOrgPluginModel> plugins;

        public FetchedPluginDirectoryPayload(PluginDirectoryType type, boolean loadMore) {
            this.type = type;
            this.loadMore = loadMore;
        }
    }

    public static class FetchedSitePluginsPayload extends Payload<FetchSitePluginsError> {
        public SiteModel site;
        public List<SitePluginModel> plugins;

        public FetchedSitePluginsPayload(FetchSitePluginsError error) {
            this.error = error;
        }

        public FetchedSitePluginsPayload(@NonNull SiteModel site, @NonNull List<SitePluginModel> plugins) {
            this.site = site;
            this.plugins = plugins;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchedWPOrgPluginPayload extends Payload<FetchWPOrgPluginError> {
        public String pluginSlug;
        public WPOrgPluginModel wpOrgPlugin;

        public FetchedWPOrgPluginPayload(String pluginSlug, FetchWPOrgPluginError error) {
            this.pluginSlug = pluginSlug;
            this.error = error;
        }

        public FetchedWPOrgPluginPayload(String pluginSlug, WPOrgPluginModel plugin) {
            this.pluginSlug = pluginSlug;
            this.wpOrgPlugin = plugin;
        }
    }

    public static class InstalledSitePluginPayload extends Payload<InstallSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public InstalledSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public InstalledSitePluginPayload(SiteModel site, InstallSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SearchedPluginDirectoryPayload extends Payload<PluginDirectoryError> {
        public String searchTerm;
        public int offset;
        public boolean canLoadMore;
        public List<WPOrgPluginModel> plugins;

        public SearchedPluginDirectoryPayload(String searchTerm, int offset) {
            this.searchTerm = searchTerm;
            this.offset = offset;
        }
    }

    public static class UpdatedSitePluginPayload extends Payload<UpdateSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;

        public UpdatedSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
        }

        public UpdatedSitePluginPayload(SiteModel site, UpdateSitePluginError error) {
            this.site = site;
            this.error = error;
        }
    }

    // Errors

    public static class ConfigureSitePluginError implements OnChangedError {
        public ConfigureSitePluginErrorType type;
        @Nullable public String message;

        ConfigureSitePluginError(ConfigureSitePluginErrorType type) {
            this.type = type;
        }

        public ConfigureSitePluginError(String type, @Nullable String message) {
            this.type = ConfigureSitePluginErrorType.fromString(type);
            this.message = message;
        }
    }

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

    public static class PluginDirectoryError implements OnChangedError {
        public PluginDirectoryErrorType type;
        @Nullable public String message;

        public PluginDirectoryError(PluginDirectoryErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
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

    public static class FetchWPOrgPluginError implements OnChangedError {
        public FetchWPOrgPluginErrorType type;

        public FetchWPOrgPluginError(FetchWPOrgPluginErrorType type) {
            this.type = type;
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

    // Error types

    public enum ConfigureSitePluginErrorType {
        GENERIC_ERROR,
        ACTIVATION_ERROR,
        DEACTIVATION_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites
        UNAUTHORIZED,
        UNKNOWN_PLUGIN;

        public static ConfigureSitePluginErrorType fromString(String string) {
            if (string != null) {
                for (ConfigureSitePluginErrorType v : ConfigureSitePluginErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

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

    public enum PluginDirectoryErrorType {
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

    public enum FetchWPOrgPluginErrorType {
        EMPTY_RESPONSE,
        GENERIC_ERROR
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
        NOT_AVAILABLE, // Return for non-jetpack sites
        UPDATE_FAIL;

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

    // OnChanged Events

    @SuppressWarnings("WeakerAccess")
    public static class OnPluginDirectoryFetched extends OnChanged<PluginDirectoryError> {
        public PluginDirectoryType type;
        public boolean loadMore;
        public boolean canLoadMore;

        public OnPluginDirectoryFetched(PluginDirectoryType type, boolean loadMore) {
            this.type = type;
            this.loadMore = loadMore;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnPluginDirectorySearched extends OnChanged<PluginDirectoryError> {
        public String searchTerm;
        public int offset;
        public boolean canLoadMore;
        public List<WPOrgPluginModel> plugins;

        public OnPluginDirectorySearched(String searchTerm, int offset) {
            this.searchTerm = searchTerm;
            this.offset = offset;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginConfigured extends OnChanged<ConfigureSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;
        public OnSitePluginConfigured(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginDeleted extends OnChanged<DeleteSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;
        public OnSitePluginDeleted(SiteModel site) {
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
        public SitePluginModel plugin;
        public OnSitePluginInstalled(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginUpdated extends OnChanged<UpdateSitePluginError> {
        public SiteModel site;
        public SitePluginModel plugin;
        public OnSitePluginUpdated(SiteModel site) {
            this.site = site;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnWPOrgPluginFetched extends OnChanged<FetchWPOrgPluginError> {
        public String pluginSlug;

        public OnWPOrgPluginFetched(String pluginSlug) {
            this.pluginSlug = pluginSlug;
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
                configureSitePlugin((ConfigureSitePluginPayload) action.getPayload());
                break;
            case DELETE_SITE_PLUGIN:
                deleteSitePlugin((DeleteSitePluginPayload) action.getPayload());
                break;
            case FETCH_PLUGIN_DIRECTORY:
                fetchPluginDirectory((FetchPluginDirectoryPayload) action.getPayload());
                break;
            case FETCH_SITE_PLUGINS:
                fetchSitePlugins((SiteModel) action.getPayload());
                break;
            case FETCH_WPORG_PLUGIN:
                fetchWPOrgPlugin((String) action.getPayload());
                break;
            case INSTALL_SITE_PLUGIN:
                installSitePlugin((InstallSitePluginPayload) action.getPayload());
                break;
            case SEARCH_PLUGIN_DIRECTORY:
                searchPluginDirectory((SearchPluginDirectoryPayload) action.getPayload());
                break;
            case UPDATE_SITE_PLUGIN:
                updateSitePlugin((UpdateSitePluginPayload) action.getPayload());
                break;
            // Network callbacks
            case CONFIGURED_SITE_PLUGIN:
                configuredSitePlugin((ConfiguredSitePluginPayload) action.getPayload());
                break;
            case DELETED_SITE_PLUGIN:
                deletedSitePlugin((DeletedSitePluginPayload) action.getPayload());
                break;
            case FETCHED_PLUGIN_DIRECTORY:
                fetchedPluginDirectory((FetchedPluginDirectoryPayload) action.getPayload());
                break;
            case FETCHED_SITE_PLUGINS:
                fetchedSitePlugins((FetchedSitePluginsPayload) action.getPayload());
                break;
            case FETCHED_WPORG_PLUGIN:
                fetchedWPOrgPlugin((FetchedWPOrgPluginPayload) action.getPayload());
                break;
            case INSTALLED_SITE_PLUGIN:
                installedSitePlugin((InstalledSitePluginPayload) action.getPayload());
                break;
            case SEARCHED_PLUGIN_DIRECTORY:
                searchedPluginDirectory((SearchedPluginDirectoryPayload) action.getPayload());
                break;
            case UPDATED_SITE_PLUGIN:
                updatedSitePlugin((UpdatedSitePluginPayload) action.getPayload());
                break;
        }
    }

    public List<WPOrgPluginModel> getPluginDirectory(PluginDirectoryType type) {
        // TODO: query the plugin directory from PluginSqlUtils
        return new ArrayList<>();
    }

    public SitePluginModel getSitePluginByName(SiteModel site, String name) {
        return PluginSqlUtils.getSitePluginByName(site, name);
    }

    public List<SitePluginModel> getSitePlugins(SiteModel site) {
        return PluginSqlUtils.getSitePlugins(site);
    }

    public WPOrgPluginModel getWPOrgPluginBySlug(String slug) {
        return PluginSqlUtils.getWPOrgPluginBySlug(slug);
    }

    // Remote actions

    private void configureSitePlugin(ConfigureSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.configureSitePlugin(payload.site, payload.plugin);
        } else {
            ConfigureSitePluginError error = new ConfigureSitePluginError(ConfigureSitePluginErrorType.NOT_AVAILABLE);
            ConfiguredSitePluginPayload errorPayload = new ConfiguredSitePluginPayload(payload.site, error);
            configuredSitePlugin(errorPayload);
        }
    }

    private void deleteSitePlugin(DeleteSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.deleteSitePlugin(payload.site, payload.plugin);
        } else {
            DeleteSitePluginError error = new DeleteSitePluginError(DeleteSitePluginErrorType.NOT_AVAILABLE);
            DeletedSitePluginPayload errorPayload = new DeletedSitePluginPayload(payload.site, payload.plugin, error);
            deletedSitePlugin(errorPayload);
        }
    }

    private void fetchPluginDirectory(FetchPluginDirectoryPayload payload) {
        mPluginWPOrgClient.fetchPluginDirectory(payload.type);
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

    private void fetchWPOrgPlugin(String pluginSlug) {
        mPluginWPOrgClient.fetchWPOrgPlugin(pluginSlug);
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

    private void searchPluginDirectory(SearchPluginDirectoryPayload payload) {
        mPluginWPOrgClient.searchPluginDirectory(payload.searchTerm);
    }

    private void updateSitePlugin(UpdateSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updateSitePlugin(payload.site, payload.plugin);
        } else {
            UpdateSitePluginError error = new UpdateSitePluginError(
                    UpdateSitePluginErrorType.NOT_AVAILABLE);
            UpdatedSitePluginPayload errorPayload = new UpdatedSitePluginPayload(payload.site, error);
            updatedSitePlugin(errorPayload);
        }
    }

    // Network callbacks

    private void configuredSitePlugin(ConfiguredSitePluginPayload payload) {
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

    private void deletedSitePlugin(DeletedSitePluginPayload payload) {
        OnSitePluginDeleted event = new OnSitePluginDeleted(payload.site);
        // If the remote returns `UNKNOWN_PLUGIN` error, it means the plugin is not installed in remote anymore
        // most likely because the plugin is already removed on a different client and it was not synced yet.
        // Since we are trying to remove an already removed plugin, we should just remove it from DB and treat it as a
        // successful action.
        if (payload.isError() && payload.error.type != DeleteSitePluginErrorType.UNKNOWN_PLUGIN) {
            event.error = payload.error;
        } else {
            event.plugin = payload.plugin;
            PluginSqlUtils.deleteSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    private void fetchedPluginDirectory(FetchedPluginDirectoryPayload payload) {
        OnPluginDirectoryFetched event = new OnPluginDirectoryFetched(payload.type, payload.loadMore);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.canLoadMore = payload.canLoadMore;
            // TODO: Save the payload.plugins to DB
            if (!payload.loadMore) {
                // This is a fresh list, we need to remove the directory records for the fetched type
                PluginSqlUtils.deletePluginDirectoryForType(payload.type);
            }
            if (payload.plugins != null) {
                // For pagination to work correctly, we need to separate the actual plugin data from the list of plugins
                // for each directory type. This is important because the same data will be fetched from multiple
                // sources. We fetch different directory types (same plugin can be in both new and popular) as well as
                // do standalone fetches for plugins with `FETCH_WPORG_PLUGIN` action.
                PluginSqlUtils.insertOrUpdatePluginDirectoryList(pluginDirectoryListFromWPOrgPlugins(payload.plugins,
                        payload.type));
            }
        }
        emitChange(event);
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

    private void fetchedWPOrgPlugin(FetchedWPOrgPluginPayload payload) {
        OnWPOrgPluginFetched event = new OnWPOrgPluginFetched(payload.pluginSlug);
        if (payload.isError()) {
            event.error = payload.error;
        } else if (event.pluginSlug != null) {
            PluginSqlUtils.insertOrUpdateWPOrgPlugin(payload.wpOrgPlugin);
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

    private void searchedPluginDirectory(SearchedPluginDirectoryPayload payload) {
        OnPluginDirectorySearched event = new OnPluginDirectorySearched(payload.searchTerm, payload.offset);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.canLoadMore = payload.canLoadMore;
            event.plugins = payload.plugins;
        }
        emitChange(event);
    }

    private void updatedSitePlugin(UpdatedSitePluginPayload payload) {
        OnSitePluginUpdated event = new OnSitePluginUpdated(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.plugin = payload.plugin;
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.plugin);
        }
        emitChange(event);
    }

    // Helpers

    private List<PluginDirectoryModel> pluginDirectoryListFromWPOrgPlugins(@NonNull List<WPOrgPluginModel> wpOrgPlugins,
                                                                           PluginDirectoryType directoryType) {
        List<PluginDirectoryModel> directoryList = new ArrayList<>(wpOrgPlugins.size());
        for (WPOrgPluginModel wpOrgPluginModel : wpOrgPlugins) {
            PluginDirectoryModel pluginDirectoryModel = new PluginDirectoryModel();
            pluginDirectoryModel.setSlug(wpOrgPluginModel.getSlug());
            pluginDirectoryModel.setDirectoryType(directoryType.toString());
        }
        return directoryList;
    }
}
