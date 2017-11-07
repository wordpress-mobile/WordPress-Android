package org.wordpress.android.fluxc.network.rest.wpcom.plugin;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.plugin.PluginWPComRestResponse.FetchPluginsResponse;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.DeleteSitePluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.DeletedSitePluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginsError;
import org.wordpress.android.fluxc.store.PluginStore.FetchSitePluginsErrorType;
import org.wordpress.android.fluxc.store.PluginStore.FetchedSitePluginsPayload;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginError;
import org.wordpress.android.fluxc.store.PluginStore.UpdateSitePluginErrorType;
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
                        List<PluginModel> plugins = new ArrayList<>();
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
                        FetchSitePluginsError fetchPluginsError
                                = new FetchSitePluginsError(FetchSitePluginsErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    fetchPluginsError.type = FetchSitePluginsErrorType.UNAUTHORIZED;
                                    break;
                            }
                        }
                        fetchPluginsError.message = networkError.message;
                        FetchedSitePluginsPayload payload = new FetchedSitePluginsPayload(fetchPluginsError);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedSitePluginsAction(payload));
                    }
                }
        );
        add(request);
    }

    public void updateSitePlugin(@NonNull final SiteModel site, @NonNull final PluginModel plugin) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.name(getEncodedPluginName(plugin)).getUrlV1_2();
        Map<String, Object> params = paramsFromPluginModel(plugin);
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, params,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                        PluginModel pluginModel = pluginModelFromResponse(site, response);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(
                                new UpdatedSitePluginPayload(site, pluginModel)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        UpdateSitePluginError updatePluginError
                                = new UpdateSitePluginError(UpdateSitePluginErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    updatePluginError.type = UpdateSitePluginErrorType.UNAUTHORIZED;
                                    break;
                                case "activation_error":
                                    updatePluginError.type = UpdateSitePluginErrorType.ACTIVATION_ERROR;
                                    break;
                                case "deactivation_error":
                                    updatePluginError.type = UpdateSitePluginErrorType.DEACTIVATION_ERROR;
                                    break;
                            }
                        }
                        updatePluginError.message = networkError.message;
                        UpdatedSitePluginPayload payload = new UpdatedSitePluginPayload(site, updatePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newUpdatedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }

    public void deleteSitePlugin(@NonNull final SiteModel site, @NonNull final PluginModel plugin) {
        String url = WPCOMREST.sites.site(site.getSiteId()).
                plugins.name(getEncodedPluginName(plugin)).delete.getUrlV1_2();
        final WPComGsonRequest<PluginWPComRestResponse> request = WPComGsonRequest.buildPostRequest(url, null,
                PluginWPComRestResponse.class,
                new Listener<PluginWPComRestResponse>() {
                    @Override
                    public void onResponse(PluginWPComRestResponse response) {
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        DeleteSitePluginError deletePluginError
                                = new DeleteSitePluginError(DeleteSitePluginErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    deletePluginError.type = DeleteSitePluginErrorType.UNAUTHORIZED;
                                    break;
                                case "delete_plugin_error":
                                    deletePluginError.type = DeleteSitePluginErrorType.DELETE_PLUGIN_ERROR;
                                    break;
                            }
                        }
                        deletePluginError.message = networkError.message;
                        DeletedSitePluginPayload payload = new DeletedSitePluginPayload(site, deletePluginError);
                        mDispatcher.dispatch(PluginActionBuilder.newDeletedSitePluginAction(payload));
                    }
                }
        );
        add(request);
    }


    private PluginModel pluginModelFromResponse(SiteModel siteModel, PluginWPComRestResponse response) {
        PluginModel pluginModel = new PluginModel();
        pluginModel.setLocalSiteId(siteModel.getId());
        pluginModel.setName(response.name);
        pluginModel.setDisplayName(response.display_name);
        pluginModel.setAuthorName(response.author);
        pluginModel.setAuthorUrl(response.author_url);
        pluginModel.setDescription(response.description);
        pluginModel.setIsActive(response.active);
        pluginModel.setIsAutoUpdateEnabled(response.autoupdate);
        pluginModel.setPluginUrl(response.plugin_url);
        pluginModel.setSlug(response.slug);
        pluginModel.setVersion(response.version);
        return pluginModel;
    }

    private Map<String, Object> paramsFromPluginModel(PluginModel pluginModel) {
        Map<String, Object> params = new HashMap<>();
        params.put("active", pluginModel.isActive());
        params.put("autoupdate", pluginModel.isAutoUpdateEnabled());
        return params;
    }

    private String getEncodedPluginName(PluginModel plugin) {
        try {
            // We need to encode plugin name otherwise names like "akismet/akismet" would fail
            return URLEncoder.encode(plugin.getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return plugin.getName();
        }
    }
}
