package org.wordpress.android.fluxc.store;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.PluginAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.ImmutablePluginModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginJetpackTunnelRestClient;
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
    @SuppressWarnings("WeakerAccess")
    public static class ConfigureSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;
        public String slug;
        public boolean isActive;
        public boolean isAutoUpdateEnabled;

        public ConfigureSitePluginPayload(SiteModel site, String pluginName, String slug, boolean isActive,
                                          boolean isAutoUpdateEnabled) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
            this.isActive = isActive;
            this.isAutoUpdateEnabled = isAutoUpdateEnabled;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class DeleteSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String slug;
        public String pluginName;

        public DeleteSitePluginPayload(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchPluginDirectoryPayload extends Payload<BaseNetworkError> {
        public PluginDirectoryType type;
        public @Nullable SiteModel site;
        public boolean loadMore;

        public FetchPluginDirectoryPayload(PluginDirectoryType type, @Nullable SiteModel site, boolean loadMore) {
            this.type = type;
            this.site = site;
            this.loadMore = loadMore;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;

        public FetchSitePluginPayload(SiteModel site, String pluginName) {
            this.site = site;
            this.pluginName = pluginName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstallSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String slug;

        public InstallSitePluginPayload(SiteModel site, String slug) {
            this.site = site;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SearchPluginDirectoryPayload extends Payload<BaseNetworkError> {
        public SiteModel site; // required to add the SitePluginModels to the OnPluginDirectorySearched
        public String searchTerm;
        public int page;

        public SearchPluginDirectoryPayload(@Nullable SiteModel site, String searchTerm, int page) {
            this.site = site;
            this.searchTerm = searchTerm;
            this.page = page;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class UpdateSitePluginPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginName;
        public String slug;

        public UpdateSitePluginPayload(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    // Response payloads

    @SuppressWarnings("WeakerAccess")
    public static class ConfiguredSitePluginPayload extends Payload<ConfigureSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;
        public SitePluginModel plugin;

        public ConfiguredSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
            this.pluginName = this.plugin.getName();
            this.slug = this.plugin.getSlug();
        }

        public ConfiguredSitePluginPayload(SiteModel site, String pluginName, ConfigureSitePluginError error) {
            this.site = site;
            this.pluginName = pluginName;
            this.error = error;
        }

        public ConfiguredSitePluginPayload(SiteModel site, String pluginName, String slug,
                                           ConfigureSitePluginError error) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class DeletedSitePluginPayload extends Payload<DeleteSitePluginError> {
        public SiteModel site;
        public String slug;
        public String pluginName;

        public DeletedSitePluginPayload(SiteModel site, String slug, String pluginName) {
            this.site = site;
            this.slug = slug;
            this.pluginName = pluginName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class FetchedPluginDirectoryPayload extends Payload<PluginDirectoryError> {
        public PluginDirectoryType type;
        public boolean loadMore = false;
        public boolean canLoadMore = false;

        // Used for PluginDirectoryType.NEW & PluginDirectoryType.Popular
        public int page;
        public List<WPOrgPluginModel> wpOrgPlugins;

        // Used for PluginDirectoryType.SITE
        public SiteModel site;
        public List<SitePluginModel> sitePlugins;

        public FetchedPluginDirectoryPayload(PluginDirectoryType type, List<WPOrgPluginModel> wpOrgPlugins,
                                             boolean loadMore, boolean canLoadMore, int page) {
            this.type = type;
            this.wpOrgPlugins = wpOrgPlugins;
            this.loadMore = loadMore;
            this.canLoadMore = canLoadMore;
            this.page = page;
        }

        public FetchedPluginDirectoryPayload(SiteModel site, List<SitePluginModel> sitePlugins) {
            this.type = PluginDirectoryType.SITE;
            this.site = site;
            this.sitePlugins = sitePlugins;
        }

        public FetchedPluginDirectoryPayload(PluginDirectoryType type, boolean loadMore, PluginDirectoryError error) {
            this.type = type;
            this.loadMore = loadMore;
            this.error = error;
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

    @SuppressWarnings("WeakerAccess")
    public static class FetchedSitePluginPayload extends Payload<FetchSitePluginError> {
        public SitePluginModel plugin;
        public String pluginName;

        public FetchedSitePluginPayload(SitePluginModel plugin) {
            this.plugin = plugin;
        }

        public FetchedSitePluginPayload(String pluginName, FetchSitePluginError error) {
            this.pluginName = pluginName;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class InstalledSitePluginPayload extends Payload<InstallSitePluginError> {
        public SiteModel site;
        public String slug;
        public SitePluginModel plugin;

        public InstalledSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
            this.slug = this.plugin.getSlug();
        }

        public InstalledSitePluginPayload(SiteModel site, String slug, InstallSitePluginError error) {
            this.site = site;
            this.slug = slug;
            this.error = error;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SearchedPluginDirectoryPayload extends Payload<PluginDirectoryError> {
        public SiteModel site;
        public String searchTerm;
        public int page;
        public boolean canLoadMore;
        public List<WPOrgPluginModel> plugins;

        public SearchedPluginDirectoryPayload(@Nullable SiteModel site, String searchTerm, int page) {
            this.site = site;
            this.searchTerm = searchTerm;
            this.page = page;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class UpdatedSitePluginPayload extends Payload<UpdateSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;
        public SitePluginModel plugin;

        public UpdatedSitePluginPayload(SiteModel site, SitePluginModel plugin) {
            this.site = site;
            this.plugin = plugin;
            this.pluginName = this.plugin.getName();
            this.slug = this.plugin.getSlug();
        }

        public UpdatedSitePluginPayload(SiteModel site, String pluginName, String slug, UpdateSitePluginError error) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
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

        public ConfigureSitePluginError(GenericErrorType type, @Nullable String message, boolean isActivating) {
            this.type = ConfigureSitePluginErrorType.fromGenericErrorType(type, isActivating);
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

        public DeleteSitePluginError(GenericErrorType type, @Nullable String message) {
            this.type = DeleteSitePluginErrorType.fromGenericErrorType(type);
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

        public PluginDirectoryError(String type, @Nullable String message) {
            this.type = PluginDirectoryErrorType.fromString(type);
            this.message = message;
        }

        public PluginDirectoryError(GenericErrorType type, @Nullable String message) {
            this.type = PluginDirectoryErrorType.fromGenericErrorType(type);
            this.message = message;
        }
    }

    public static class FetchWPOrgPluginError implements OnChangedError {
        public FetchWPOrgPluginErrorType type;

        public FetchWPOrgPluginError(FetchWPOrgPluginErrorType type) {
            this.type = type;
        }
    }

    public static class FetchSitePluginError implements OnChangedError {
        public FetchSitePluginErrorType type;
        @Nullable public String message;

        public FetchSitePluginError(FetchSitePluginErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public FetchSitePluginError(GenericErrorType type, @Nullable String message) {
            this.type = FetchSitePluginErrorType.fromGenericErrorType(type);
            this.message = message;
        }
    }

    public static class InstallSitePluginError implements OnChangedError {
        public InstallSitePluginErrorType type;
        @Nullable  public Integer errorCode;
        @Nullable public String message;

        InstallSitePluginError(InstallSitePluginErrorType type) {
            this(type, null);
        }

        InstallSitePluginError(InstallSitePluginErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public InstallSitePluginError(String type, @Nullable String message) {
            this.type = InstallSitePluginErrorType.fromString(type);
            this.message = message;
        }

        public InstallSitePluginError(BaseNetworkError error) {
            this.type = InstallSitePluginErrorType.fromNetworkError(error);
            this.message = error.message;
            if (error.hasVolleyError()) {
                this.errorCode = error.volleyError.networkResponse.statusCode;
            }
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

    static class RemoveSitePluginsError implements OnChangedError {
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

        public static ConfigureSitePluginErrorType fromGenericErrorType(GenericErrorType genericErrorType,
                                                                        boolean isActivating) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case TIMEOUT:
                    case NO_CONNECTION:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                        if (isActivating) {
                            return ACTIVATION_ERROR;
                        } else {
                            return DEACTIVATION_ERROR;
                        }
                    case NOT_FOUND:
                    case CENSORED:
                        return UNKNOWN_PLUGIN;
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
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

        public static DeleteSitePluginErrorType fromGenericErrorType(GenericErrorType genericErrorType) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case TIMEOUT:
                    case NO_CONNECTION:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                        return DELETE_PLUGIN_ERROR;
                    case NOT_FOUND:
                    case CENSORED:
                        return UNKNOWN_PLUGIN;
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return DeleteSitePluginErrorType.UNAUTHORIZED;
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum PluginDirectoryErrorType {
        EMPTY_RESPONSE, // Should be used for NEW & POPULAR plugin directory
        GENERIC_ERROR,
        NOT_AVAILABLE, // Return for non-jetpack sites for SITE plugin directory
        UNAUTHORIZED; // Should only be used for SITE plugin directory

        public static PluginDirectoryErrorType fromString(String string) {
            if (string != null) {
                for (PluginDirectoryErrorType v : PluginDirectoryErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }

        public static PluginDirectoryErrorType fromGenericErrorType(GenericErrorType genericErrorType) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case NO_CONNECTION:
                    case TIMEOUT:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                    case NOT_FOUND:
                    case CENSORED:
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum FetchWPOrgPluginErrorType {
        EMPTY_RESPONSE,
        GENERIC_ERROR,
        PLUGIN_DOES_NOT_EXIST
    }

    public enum FetchSitePluginErrorType {
        UNAUTHORIZED,
        NOT_AVAILABLE,
        EMPTY_RESPONSE,
        GENERIC_ERROR,
        PLUGIN_DOES_NOT_EXIST;

        public static FetchSitePluginErrorType fromGenericErrorType(GenericErrorType genericErrorType) {
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case NOT_FOUND:
                        return PLUGIN_DOES_NOT_EXIST;
                    case NO_CONNECTION:
                    case TIMEOUT:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                    case CENSORED:
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
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

        private static final String  PLUGIN_ALREADY_EXISTS = "Destination folder already exists.";

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

        public static InstallSitePluginErrorType fromNetworkError(BaseNetworkError error) {
            if (PLUGIN_ALREADY_EXISTS.equalsIgnoreCase(error.message)) {
                return PLUGIN_ALREADY_INSTALLED;
            }
            GenericErrorType genericErrorType = error.type;
            if (genericErrorType != null) {
                switch (genericErrorType) {
                    case TIMEOUT:
                    case NO_CONNECTION:
                    case NETWORK_ERROR:
                    case SERVER_ERROR:
                        return INSTALL_FAILURE;
                    case NOT_FOUND:
                    case CENSORED:
                        return NO_PLUGIN_INSTALLED;
                    case INVALID_SSL_CERTIFICATE:
                    case HTTP_AUTH_ERROR:
                    case AUTHORIZATION_REQUIRED:
                    case NOT_AUTHENTICATED:
                        return UNAUTHORIZED;
                    case INVALID_RESPONSE:
                    case PARSE_ERROR:
                    case UNKNOWN:
                        return GENERIC_ERROR;
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
        public @Nullable SiteModel site;
        public String searchTerm;
        public int page;
        public boolean canLoadMore;
        public List<ImmutablePluginModel> plugins;

        public OnPluginDirectorySearched(@Nullable SiteModel site, String searchTerm, int page) {
            this.site = site;
            this.searchTerm = searchTerm;
            this.page = page;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginConfigured extends OnChanged<ConfigureSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;

        public OnSitePluginConfigured(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginDeleted extends OnChanged<DeleteSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;

        public OnSitePluginDeleted(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginInstalled extends OnChanged<InstallSitePluginError> {
        public SiteModel site;
        public String slug;

        public OnSitePluginInstalled(SiteModel site, String slug) {
            this.site = site;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginUpdated extends OnChanged<UpdateSitePluginError> {
        public SiteModel site;
        public String pluginName;
        public String slug;

        public OnSitePluginUpdated(SiteModel site, String pluginName, String slug) {
            this.site = site;
            this.pluginName = pluginName;
            this.slug = slug;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnWPOrgPluginFetched extends OnChanged<FetchWPOrgPluginError> {
        public String pluginSlug;

        public OnWPOrgPluginFetched(String pluginSlug) {
            this.pluginSlug = pluginSlug;
        }
    }

    public static class OnSitePluginFetched extends OnChanged<FetchSitePluginError> {
        public SitePluginModel plugin;
        public String pluginName;

        public OnSitePluginFetched(FetchedSitePluginPayload payload) {
            this.plugin = payload.plugin;
            this.pluginName = payload.pluginName;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class OnSitePluginsRemoved extends OnChanged<RemoveSitePluginsError> {
        public SiteModel site;
        public int rowsAffected;

        public OnSitePluginsRemoved(SiteModel site, int rowsAffected) {
            this.site = site;
            this.rowsAffected = rowsAffected;
        }
    }

    private final PluginRestClient mPluginRestClient;
    private final PluginWPOrgClient mPluginWPOrgClient;
    private final PluginCoroutineStore mPluginCoroutineStore;
    private final PluginJetpackTunnelRestClient mPluginJetpackTunnelRestClient;

    @Inject public PluginStore(Dispatcher dispatcher,
                               PluginRestClient pluginRestClient,
                               PluginWPOrgClient pluginWPOrgClient,
                               PluginCoroutineStore pluginCoroutineStore,
                               PluginJetpackTunnelRestClient pluginJetpackTunnelRestClient) {
        super(dispatcher);
        mPluginRestClient = pluginRestClient;
        mPluginWPOrgClient = pluginWPOrgClient;
        mPluginCoroutineStore = pluginCoroutineStore;
        mPluginJetpackTunnelRestClient = pluginJetpackTunnelRestClient;
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
            case FETCH_WPORG_PLUGIN:
                fetchWPOrgPlugin((String) action.getPayload());
                break;
            case FETCH_SITE_PLUGIN:
                fetchSitePlugin((FetchSitePluginPayload) action.getPayload());
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
            // Local actions
            case REMOVE_SITE_PLUGINS:
                removeSitePlugins((SiteModel) action.getPayload());
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
            case FETCHED_WPORG_PLUGIN:
                fetchedWPOrgPlugin((FetchedWPOrgPluginPayload) action.getPayload());
                break;
            case FETCHED_SITE_PLUGIN:
                fetchedSitePlugin((FetchedSitePluginPayload) action.getPayload());
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

    public @NonNull List<ImmutablePluginModel> getPluginDirectory(@NonNull SiteModel site, PluginDirectoryType type) {
        // Site plugins are handled differently
        if (type == PluginDirectoryType.SITE) {
            return getSitePlugins(site);
        }
        List<ImmutablePluginModel> immutablePlugins = new ArrayList<>();
        List<WPOrgPluginModel> wpOrgPlugins = PluginSqlUtils.getWPOrgPluginsForDirectory(type);
        for (WPOrgPluginModel wpOrgPlugin : wpOrgPlugins) {
            String slug = wpOrgPlugin.getSlug();
            SitePluginModel sitePlugin = PluginSqlUtils.getSitePluginBySlug(site, slug);
            immutablePlugins.add(ImmutablePluginModel.newInstance(sitePlugin, wpOrgPlugin));
        }
        return immutablePlugins;
    }

    public @Nullable ImmutablePluginModel getImmutablePluginBySlug(@NonNull SiteModel site, String slug) {
        SitePluginModel sitePlugin = PluginSqlUtils.getSitePluginBySlug(site, slug);
        WPOrgPluginModel wpOrgPlugin = PluginSqlUtils.getWPOrgPluginBySlug(slug);
        return ImmutablePluginModel.newInstance(sitePlugin, wpOrgPlugin);
    }

    private @NonNull List<ImmutablePluginModel> getSitePlugins(@NonNull SiteModel site) {
        List<ImmutablePluginModel> immutablePlugins = new ArrayList<>();
        List<SitePluginModel> sitePlugins = PluginSqlUtils.getSitePlugins(site);
        for (SitePluginModel sitePluginModel : sitePlugins) {
            String slug = sitePluginModel.getSlug();
            WPOrgPluginModel wpOrgPluginModel = PluginSqlUtils.getWPOrgPluginBySlug(slug);
            if (wpOrgPluginModel == null) {
                mDispatcher.dispatch(PluginActionBuilder.newFetchWporgPluginAction(slug));
            }
            immutablePlugins.add(ImmutablePluginModel.newInstance(sitePluginModel, wpOrgPluginModel));
        }
        return immutablePlugins;
    }

    // Remote actions

    private void configureSitePlugin(ConfigureSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.configureSitePlugin(payload.site, payload.pluginName, payload.slug, payload.isActive,
                    payload.isAutoUpdateEnabled);
        } else if (payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.configurePlugin(payload.site, payload.pluginName, payload.isActive);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.configureSitePlugin(payload.site, payload.pluginName, payload.slug, payload.isActive);
        } else {
            ConfigureSitePluginError error = new ConfigureSitePluginError(ConfigureSitePluginErrorType.NOT_AVAILABLE);
            ConfiguredSitePluginPayload errorPayload = new ConfiguredSitePluginPayload(payload.site, payload.slug,
                    payload.pluginName, error);
            mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(errorPayload));
        }
    }

    private void deleteSitePlugin(DeleteSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.deleteSitePlugin(payload.site, payload.pluginName, payload.slug);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.deleteSitePlugin(payload.site, payload.pluginName, payload.slug);
        } else {
            DeleteSitePluginError error = new DeleteSitePluginError(DeleteSitePluginErrorType.NOT_AVAILABLE);
            DeletedSitePluginPayload errorPayload = new DeletedSitePluginPayload(payload.site, payload.slug,
                    payload.pluginName);
            errorPayload.error = error;
            mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(errorPayload));
        }
    }

    private void fetchPluginDirectory(FetchPluginDirectoryPayload payload) {
        if (payload.type == PluginDirectoryType.SITE) {
            fetchSitePlugins(payload.site);
        } else if (payload.type == PluginDirectoryType.FEATURED) {
            mPluginWPOrgClient.fetchFeaturedPlugins();
        } else {
            int page = 1;
            if (payload.loadMore) {
                page = PluginSqlUtils.getLastRequestedPageForDirectoryType(payload.type) + 1;
            }
            mPluginWPOrgClient.fetchPluginDirectory(payload.type, page);
        }
    }

    private void fetchSitePlugins(@Nullable SiteModel site) {
        if (site != null && site.isUsingWpComRestApi() && site.isJetpackConnected()) {
            mPluginRestClient.fetchSitePlugins(site);
        } else if (site != null && !site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.fetchWPApiPlugins(site);
        } else {
            PluginDirectoryError error = new PluginDirectoryError(PluginDirectoryErrorType.NOT_AVAILABLE, null);
            FetchedPluginDirectoryPayload errorPayload = new FetchedPluginDirectoryPayload(PluginDirectoryType.SITE,
                    false, error);
            mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(errorPayload));
        }
    }

    private void fetchWPOrgPlugin(String pluginSlug) {
        mPluginWPOrgClient.fetchWPOrgPlugin(pluginSlug);
    }

    /* Fetch a single plugin from a site, to get its information and whether it exists or not.
       Currently this is only supported on sites connected using Jetpack plugin or Jetpack Connection Package.
     */
    private void fetchSitePlugin(FetchSitePluginPayload payload) {
        if (payload.site.isJetpackConnected() || payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.fetchPlugin(payload.site, payload.pluginName);
        } else if (!payload.site.isUsingWpComRestApi()){
            mPluginCoroutineStore.fetchWPApiPlugin(payload.site, payload.pluginName);
        } else {
            FetchSitePluginError error = new FetchSitePluginError(FetchSitePluginErrorType.NOT_AVAILABLE, null);
            FetchedSitePluginPayload errorPayload =
                    new FetchedSitePluginPayload(payload.pluginName, error);
            mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginAction(errorPayload));
        }
    }

    private void installSitePlugin(InstallSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.installSitePlugin(payload.site, payload.slug);
        } else if (payload.site.isJetpackCPConnected()) {
            mPluginJetpackTunnelRestClient.installPlugin(payload.site, payload.slug);
        } else if (!payload.site.isUsingWpComRestApi()) {
            mPluginCoroutineStore.installSitePlugin(payload.site, payload.slug);
        } else {
            InstallSitePluginError error = new InstallSitePluginError(InstallSitePluginErrorType.NOT_AVAILABLE);
            InstalledSitePluginPayload errorPayload = new InstalledSitePluginPayload(payload.site,
                    payload.slug, error);
            mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(errorPayload));
        }
    }

    private void searchPluginDirectory(SearchPluginDirectoryPayload payload) {
        mPluginWPOrgClient.searchPluginDirectory(payload.site, payload.searchTerm, payload.page);
    }

    private void updateSitePlugin(UpdateSitePluginPayload payload) {
        if (payload.site.isUsingWpComRestApi() && payload.site.isJetpackConnected()) {
            mPluginRestClient.updateSitePlugin(payload.site, payload.pluginName, payload.slug);
        } else {
            UpdateSitePluginError error = new UpdateSitePluginError(
                    UpdateSitePluginErrorType.NOT_AVAILABLE);
            UpdatedSitePluginPayload errorPayload = new UpdatedSitePluginPayload(payload.site,
                    payload.pluginName, payload.slug, error);
            mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(errorPayload));
        }
    }

    // Local actions
    private void removeSitePlugins(SiteModel site) {
        if (site == null) {
            return;
        }
        int rowsAffected = PluginSqlUtils.deleteSitePlugins(site);
        emitChange(new OnSitePluginsRemoved(site, rowsAffected));
    }

    // Network callbacks

    private void configuredSitePlugin(ConfiguredSitePluginPayload payload) {
        OnSitePluginConfigured event = new OnSitePluginConfigured(payload.site, payload.pluginName, payload.slug);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    private void deletedSitePlugin(DeletedSitePluginPayload payload) {
        OnSitePluginDeleted event = new OnSitePluginDeleted(payload.site, payload.pluginName, payload.slug);
        // If the remote returns `UNKNOWN_PLUGIN` error, it means the plugin is not installed in remote anymore
        // most likely because the plugin is already removed on a different client and it was not synced yet.
        // Since we are trying to remove an already removed plugin, we should just remove it from DB and treat it as a
        // successful action.
        if (payload.isError() && payload.error.type != DeleteSitePluginErrorType.UNKNOWN_PLUGIN) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.deleteSitePlugin(payload.site, payload.slug);
        }
        emitChange(event);
    }

    private void fetchedPluginDirectory(FetchedPluginDirectoryPayload payload) {
        OnPluginDirectoryFetched event = new OnPluginDirectoryFetched(payload.type, payload.loadMore);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.canLoadMore = payload.canLoadMore;
            if (event.type == PluginDirectoryType.SITE) {
                PluginSqlUtils.insertOrReplaceSitePlugins(payload.site, payload.sitePlugins);
            } else {
                if (!payload.loadMore) {
                    // This is a fresh list, we need to remove the directory records for the fetched type
                    PluginSqlUtils.deletePluginDirectoryForType(payload.type);
                }
                if (payload.wpOrgPlugins != null) {
                    // For pagination to work correctly, we need to separate the actual plugin data from the list of
                    // plugins for each directory type. This is important because the same data will be fetched from
                    // multiple sources. We fetch different directory types (same plugin can be in both new and popular)
                    // as well as do standalone fetches for plugins with `FETCH_WPORG_PLUGIN` action. We also need to
                    // keep track of the page the plugin belongs to, because the `per_page` parameter is unreliable.
                    PluginSqlUtils.insertPluginDirectoryList(
                            pluginDirectoryListFromWPOrgPlugins(payload.wpOrgPlugins, payload.type, payload.page));
                    PluginSqlUtils.insertOrUpdateWPOrgPluginList(payload.wpOrgPlugins);
                }
            }
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

    private void fetchedSitePlugin(FetchedSitePluginPayload payload) {
        OnSitePluginFetched event = new OnSitePluginFetched(payload);
        if (payload.isError()) {
            event.error = payload.error;
        }
        emitChange(event);
    }

    private void installedSitePlugin(InstalledSitePluginPayload payload) {
        OnSitePluginInstalled event = new OnSitePluginInstalled(payload.site, payload.slug);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);

        // Once the plugin is installed activate it and enable auto-updates
        if (!payload.isError() && payload.plugin != null) {
            try {
                // Give a second to the server as otherwise the following configure call may fail
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // https://www.javaspecialists.eu/archive/Issue056-Shutting-down-Threads-Cleanly.html
                Thread.currentThread().interrupt();
            }

            ConfigureSitePluginPayload configurePayload = new ConfigureSitePluginPayload(payload.site,
                    payload.plugin.getName(), payload.plugin.getSlug(), true, true);
            mDispatcher.dispatch(PluginActionBuilder.newConfigureSitePluginAction(configurePayload));
        }
    }

    private void searchedPluginDirectory(SearchedPluginDirectoryPayload payload) {
        OnPluginDirectorySearched event = new OnPluginDirectorySearched(payload.site, payload.searchTerm, payload.page);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            event.canLoadMore = payload.canLoadMore;
            PluginSqlUtils.insertOrUpdateWPOrgPluginList(payload.plugins);
            List<ImmutablePluginModel> immutablePluginList = new ArrayList<>();
            for (WPOrgPluginModel wpOrgPlugin : payload.plugins) {
                SitePluginModel sitePlugin = null;
                if (payload.site != null) {
                    sitePlugin = PluginSqlUtils.getSitePluginBySlug(payload.site, wpOrgPlugin.getSlug());
                }
                immutablePluginList.add(ImmutablePluginModel.newInstance(sitePlugin, wpOrgPlugin));
            }
            event.plugins = immutablePluginList;
        }
        emitChange(event);
    }

    private void updatedSitePlugin(UpdatedSitePluginPayload payload) {
        OnSitePluginUpdated event = new OnSitePluginUpdated(payload.site, payload.pluginName, payload.slug);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            PluginSqlUtils.insertOrUpdateSitePlugin(payload.site, payload.plugin);
        }
        emitChange(event);
    }

    // Helpers

    private List<PluginDirectoryModel> pluginDirectoryListFromWPOrgPlugins(@NonNull List<WPOrgPluginModel> wpOrgPlugins,
                                                                           PluginDirectoryType directoryType,
                                                                           int page) {
        List<PluginDirectoryModel> directoryList = new ArrayList<>(wpOrgPlugins.size());
        for (WPOrgPluginModel wpOrgPluginModel : wpOrgPlugins) {
            PluginDirectoryModel pluginDirectoryModel = new PluginDirectoryModel();
            pluginDirectoryModel.setSlug(wpOrgPluginModel.getSlug());
            pluginDirectoryModel.setDirectoryType(directoryType.toString());
            pluginDirectoryModel.setPage(page);
            directoryList.add(pluginDirectoryModel);
        }
        return directoryList;
    }
}
