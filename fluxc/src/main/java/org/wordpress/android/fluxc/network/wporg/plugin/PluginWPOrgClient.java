package org.wordpress.android.fluxc.network.wporg.plugin;

import android.support.annotation.NonNull;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseErrorListener;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.wporg.BaseWPOrgAPIClient;
import org.wordpress.android.fluxc.network.wporg.WPOrgAPIGsonRequest;
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginError;
import org.wordpress.android.fluxc.store.PluginStore.FetchWPOrgPluginErrorType;
import org.wordpress.android.fluxc.store.PluginStore.FetchedPluginDirectoryPayload;
import org.wordpress.android.fluxc.store.PluginStore.FetchedWPOrgPluginPayload;
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryError;
import org.wordpress.android.fluxc.store.PluginStore.PluginDirectoryErrorType;
import org.wordpress.android.fluxc.store.PluginStore.SearchedPluginDirectoryPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PluginWPOrgClient extends BaseWPOrgAPIClient {
    private static final int FETCH_PLUGIN_DIRECTORY_PAGE_SIZE = 50;
    private final Dispatcher mDispatcher;

    @Inject
    public PluginWPOrgClient(Dispatcher dispatcher, RequestQueue requestQueue, UserAgent userAgent) {
        super(dispatcher, requestQueue, userAgent);
        mDispatcher = dispatcher;
    }

    public void fetchPluginDirectory(final PluginDirectoryType directoryType) {
        String url = WPORGAPI.plugins.info.version("1.1").getUrl() + "?action=query_plugins";
        final Map<String, String> params = getCommonPluginDirectoryParams();
        params.put("request[browse]", directoryType.toString());
        final WPOrgAPIGsonRequest<FetchPluginDirectoryResponse> request =
                new WPOrgAPIGsonRequest<>(Method.POST, url, params, null, FetchPluginDirectoryResponse.class,
                        new Listener<FetchPluginDirectoryResponse>() {
                            @Override
                            public void onResponse(FetchPluginDirectoryResponse response) {
                                // TODO: handle pagination and return correct value for loadMore
                                FetchedPluginDirectoryPayload payload =
                                        new FetchedPluginDirectoryPayload(directoryType, true);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                // TODO: handle pagination and return correct value for loadMore
                                FetchedPluginDirectoryPayload payload =
                                        new FetchedPluginDirectoryPayload(directoryType, true);
                                payload.error = new PluginDirectoryError(
                                        PluginDirectoryErrorType.GENERIC_ERROR, networkError.message);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        }
                );
        add(request);
    }

    public void fetchWPOrgPlugin(final String pluginSlug) {
        String url = WPORGAPI.plugins.info.version("1.0").slug(pluginSlug).getUrl();
        Map<String, String> params = new HashMap<>();
        // TODO: check if we need more fields similar to the ones in getPluginDirectoryParams
        params.put("fields", "banners,icons");
        final WPOrgAPIGsonRequest<WPOrgPluginResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, params, null, WPOrgPluginResponse.class,
                        new Listener<WPOrgPluginResponse>() {
                            @Override
                            public void onResponse(WPOrgPluginResponse response) {
                                if (response == null) {
                                    FetchWPOrgPluginError error = new FetchWPOrgPluginError(
                                            FetchWPOrgPluginErrorType.EMPTY_RESPONSE);
                                    mDispatcher.dispatch(PluginActionBuilder.newFetchedWporgPluginAction(
                                            new FetchedWPOrgPluginPayload(pluginSlug, error)));
                                    return;
                                }
                                WPOrgPluginModel wpOrgPluginModel = wpOrgPluginFromResponse(response);
                                FetchedWPOrgPluginPayload payload =
                                        new FetchedWPOrgPluginPayload(pluginSlug, wpOrgPluginModel);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedWporgPluginAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                FetchWPOrgPluginError error = new FetchWPOrgPluginError(
                                        FetchWPOrgPluginErrorType.GENERIC_ERROR);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedWporgPluginAction(
                                        new FetchedWPOrgPluginPayload(pluginSlug, error)));
                            }
                        }
                );
        add(request);
    }

    public void searchPluginDirectory(final String searchTerm) {
        String url = WPORGAPI.plugins.info.version("1.1").getUrl() + "?action=query_plugins";
        final Map<String, String> params = getCommonPluginDirectoryParams();
        params.put("request[search]", searchTerm);
        final WPOrgAPIGsonRequest<FetchPluginDirectoryResponse> request =
                new WPOrgAPIGsonRequest<>(Method.POST, url, params, null, FetchPluginDirectoryResponse.class,
                        new Listener<FetchPluginDirectoryResponse>() {
                            @Override
                            public void onResponse(FetchPluginDirectoryResponse response) {
                                // TODO: handle pagination and return correct value for offset
                                SearchedPluginDirectoryPayload payload =
                                        new SearchedPluginDirectoryPayload(searchTerm, 0);
                                if (response != null) {
                                    payload.plugins = wpOrgPluginListFromResponse(response);
                                }
                                // TODO: throw an error if the response is null
                                mDispatcher.dispatch(PluginActionBuilder.newSearchedPluginDirectoryAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                // TODO: handle pagination and return correct value for offset
                                SearchedPluginDirectoryPayload payload =
                                        new SearchedPluginDirectoryPayload(searchTerm, 0);
                                payload.error = new PluginDirectoryError(
                                        PluginDirectoryErrorType.GENERIC_ERROR, networkError.message);
                                mDispatcher.dispatch(PluginActionBuilder.newSearchedPluginDirectoryAction(payload));
                            }
                        }
                );
        add(request);
    }

    private Map<String, String> getCommonPluginDirectoryParams() {
        Map<String, String> params = new HashMap<>();
        // TODO: Handle pagination
        params.put("request[page]", String.valueOf(1));
        params.put("request[per_page]", String.valueOf(FETCH_PLUGIN_DIRECTORY_PAGE_SIZE));
        params.put("request[fields][banners]", String.valueOf(1));
        params.put("request[fields][compatibility]", String.valueOf(1));
        params.put("request[fields][icons]", String.valueOf(1));
        params.put("request[fields][requires]", String.valueOf(1));
        params.put("request[fields][sections]", String.valueOf(1));
        params.put("request[fields][tested]", String.valueOf(0));
        return params;
    }

    private List<WPOrgPluginModel> wpOrgPluginListFromResponse(@NonNull FetchPluginDirectoryResponse response) {
        List<WPOrgPluginModel> pluginList = new ArrayList<>();
        if (response.plugins != null) {
            for (WPOrgPluginResponse wpOrgPluginResponse : response.plugins) {
                pluginList.add(wpOrgPluginFromResponse(wpOrgPluginResponse));
            }
        }
        return pluginList;
    }

    private WPOrgPluginModel wpOrgPluginFromResponse(WPOrgPluginResponse response) {
        WPOrgPluginModel wpOrgPluginModel = new WPOrgPluginModel();
        wpOrgPluginModel.setAuthorAsHtml(response.authorAsHtml);
        wpOrgPluginModel.setBanner(response.banner);
        wpOrgPluginModel.setDescriptionAsHtml(response.descriptionAsHtml);
        wpOrgPluginModel.setFaqAsHtml(response.faqAsHtml);
        wpOrgPluginModel.setHomepageUrl(response.homepageUrl);
        wpOrgPluginModel.setIcon(response.icon);
        wpOrgPluginModel.setInstallationInstructionsAsHtml(response.installationInstructionsAsHtml);
        wpOrgPluginModel.setLastUpdated(response.lastUpdated);
        wpOrgPluginModel.setName(response.name);
        wpOrgPluginModel.setRating(response.rating);
        wpOrgPluginModel.setRequiredWordPressVersion(response.requiredWordPressVersion);
        wpOrgPluginModel.setSlug(response.slug);
        wpOrgPluginModel.setVersion(response.version);
        wpOrgPluginModel.setWhatsNewAsHtml(response.whatsNewAsHtml);
        wpOrgPluginModel.setDownloadCount(response.downloadCount);
        wpOrgPluginModel.setNumberOfRatings(response.numberOfRatings);
        wpOrgPluginModel.setNumberOfRatingsOfOne(response.numberOfRatingsOfOne);
        wpOrgPluginModel.setNumberOfRatingsOfTwo(response.numberOfRatingsOfTwo);
        wpOrgPluginModel.setNumberOfRatingsOfThree(response.numberOfRatingsOfThree);
        wpOrgPluginModel.setNumberOfRatingsOfFour(response.numberOfRatingsOfFour);
        wpOrgPluginModel.setNumberOfRatingsOfFive(response.numberOfRatingsOfFive);
        return wpOrgPluginModel;
    }
}
