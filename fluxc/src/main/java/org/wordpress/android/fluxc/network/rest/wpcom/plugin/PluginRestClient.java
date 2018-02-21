package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginWPComRestResponse.FetchPluginsResponse;
import org.wordpress.android.fluxc.store.PluginStore.ConfiguredSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.DeletedSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginsError;
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.store.PluginStore.InstallSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.InstalledSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.ConfigureSitePluginError;
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
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginsAction(
                                new FetchedSitePluginsPayload(site, plugins)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        FetchSitePluginsError fetchPluginsError = new FetchSitePluginsError(((WPComGsonNetworkError)
                                networkError).apiError, networkError.message);
                        FetchedSitePluginsPayload payload = new FetchedSitePluginsPayload(fetchPluginsError);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void configureSitePlugin(@NonNull final SiteModel site, @NonNull final SitePluginModel plugin) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(getEncodedPluginName(plugin)).getUrlV1_2();
        Map<String, Object> params = paramsFromPluginModel(plugin);
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, params,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        pluginFromResponse.setId(plugin.getId());
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
                                new ConfiguredSitePluginPayload(site, configurePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newConfiguredSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void deleteSitePlugin(@NonNull final SiteModel site, @NonNull final SitePluginModel plugin) {
        String url = WPCOMREST.sites.site(site.getSiteId()).
                plugins.name(getEncodedPluginName(plugin)).delete.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        pluginFromResponse.setId(plugin.getId());
                        mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(
                                new DeletedSitePluginPayload(site, pluginFromResponse)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        DeleteSitePluginError deletePluginError = new DeleteSitePluginError(((WPComGsonNetworkError)
                                networkError).apiError, networkError.message);
                        DeletedSitePluginPayload payload =
                                new DeletedSitePluginPayload(site, plugin, deletePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void installSitePlugin(@NonNull final SiteModel site, String pluginName) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(pluginName).install.getUrlV1_2();
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
                        InstalledSitePluginPayload payload = new InstalledSitePluginPayload(site, installPluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newInstalledSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void updateSitePlugin(@NonNull final SiteModel site, @NonNull final SitePluginModel plugin) {
        String url = WPCOMREST.sites.site(site.getSiteId()).
                plugins.name(getEncodedPluginName(plugin)).update.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        SitePluginModel pluginFromResponse = pluginModelFromResponse(site, response);
                        pluginFromResponse.setId(plugin.getId());
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
                        UpdatedSitePluginPayload payload = new UpdatedSitePluginPayload(site,
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

    private Map<String, Object> paramsFromPluginModel(SitePluginModel sitePluginModel) {
        Map<String, Object> params = new HashMap<>();
        params.put("active", sitePluginModel.isActive());
        params.put("autoupdate", sitePluginModel.isAutoUpdateEnabled());
        return params;
    }

    private String getEncodedPluginName(SitePluginModel plugin) {
        try {
            // We need to encode plugin name otherwise names like "akismet/akismet" would fail
            return URLEncoder.encode(plugin.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return plugin.getName();
        }
    }
}
