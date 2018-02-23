package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginWPComRestResponse.FetchPluginsResponse;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.DeletedSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginDirectoryPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryError;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.UpdatedSitePluginPayload;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginRestClient extends BaseWPComRestClient {
    @Inject
    public PluginRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                            AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchSitePlugins(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.getUrlV1_2();
        final WPComGsonRequest<FetchPluginsResponse> request = WPComGsonRequest.buildGetRequest(url, null,
                FetchPluginsResponse.class,
                new Listener<FetchPluginsResponse>() {
                    @Override
                    public void onResponse(FetchPluginsResponse response) {
                        List<SitePluginModel> plugins = new ArrayList<>();
                        if (response.plugins != null) {
                            for (PluginWPComRestResponse pluginResponse : response.plugins) {
                                plugins.add(pluginModelFromResponse(site, pluginResponse));
                            }
                        }
                        FetchedPluginDirectoryPayload payload =
                                new FetchedPluginDirectoryPayload(site, plugins);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        PluginDirectoryError directoryError = new PluginDirectoryError(((WPComGsonNetworkError)
                                networkError).apiError, networkError.message);
                        FetchedPluginDirectoryPayload payload =
                                new FetchedPluginDirectoryPayload(PluginDirectoryType.SITE, false, directoryError);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                    }
                }
        );
        add(request);
    }

    public void configureSitePlugin(@NonNull final SiteModel site, @NonNull final String pluginName,
                                    @NonNull final String slug, boolean isActive, boolean isAutoUpdatesEnabled) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(getEncodedPluginName(pluginName)).getUrlV1_2();
        Map<String, Object> params = new HashMap<>();
        params.put("active", isActive);
        params.put("autoupdate", isAutoUpdatesEnabled);
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, params,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(
                                new ConfiguredSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        ConfigureSitePluginError configurePluginError = new ConfigureSitePluginError(((
                                WPComGsonNetworkError) networkError).apiError, networkError.message);
                        ConfiguredSitePluginPayload payload =
                                new ConfiguredSitePluginPayload(site, pluginName, slug, configurePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void deleteSitePlugin(@NonNull final SiteModel site, @NonNull final String pluginName,
                                 @NonNull final String slug) {
        String url = WPCOMREST.sites.site(site.getSiteId()).
                plugins.name(getEncodedPluginName(pluginName)).delete.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(
                                new DeletedSitePluginPayload(site, slug, pluginName)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        DeletedSitePluginPayload payload =
                                new DeletedSitePluginPayload(site, slug, pluginName);
                        payload.error = new DeleteSitePluginError(((WPComGsonNetworkError)
                                networkError).apiError, networkError.message);
                        mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void installSitePlugin(@NonNull final SiteModel site, final String pluginSlug) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.slug(pluginSlug).install.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(
                                new InstalledSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        InstallSitePluginError installPluginError = new InstallSitePluginError(((WPComGsonNetworkError)
                                networkError).apiError, networkError.message);
                        InstalledSitePluginPayload payload = new InstalledSitePluginPayload(site, pluginSlug,
                                installPluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void updateSitePlugin(@NonNull final SiteModel site, @NonNull final String pluginName,
                                 @NonNull final String slug) {
        String url = WPCOMREST.sites.site(site.getSiteId()).
                plugins.name(getEncodedPluginName(pluginName)).update.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(
                                new UpdatedSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        UpdateSitePluginError updatePluginError
                                = new UpdateSitePluginError(((WPComGsonNetworkError) networkError).apiError,
                                networkError.message);
                        UpdatedSitePluginPayload payload = new UpdatedSitePluginPayload(site, pluginName, slug,
                                updatePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    private SitePluginModel pluginModelFromResponse(SiteModel siteModel, PluginWPComRestResponse response) {
        SitePluginModel sitePluginModel = new SitePluginModel();
        sitePluginModel.setLocalSiteId(siteModel.getId());
        sitePluginModel.setName(response.name);
        sitePluginModel.setDisplayName(StringEscapeUtils.unescapeHtml4(response.display_name));
        sitePluginModel.setAuthorName(StringEscapeUtils.unescapeHtml4(response.author));
        sitePluginModel.setAuthorUrl(response.author_url);
        sitePluginModel.setDescription(StringEscapeUtils.unescapeHtml4(response.description));
        sitePluginModel.setIsActive(response.active);
        sitePluginModel.setIsAutoUpdateEnabled(response.autoupdate);
        sitePluginModel.setPluginUrl(response.plugin_url);
        sitePluginModel.setSlug(response.slug);
        sitePluginModel.setVersion(response.version);
        if (response.action_links != null) {
            sitePluginModel.setSettingsUrl(response.action_links.settings);
        }
        return sitePluginModel;
    }

    private String getEncodedPluginName(String pluginName) {
        try {
            // We need to encode plugin name otherwise names like "akismet/akismet" would fail
            return URLEncoder.encode(pluginName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return pluginName;
        }
    }
}
