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
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemeError;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThemeRestClient extends BaseWPComRestClient {
    @Inject
    public ThemeRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /** Endpoint: v1.1/site/$siteId/themes/mine */
    public void activateTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("theme", theme.getThemeId());

        add(WPComGsonRequest.buildPostRequest(url, params, ThemeWPComResponse.class,
                new Response.Listener<ThemeWPComResponse>() {
                    @Override
                    public void onResponse(ThemeWPComResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to theme activation request.");
                        ActivateThemePayload payload = new ActivateThemePayload(site, theme);
                        payload.theme.setActive(true);
                        mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.d(AppLog.T.API, "Received error response to theme activation request.");
                        ActivateThemeError themeError = new ActivateThemeError(ThemeErrorType.GENERIC_ERROR, null);
                        ActivateThemePayload payload = new ActivateThemePayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.1/themes */
    public void fetchWpComThemes() {
        String url = WPCOMREST.themes.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, MultipleWPComThemesResponse.class,
                new Response.Listener<MultipleWPComThemesResponse>() {
                    @Override
                    public void onResponse(MultipleWPComThemesResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to WP.com themes fetch request.");
                        FetchedThemesPayload payload = new FetchedThemesPayload(null);
                        payload.themes = createThemeListFromWPComResponse(response);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to WP.com themes fetch request.");
                        FetchThemesError themeError = new FetchThemesError(ThemeErrorType.GENERIC_ERROR, null);
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
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
                        List<ThemeModel> themes = createThemeListFromJetpackResponse(response);
                        FetchedThemesPayload payload = new FetchedThemesPayload(site, themes);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to Jetpack installed themes fetch request.");
                        FetchThemesError themeError = new FetchThemesError(ThemeErrorType.GENERIC_ERROR, null);
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.2/site/$siteId/themes */
    public void fetchWpComSiteThemes(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.getUrlV1_2();
        add(WPComGsonRequest.buildGetRequest(url, null, MultipleWPComThemesResponse.class,
                new Response.Listener<MultipleWPComThemesResponse>() {
                    @Override
                    public void onResponse(MultipleWPComThemesResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to themes fetch request for WP.com site.");
                        FetchedThemesPayload payload = new FetchedThemesPayload(null);
                        payload.themes = createThemeListFromWPComResponse(response);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to themes fetch request for WP.com site.");
                        FetchThemesError themeError = new FetchThemesError(ThemeErrorType.GENERIC_ERROR, null);
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.1/site/$siteId/themes/mine; same endpoint for both Jetpack and WP.com sites */
    public void fetchCurrentTheme(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, ThemeWPComResponse.class,
                new Response.Listener<ThemeWPComResponse>() {
                    @Override
                    public void onResponse(ThemeWPComResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to current theme fetch request.");
                        ThemeModel responseTheme = createThemeFromWPComResponse(response);
                        FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, responseTheme);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseRequest.BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to current theme fetch request.");
                        FetchThemesError themeError = new FetchThemesError(ThemeErrorType.GENERIC_ERROR, null);
                        FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                    }
                }));
    }

    private static ThemeModel createThemeFromWPComResponse(ThemeWPComResponse response) {
        ThemeModel theme = new ThemeModel();
        theme.setThemeId(response.id);
        theme.setSlug(response.slug);
        theme.setStylesheet(response.stylesheet);
        theme.setName(response.name);
        theme.setAuthorName(response.author);
        theme.setAuthorUrl(response.author_uri);
        theme.setThemeUrl(response.theme_uri);
        theme.setDemoUrl(response.demo_uri);
        theme.setVersion(response.version);
        theme.setScreenshotUrl(response.screenshot);
        theme.setDescription(response.description);
        theme.setDownloadUrl(response.download_uri);
        return theme;
    }

    private static ThemeModel createThemeFromJetpackResponse(ThemeJetpackResponse response) {
        ThemeModel theme = new ThemeModel();
        theme.setThemeId(response.id);
        theme.setName(response.name);
        theme.setThemeUrl(response.theme_uri);
        theme.setDescription(response.description);
        theme.setAuthorName(response.author);
        theme.setAuthorUrl(response.author_uri);
        theme.setVersion(response.version);
        theme.setActive(response.active);
        theme.setAutoUpdate(response.autoupdate);
        theme.setAutoUpdateTranslation(response.autoupdate_translation);

        // the screenshot field in Jetpack responses does not contain a protocol so we'll prepend 'https'
        String screenshotUrl = response.screenshot;
        if (screenshotUrl != null && screenshotUrl.startsWith("//")) {
            screenshotUrl = "https" + screenshotUrl;
        }
        theme.setScreenshotUrl(screenshotUrl);

        return theme;
    }

    /** Creates a list of ThemeModels from the WP.com /v1.1/themes REST response. */
    private static List<ThemeModel> createThemeListFromWPComResponse(MultipleWPComThemesResponse response) {
        List<ThemeModel> themeList = new ArrayList<>();
        for (ThemeWPComResponse item : response.themes.values()) {
            themeList.add(createThemeFromWPComResponse(item));
        }
        return themeList;
    }

    /** Creates a list of ThemeModels from the Jetpack /v1/sites/$siteId/themes REST response. */
    private static List<ThemeModel> createThemeListFromJetpackResponse(MultipleJetpackThemesResponse response) {
        List<ThemeModel> themeList = new ArrayList<>();
        for (ThemeJetpackResponse item : response.themes) {
            themeList.add(createThemeFromJetpackResponse(item));
        }
        return themeList;
    }
}
