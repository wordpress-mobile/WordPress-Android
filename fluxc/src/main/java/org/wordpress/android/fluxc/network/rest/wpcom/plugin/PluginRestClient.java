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
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginsError;
import org.wordpress.android.fluxc.store.PluginStore.FetchPluginsErrorType;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginsPayload;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginRestClient extends BaseWPComRestClient {
    @Inject
    public PluginRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                              AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    public void fetchPlugins(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).plugins.getUrlV1_1();
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
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginsAction(new
                                FetchedPluginsPayload(site, plugins)));
                    }
                },
                new BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                        FetchPluginsError fetchPluginsError
                                = new FetchPluginsError(FetchPluginsErrorType.GENERIC_ERROR);
                        if (networkError instanceof WPComGsonNetworkError) {
                            switch (((WPComGsonNetworkError) networkError).apiError) {
                                case "unauthorized":
                                    fetchPluginsError.type = FetchPluginsErrorType.UNAUTHORIZED;
                                    break;
                            }
                        }
                        fetchPluginsError.message = networkError.message;
                        FetchedPluginsPayload payload = new FetchedPluginsPayload(fetchPluginsError);
                        mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginsAction(payload));
                    }
                }
        );
        add(request);
    }

    private PluginModel pluginModelFromResponse(SiteModel siteModel, PluginWPComRestResponse response) {
        PluginModel pluginModel = new PluginModel();
        pluginModel.setLocalSiteId(siteModel.getId());
        pluginModel.setName(response.id);
        pluginModel.setDisplayName(response.name);
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
}
