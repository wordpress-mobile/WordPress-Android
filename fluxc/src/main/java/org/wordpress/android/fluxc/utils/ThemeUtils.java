package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeJetpackResponse.MultipleJetpackThemesResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeJetpackResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeWPComResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeWPComResponse.MultipleWPComThemesResponse;

import java.util.ArrayList;
import java.util.List;

public class ThemeUtils {
    public static ThemeModel createThemeFromWPComResponse(ThemeWPComResponse response) {
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

    public static ThemeModel createThemeFromJetpackResponse(ThemeJetpackResponse response) {
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
        if (!TextUtils.isEmpty(screenshotUrl) && screenshotUrl.startsWith("//")) {
            screenshotUrl = screenshotUrl + "https";
        }
        theme.setScreenshotUrl(screenshotUrl);

        return theme;
    }

    /** Creates a list of ThemeModels from the WP.com /v1.1/themes REST response. */
    public static List<ThemeModel> createThemeListFromWPComResponse(MultipleWPComThemesResponse response) {
        List<ThemeModel> themeList = new ArrayList<>();
        for (ThemeWPComResponse item : response.themes.values()) {
            themeList.add(createThemeFromWPComResponse(item));
        }
        return themeList;
    }

    /** Creates a list of ThemeModels from the Jetpack /v1/sites/$siteId/themes REST response. */
    public static List<ThemeModel> createThemeListFromJetpackResponse(MultipleJetpackThemesResponse response) {
        List<ThemeModel> themeList = new ArrayList<>();
        for (ThemeJetpackResponse item : response.themes) {
            themeList.add(createThemeFromJetpackResponse(item));
        }
        return themeList;
    }
}
