package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import android.content.Context;
import android.support.annotation.NonNull;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeWPComResponse.MultipleWPComThemesResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeJetpackResponse.MultipleJetpackThemesResponse;
import org.wordpress.android.fluxc.store.ThemeStore.ThemeErrorType;
import org.wordpress.android.fluxc.store.ThemeStore.FetchThemesError;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedThemesPayload;
import org.wordpress.android.fluxc.utils.ThemeUtils;
import org.wordpress.android.util.AppLog;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThemeRestClient extends BaseWPComRestClient {
    @Inject
    public ThemeRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /** Endpoint: v1.1/themes/ */
    public void fetchWpComThemes() {
        String url = WPCOMREST.themes.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, MultipleWPComThemesResponse.class,
                new Response.Listener<MultipleWPComThemesResponse>() {
                    @Override
                    public void onResponse(MultipleWPComThemesResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to WP.com themes fetch request.");
                        FetchedThemesPayload payload = new FetchedThemesPayload(null);
                        payload.themes = ThemeUtils.createThemeListFromWPComResponse(response);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.d(AppLog.T.API, "Received error response to WP.com themes fetch request.");
                        FetchThemesError themeError =
                                new FetchThemesError(ThemeErrorType.GENERIC_ERROR, "Error fetching WP.com themes");
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpThemesAction(payload));
                    }
                }));
    }

    /** Endpoint: v1/site/$siteId/themes */
    public void fetchJetpackInstalledThemes(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.getUrlV1();
        add(WPComGsonRequest.buildGetRequest(url, null, MultipleJetpackThemesResponse.class,
                new Response.Listener<MultipleJetpackThemesResponse>() {
                    @Override
                    public void onResponse(MultipleJetpackThemesResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to Jetpack installed themes fetch request.");
                        List<ThemeModel> themes = ThemeUtils.createThemeListFromJetpackResponse(response);
                        FetchedThemesPayload payload = new FetchedThemesPayload(site, themes);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        FetchThemesError themeError = new FetchThemesError(ThemeErrorType.GENERIC_ERROR,
                                "Error fetching installed themes for Jetpack site.");
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                        AppLog.d(AppLog.T.API, "Received error response to Jetpack installed themes fetch request.");
                    }
                }));
    }
}
