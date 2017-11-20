package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.WPComThemeResponse.WPComThemeListResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.JetpackThemeResponse.JetpackThemeListResponse;
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError;
import org.wordpress.android.fluxc.store.ThemeStore.SearchedThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemePayload;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ThemeRestClient extends BaseWPComRestClient {
    private static final String WP_THEME_FETCH_NUMBER_PARAM = "number=500";

    @Inject
    public ThemeRestClient(Context appContext, Dispatcher dispatcher, RequestQueue requestQueue,
                           AccessToken accessToken, UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /** Endpoint: v1.1/site/$siteId/themes/$themeId/delete */
    public void deleteTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.theme(theme.getThemeId()).delete.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, JetpackThemeResponse.class,
                new Response.Listener<JetpackThemeResponse>() {
                    @Override
                    public void onResponse(JetpackThemeResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to Jetpack theme deletion request.");
                        ThemeModel responseTheme = createThemeFromJetpackResponse(response);
                        responseTheme.setId(theme.getId());
                        responseTheme.setLocalSiteId(site.getId());
                        ActivateThemePayload payload = new ActivateThemePayload(site, responseTheme);
                        mDispatcher.dispatch(ThemeActionBuilder.newDeletedThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.d(AppLog.T.API, "Received error response to Jetpack theme deletion request.");
                        ActivateThemePayload payload = new ActivateThemePayload(site, theme);
                        payload.error = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        mDispatcher.dispatch(ThemeActionBuilder.newDeletedThemeAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.1/site/$siteId/themes/$themeId/install */
    public void installTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String themeId = getThemeIdWithWpComSuffix(theme);
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.theme(themeId).install.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, JetpackThemeResponse.class,
                new Response.Listener<JetpackThemeResponse>() {
                    @Override
                    public void onResponse(JetpackThemeResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to Jetpack theme installation request.");
                        ThemeModel responseTheme = createThemeFromJetpackResponse(response);
                        responseTheme.setLocalSiteId(site.getId());
                        ActivateThemePayload payload = new ActivateThemePayload(site, responseTheme);
                        mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.d(AppLog.T.API, "Received error response to Jetpack theme installation request.");
                        ActivateThemePayload payload = new ActivateThemePayload(site, theme);
                        payload.error = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.1/site/$siteId/themes/mine */
    public void activateTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("theme", theme.getThemeId());

        add(WPComGsonRequest.buildPostRequest(url, params, WPComThemeResponse.class,
                new Response.Listener<WPComThemeResponse>() {
                    @Override
                    public void onResponse(WPComThemeResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to theme activation request.");
                        ActivateThemePayload payload = new ActivateThemePayload(site, theme);
                        payload.theme.setActive(StringUtils.equals(theme.getThemeId(), response.id));
                        mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.d(AppLog.T.API, "Received error response to theme activation request.");
                        ActivateThemePayload payload = new ActivateThemePayload(site, theme);
                        payload.error = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                    }
                }));
    }

    /** [Undocumented!] Endpoint: v1.2/themes */
    public void fetchWpComThemes() {
        String url = WPCOMREST.themes.getUrlV1_2() + "?" + WP_THEME_FETCH_NUMBER_PARAM;
        add(WPComGsonRequest.buildGetRequest(url, null, WPComThemeListResponse.class,
                new Response.Listener<WPComThemeListResponse>() {
                    @Override
                    public void onResponse(WPComThemeListResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to WP.com themes fetch request.");
                        FetchedThemesPayload payload = new FetchedThemesPayload(null);
                        payload.themes = createThemeListFromArrayResponse(response);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to WP.com themes fetch request.");
                        ThemesError themeError = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                    }
                }));
    }

    /** Endpoint: v1/site/$siteId/themes */
    public void fetchJetpackInstalledThemes(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.getUrlV1();
        add(WPComGsonRequest.buildGetRequest(url, null, JetpackThemeListResponse.class,
                new Response.Listener<JetpackThemeListResponse>() {
                    @Override
                    public void onResponse(JetpackThemeListResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to Jetpack installed themes fetch request.");
                        List<ThemeModel> themes = createThemeListFromJetpackResponse(response);
                        FetchedThemesPayload payload = new FetchedThemesPayload(site, themes);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to Jetpack installed themes fetch request.");
                        ThemesError themeError = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        FetchedThemesPayload payload = new FetchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                    }
                }));
    }

    /** Endpoint: v1.1/site/$siteId/themes/mine; same endpoint for both Jetpack and WP.com sites */
    public void fetchCurrentTheme(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, WPComThemeResponse.class,
                new Response.Listener<WPComThemeResponse>() {
                    @Override
                    public void onResponse(WPComThemeResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to current theme fetch request.");
                        ThemeModel responseTheme = createThemeFromWPComResponse(response);
                        FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, responseTheme);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to current theme fetch request.");
                        ThemesError themeError = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                    }
                }));
    }

    /** [Undocumented!] Endpoint: v1.2/themes?search=$term */
    public void searchThemes(@NonNull final String searchTerm) {
        String url = WPCOMREST.themes.getUrlV1_2() + "?search=" + searchTerm;
        add(WPComGsonRequest.buildGetRequest(url, null, WPComThemeListResponse.class,
                new Response.Listener<WPComThemeListResponse>() {
                    @Override
                    public void onResponse(WPComThemeListResponse response) {
                        AppLog.d(AppLog.T.API, "Received response to search themes request.");
                        SearchedThemesPayload payload =
                                new SearchedThemesPayload(searchTerm, createThemeListFromArrayResponse(response));
                        mDispatcher.dispatch(ThemeActionBuilder.newSearchedThemesAction(payload));
                    }
                }, new BaseRequest.BaseErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull BaseNetworkError error) {
                        AppLog.e(AppLog.T.API, "Received error response to search themes request.");
                        ThemesError themeError = new ThemesError(
                                ((WPComGsonRequest.WPComGsonNetworkError) error).apiError, error.message);
                        SearchedThemesPayload payload = new SearchedThemesPayload(themeError);
                        mDispatcher.dispatch(ThemeActionBuilder.newSearchedThemesAction(payload));
                    }
                }));
    }

    private static ThemeModel createThemeFromWPComResponse(WPComThemeResponse response) {
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
        if (!TextUtils.isEmpty(response.price)) {
            theme.setCurrency(response.price.substring(0, 1));
            theme.setPrice(Integer.valueOf(response.price.substring(1)));
        }
        return theme;
    }

    private static ThemeModel createThemeFromJetpackResponse(JetpackThemeResponse response) {
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
            screenshotUrl = "https:" + screenshotUrl;
        }
        theme.setScreenshotUrl(screenshotUrl);

        return theme;
    }

    private static List<ThemeModel> createThemeListFromArrayResponse(WPComThemeListResponse response) {
        final List<ThemeModel> themeList = new ArrayList<>();
        for (WPComThemeResponse item : response.themes) {
            themeList.add(createThemeFromWPComResponse(item));
        }
        return themeList;
    }

    /** Creates a list of ThemeModels from the Jetpack /v1/sites/$siteId/themes REST response. */
    private static List<ThemeModel> createThemeListFromJetpackResponse(JetpackThemeListResponse response) {
        final List<ThemeModel> themeList = new ArrayList<>();
        for (JetpackThemeResponse item : response.themes) {
            themeList.add(createThemeFromJetpackResponse(item));
        }
        return themeList;
    }

    /**
     * Must provide theme slug with -wpcom suffix to install a WP.com theme on a Jetpack site.
     * Per documentation in the developer console: https://developer.wordpress.com/docs/api/console/
     */
    private @NonNull String getThemeIdWithWpComSuffix(ThemeModel theme) {
        if (theme == null || theme.getThemeId() == null) {
            return "";
        } else if (theme.getThemeId().endsWith("-wpcom")) {
            return theme.getThemeId();
        }

        return theme.getThemeId() + "-wpcom";
    }
}
