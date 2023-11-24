package org.wordpress.android.fluxc.network.rest.wpcom.theme;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.RequestQueue;

import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMREST;
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.UserAgent;
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.JetpackThemeResponse.JetpackThemeListResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.WPComThemeResponse.WPComThemeListResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.WPComThemeResponse.WPComThemeMobileFriendlyTaxonomy;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedCurrentThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedSiteThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedStarterDesignsPayload;
import org.wordpress.android.fluxc.store.ThemeStore.FetchedWpComThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;
import org.wordpress.android.fluxc.store.ThemeStore.ThemesError;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ThemeRestClient extends BaseWPComRestClient {
    private static final String WPCOM_MOBILE_FRIENDLY_TAXONOMY_SLUG = "mobile-friendly";
    private static final String THEME_TYPE_EXTERNAL = "managed-external";

    @Inject public ThemeRestClient(
            Context appContext,
            Dispatcher dispatcher,
            @Named("regular") RequestQueue requestQueue,
            AccessToken accessToken,
            UserAgent userAgent) {
        super(appContext, dispatcher, requestQueue, accessToken, userAgent);
    }

    /**
     * [Undocumented!] Endpoint: v1.1/sites/$siteId/themes/$themeId/delete
     */
    public void deleteTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.theme(theme.getThemeId()).delete.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, JetpackThemeResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to Jetpack theme deletion request.");
                    ThemeModel responseTheme = createThemeFromJetpackResponse(response);
                    responseTheme.setId(theme.getId());
                    SiteThemePayload payload = new SiteThemePayload(site, responseTheme);
                    mDispatcher.dispatch(ThemeActionBuilder.newDeletedThemeAction(payload));
                }, error -> {
                    AppLog.d(AppLog.T.API, "Received error response to Jetpack theme deletion request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.error = new ThemesError(error.apiError, error.message);
                    mDispatcher.dispatch(ThemeActionBuilder.newDeletedThemeAction(payload));
                }));
    }

    /**
     * [Undocumented!] Endpoint: v1.1/sites/$siteId/themes/$themeId/install
     */
    public void installTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String themeId = getThemeIdWithWpComSuffix(theme);
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.theme(themeId).install.getUrlV1_1();
        add(WPComGsonRequest.buildPostRequest(url, null, JetpackThemeResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to Jetpack theme installation request.");
                    ThemeModel responseTheme = createThemeFromJetpackResponse(response);
                    SiteThemePayload payload = new SiteThemePayload(site, responseTheme);
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                }, error -> {
                    AppLog.d(AppLog.T.API, "Received error response to Jetpack theme installation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.error = new ThemesError(error.apiError, error.message);
                    mDispatcher.dispatch(ThemeActionBuilder.newInstalledThemeAction(payload));
                }));
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    public void activateTheme(@NonNull final SiteModel site, @NonNull final ThemeModel theme) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        Map<String, Object> params = new HashMap<>();
        params.put("theme", theme.getThemeId());

        add(WPComGsonRequest.buildPostRequest(url, params, WPComThemeResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to theme activation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.theme.setActive(StringUtils.equals(theme.getThemeId(), response.id));
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                }, error -> {
                    AppLog.d(AppLog.T.API, "Received error response to theme activation request.");
                    SiteThemePayload payload = new SiteThemePayload(site, theme);
                    payload.error = new ThemesError(error.apiError, error.message);
                    mDispatcher.dispatch(ThemeActionBuilder.newActivatedThemeAction(payload));
                }));
    }

    /**
     * [Undocumented!] Endpoint: v1.2/themes
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/themes/">Previous version</a>
     */
    public void fetchWpComThemes(@Nullable String filter, int resultsLimit) {
        String url = WPCOMREST.themes.getUrlV1_2();
        Map<String, String> params = new HashMap<>();
        params.put("number", String.valueOf(resultsLimit));
        if (filter != null) {
            params.put("filter", filter);
        }
        add(WPComGsonRequest.buildGetRequest(url, params, WPComThemeListResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to WP.com themes fetch request.");
                    List<ThemeModel> themes = createThemeListFromArrayResponse(response);
                    FetchedWpComThemesPayload payload = new FetchedWpComThemesPayload(themes);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to WP.com themes fetch request.");
                    ThemesError themeError = new ThemesError(
                            error.apiError, error.message);
                    FetchedWpComThemesPayload payload = new FetchedWpComThemesPayload(themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedWpComThemesAction(payload));
                }));
    }

    /**
     * Endpoint:  v2/common-starter-site-designs
     */
    public void fetchStarterDesigns(
            @Nullable Float previewWidth,
            @Nullable Float previewHeight,
            @Nullable Float scale,
            @Nullable String[] groups) {
        Map<String, String> params = new HashMap<>();
        params.put("type", "mobile");
        if (previewWidth != null) {
            params.put("preview_width", String.format(Locale.US, "%.1f", previewWidth));
        }
        if (previewHeight != null) {
            params.put("preview_height", String.format(Locale.US, "%.1f", previewHeight));
        }
        if (scale != null) {
            params.put("scale", String.format(Locale.US, "%.1f", scale));
        }
        if (groups != null && groups.length > 0) {
            params.put("group", TextUtils.join(",", groups));
        }
        String url = WPCOMV2.common_starter_site_designs.getUrl();
        add(WPComGsonRequest.buildGetRequest(url, params, StarterDesignsResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to WP.com starter designs fetch request.");
                    FetchedStarterDesignsPayload payload =
                            new FetchedStarterDesignsPayload(response.getDesigns(), response.getCategories());
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedStarterDesignsAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to WP.com starter designs fetch request.");
                    ThemesError themeError = new ThemesError(error.apiError, error.message);
                    FetchedStarterDesignsPayload payload = new FetchedStarterDesignsPayload(themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedStarterDesignsAction(payload));
                }));
    }

    /**
     * [Undocumented!] Endpoint: v1/sites/$siteId/themes
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/">Similar endpoint</a>
     */
    public void fetchJetpackInstalledThemes(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.getUrlV1();
        add(WPComGsonRequest.buildGetRequest(url, null, JetpackThemeListResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to Jetpack installed themes fetch request.");
                    List<ThemeModel> themes = createThemeListFromJetpackResponse(response);
                    FetchedSiteThemesPayload payload = new FetchedSiteThemesPayload(site, themes);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to Jetpack installed themes fetch request.");
                    ThemesError themeError = new ThemesError(error.apiError, error.message);
                    FetchedSiteThemesPayload payload = new FetchedSiteThemesPayload(site, themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedInstalledThemesAction(payload));
                }));
    }

    /**
     * Endpoint: v1.1/sites/$siteId/themes/mine; same endpoint for both Jetpack and WP.com sites!
     *
     * @see <a href="https://developer.wordpress.com/docs/api/1.1/get/sites/%24site/themes/mine/">Documentation</a>
     */
    public void fetchCurrentTheme(@NonNull final SiteModel site) {
        String url = WPCOMREST.sites.site(site.getSiteId()).themes.mine.getUrlV1_1();
        add(WPComGsonRequest.buildGetRequest(url, null, WPComThemeResponse.class,
                response -> {
                    AppLog.d(AppLog.T.API, "Received response to current theme fetch request.");
                    ThemeModel responseTheme = createThemeFromWPComResponse(response);
                    FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, responseTheme);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                }, error -> {
                    AppLog.e(AppLog.T.API, "Received error response to current theme fetch request.");
                    ThemesError themeError = new ThemesError(error.apiError, error.message);
                    FetchedCurrentThemePayload payload = new FetchedCurrentThemePayload(site, themeError);
                    mDispatcher.dispatch(ThemeActionBuilder.newFetchedCurrentThemeAction(payload));
                }));
    }

    @NonNull
    private static ThemeModel createThemeFromWPComResponse(@NonNull WPComThemeResponse response) {
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
        theme.setThemeType(response.theme_type);
        if (response.theme_type != null) {
            theme.setIsExternalTheme(response.theme_type.equals(THEME_TYPE_EXTERNAL));
        }
        theme.setDescription(response.description);
        theme.setDownloadUrl(response.download_uri);
        if (TextUtils.isEmpty(response.price)) {
            theme.setFree(true);
        } else {
            theme.setFree(false);
            theme.setPriceText(response.price);
        }

        // detect the mobile-friendly category slug if there
        if (response.taxonomies != null && response.taxonomies.theme_mobile_friendly != null) {
            String category = null;
            for (WPComThemeMobileFriendlyTaxonomy taxonomy : response.taxonomies.theme_mobile_friendly) {
                // The server response has two taxonomies defined here. One is named "mobile-friendly" and the other is
                //  a more specific category the theme belongs to. We're only interested in the specific one here so,
                //  ignore the "mobile-friendly" one.
                if (taxonomy.slug.equals(WPCOM_MOBILE_FRIENDLY_TAXONOMY_SLUG)) {
                    continue;
                }

                category = taxonomy.slug;

                // we got the category slug so, no need to continue looping
                break;
            }
            theme.setMobileFriendlyCategorySlug(category);
        }

        return theme;
    }

    @NonNull
    private static ThemeModel createThemeFromJetpackResponse(@NonNull JetpackThemeResponse response) {
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
        if (screenshotUrl.startsWith("//")) {
            screenshotUrl = "https:" + screenshotUrl;
        }
        theme.setScreenshotUrl(screenshotUrl);

        return theme;
    }

    @NonNull
    private static List<ThemeModel> createThemeListFromArrayResponse(@NonNull WPComThemeListResponse response) {
        final List<ThemeModel> themeList = new ArrayList<>();
        for (WPComThemeResponse item : response.themes) {
            themeList.add(createThemeFromWPComResponse(item));
        }
        return themeList;
    }

    /**
     * Creates a list of ThemeModels from the Jetpack /v1/sites/$siteId/themes REST response.
     */
    @NonNull
    private static List<ThemeModel> createThemeListFromJetpackResponse(@NonNull JetpackThemeListResponse response) {
        final List<ThemeModel> themeList = new ArrayList<>();
        for (JetpackThemeResponse item : response.themes) {
            themeList.add(createThemeFromJetpackResponse(item));
        }
        return themeList;
    }

    /**
     * Must provide theme slug with -wpcom suffix to install a WP.com theme on a Jetpack site.
     *
     * @see <a href="https://developer.wordpress.com/docs/api/console/">Documentation</a>
     */
    @NonNull
    private String getThemeIdWithWpComSuffix(@NonNull ThemeModel theme) {
        if (theme.getThemeId() == null) {
            return "";
        } else if (theme.getThemeId().endsWith("-wpcom")) {
            return theme.getThemeId();
        }

        return theme.getThemeId() + "-wpcom";
    }
}
