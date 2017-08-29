package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeJetpackResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeWPComResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeWPComResponse.MultipleWPComThemesResponse;

import java.util.ArrayList;
import java.util.List;

public class ThemeUtils {
    public static ThemeModel createThemeFromWPComResponse(ThemeWPComResponse response) {
        ThemeModel theme = new ThemeModel();
        theme.setThemeId(response.id);
        theme.setName(response.name);
        theme.setAuthorName(response.author);
        theme.setScreenshotUrl(response.screenshot);
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
    public static List<ThemeModel> createThemeListFromJetpackResponse(ThemeJetpackResponse.MultipleJetpackThemesResponse response) {
        List<ThemeModel> themeList = new ArrayList<>();
        for (ThemeJetpackResponse item : response.themes) {
            ThemeModel theme = new ThemeModel();
            theme.setThemeId(item.id);
            theme.setName(item.name);
            theme.setAuthorName(item.author);

            // the screenshot field in Jetpack responses does not contain a protocol so we'll prepend 'https'
            String screenshotUrl = item.screenshot;
            if (!TextUtils.isEmpty(screenshotUrl) && screenshotUrl.startsWith("//")) {
                screenshotUrl = screenshotUrl + "https";
            }
            theme.setScreenshotUrl(screenshotUrl);

            themeList.add(theme);
        }
        return themeList;
    }
}
