package org.wordpress.android.fluxc.network.wporg.plugin;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response.Listener;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PluginActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;
import org.wordpress.android.fluxc.generated.endpoint.WPORGAPI;
import org.wordpress.android.fluxc.model.SiteModel;
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

    public void fetchPluginDirectory(final PluginDirectoryType directoryType, int page) {
        if (directoryType == PluginDirectoryType.FEATURED) {
            // This check is not really necessary currently - but defensive programming ftw
            fetchFeaturedPlugins();
            return;
        }
        String url = WPORGAPI.plugins.info.version("1.1").getUrl();
        final boolean loadMore = page > 1;
        final Map<String, String> params = getCommonPluginDirectoryParams(page);
        params.put("request[browse]", directoryType.toString());
        final WPOrgAPIGsonRequest<FetchPluginDirectoryResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, params, null, FetchPluginDirectoryResponse.class,
                        new Listener<FetchPluginDirectoryResponse>() {
                            @Override
                            public void onResponse(FetchPluginDirectoryResponse response) {
                                FetchedPluginDirectoryPayload payload;
                                if (response != null) {
                                    boolean canLoadMore = response.info.page < response.info.pages;
                                    List<WPOrgPluginModel> wpOrgPlugins = wpOrgPluginListFromResponse(response);
                                    payload = new FetchedPluginDirectoryPayload(directoryType, wpOrgPlugins,
                                            loadMore, canLoadMore, response.info.page);
                                } else {
                                    PluginDirectoryError directoryError = new PluginDirectoryError(
                                            PluginDirectoryErrorType.EMPTY_RESPONSE, null);
                                    payload = new FetchedPluginDirectoryPayload(directoryType, loadMore,
                                            directoryError);
                                }
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                PluginDirectoryError directoryError = new PluginDirectoryError(
                                        PluginDirectoryErrorType.GENERIC_ERROR, networkError.message);
                                FetchedPluginDirectoryPayload payload =
                                        new FetchedPluginDirectoryPayload(directoryType, loadMore, directoryError);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        }
                );
        add(request);
    }

    public void fetchFeaturedPlugins() {
        String url = WPCOMV2.plugins.featured.getUrl();
        final WPOrgAPIGsonRequest<WPOrgPluginResponse[]> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, null, null, WPOrgPluginResponse[].class,
                        new Listener<WPOrgPluginResponse[]>() {
                            @Override
                            public void onResponse(WPOrgPluginResponse[] response) {
                                FetchedPluginDirectoryPayload payload;
                                List<WPOrgPluginModel> wpOrgPlugins = new ArrayList<>();
                                if (response != null) {
                                    for (WPOrgPluginResponse wpOrgPluginResponse : response) {
                                        wpOrgPlugins.add(wpOrgPluginFromResponse(wpOrgPluginResponse));
                                    }
                                }
                                payload = new FetchedPluginDirectoryPayload(PluginDirectoryType.FEATURED, wpOrgPlugins,
                                        false, false, 1);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                PluginDirectoryError directoryError = new PluginDirectoryError(
                                        PluginDirectoryErrorType.GENERIC_ERROR, networkError.message);
                                FetchedPluginDirectoryPayload payload =
                                        new FetchedPluginDirectoryPayload(PluginDirectoryType.FEATURED, false,
                                                directoryError);
                                mDispatcher.dispatch(PluginActionBuilder.newFetchedPluginDirectoryAction(payload));
                            }
                        }
                );
        add(request);
    }

    public void fetchWPOrgPlugin(final String pluginSlug) {
        String url = WPORGAPI.plugins.info.version("1.0").slug(pluginSlug).getUrl();
        Map<String, String> params = new HashMap<>();
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
                                if (!TextUtils.isEmpty(response.errorMessage)) {
                                    // Plugin does not exist error returned with success code
                                    FetchWPOrgPluginError error = new FetchWPOrgPluginError(
                                            FetchWPOrgPluginErrorType.PLUGIN_DOES_NOT_EXIST);
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

    public void searchPluginDirectory(@Nullable final SiteModel site, final String searchTerm, final int page) {
        String url = WPORGAPI.plugins.info.version("1.1").getUrl();
        final Map<String, String> params = getCommonPluginDirectoryParams(page);
        params.put("request[search]", searchTerm);
        final WPOrgAPIGsonRequest<FetchPluginDirectoryResponse> request =
                new WPOrgAPIGsonRequest<>(Method.GET, url, params, null, FetchPluginDirectoryResponse.class,
                        new Listener<FetchPluginDirectoryResponse>() {
                            @Override
                            public void onResponse(FetchPluginDirectoryResponse response) {
                                SearchedPluginDirectoryPayload payload =
                                        new SearchedPluginDirectoryPayload(site, searchTerm, page);
                                if (response != null) {
                                    payload.canLoadMore = response.info.page < response.info.pages;
                                    payload.plugins = wpOrgPluginListFromResponse(response);
                                } else {
                                    payload.error = new PluginDirectoryError(
                                            PluginDirectoryErrorType.EMPTY_RESPONSE, null);
                                }
                                mDispatcher.dispatch(PluginActionBuilder.newSearchedPluginDirectoryAction(payload));
                            }
                        },
                        new BaseErrorListener() {
                            @Override
                            public void onErrorResponse(@NonNull BaseNetworkError networkError) {
                                SearchedPluginDirectoryPayload payload =
                                        new SearchedPluginDirectoryPayload(site, searchTerm, page);
                                payload.error = new PluginDirectoryError(
                                        PluginDirectoryErrorType.GENERIC_ERROR, networkError.message);
                                mDispatcher.dispatch(PluginActionBuilder.newSearchedPluginDirectoryAction(payload));
                            }
                        }
                );
        add(request);
    }

    private Map<String, String> getCommonPluginDirectoryParams(int page) {
        Map<String, String> params = new HashMap<>();
        params.put("action", "query_plugins");
        params.put("request[page]", String.valueOf(page));
        params.put("request[per_page]", String.valueOf(FETCH_PLUGIN_DIRECTORY_PAGE_SIZE));
        params.put("request[fields][banners]", String.valueOf(1));
        params.put("request[fields][compatibility]", String.valueOf(1));
        params.put("request[fields][icons]", String.valueOf(1));
        params.put("request[fields][requires]", String.valueOf(1));
        params.put("request[fields][sections]", String.valueOf(0));
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
        wpOrgPluginModel.setDisplayName(StringEscapeUtils.unescapeHtml4(response.name));
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
