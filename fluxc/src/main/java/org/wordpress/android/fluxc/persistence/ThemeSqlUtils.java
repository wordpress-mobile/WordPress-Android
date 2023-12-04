package org.wordpress.android.fluxc.persistence;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wellsql.generated.ThemeModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;

import java.util.List;

public class ThemeSqlUtils {
    public static void insertOrUpdateSiteTheme(@NonNull SiteModel site, @NonNull ThemeModel theme) {
        List<ThemeModel> existing = WellSql.select(ThemeModel.class)
                .where().beginGroup()
                .equals(ThemeModelTable.THEME_ID, theme.getThemeId())
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .equals(ThemeModelTable.IS_WP_COM_THEME, false)
                .endGroup().endWhere().getAsModel();

        // Make sure the local id of the theme is set correctly
        theme.setLocalSiteId(site.getId());
        // Always remove WP.com flag while storing as a site associate theme as we might be saving
        // a copy of a wp.com theme after an activation
        theme.setIsWpComTheme(false);

        if (existing.isEmpty()) {
            // theme is not in the local DB so we insert it
            WellSql.insert(theme).asSingleTransaction(true).execute();
        } else {
            // theme already exists in the local DB so we update the existing row with the passed theme
            WellSql.update(ThemeModel.class).whereId(existing.get(0).getId())
                    .put(theme, new UpdateAllExceptId<>(ThemeModel.class)).execute();
        }
    }

    public static void insertOrReplaceWpComThemes(@NonNull List<ThemeModel> themes) {
        // remove existing WP.com themes
        removeWpComThemes();

        // ensure WP.com flag is set before inserting
        for (ThemeModel theme : themes) {
            theme.setIsWpComTheme(true);
        }

        WellSql.insert(themes).asSingleTransaction(true).execute();
    }

    public static void insertOrReplaceInstalledThemes(@NonNull SiteModel site, @NonNull List<ThemeModel> themes) {
        // remove existing installed themes
        removeSiteThemes(site);

        // ensure site ID is set before inserting
        for (ThemeModel theme : themes) {
            theme.setLocalSiteId(site.getId());
        }

        WellSql.insert(themes).asSingleTransaction(true).execute();
    }

    public static void insertOrReplaceActiveThemeForSite(@NonNull SiteModel site, @NonNull ThemeModel theme) {
        // find any existing active theme for the site and unset active flag
        List<ThemeModel> existing = getActiveThemeForSite(site);
        if (!existing.isEmpty()) {
            for (ThemeModel activeTheme : existing) {
                activeTheme.setActive(false);
                WellSql.update(ThemeModel.class)
                        .whereId(activeTheme.getId())
                        .put(activeTheme).execute();
            }
        }

        // make sure active flag is set
        theme.setActive(true);
        insertOrUpdateSiteTheme(site, theme);
    }

    @NonNull
    public static List<ThemeModel> getActiveThemeForSite(@NonNull SiteModel site) {
        return WellSql.select(ThemeModel.class)
                .where().beginGroup()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .equals(ThemeModelTable.ACTIVE, true)
                .endGroup().endWhere().getAsModel();
    }

    @NonNull
    public static List<ThemeModel> getWpComThemes() {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.IS_WP_COM_THEME, true)
                .endWhere().getAsModel();
    }

    @NonNull
    public static List<ThemeModel> getWpComThemes(@NonNull List<String> themeIds) {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.IS_WP_COM_THEME, true)
                .isIn(ThemeModelTable.THEME_ID, themeIds)
                .endWhere().getAsModel();
    }

    @NonNull
    public static List<ThemeModel> getWpComMobileFriendlyThemes(@NonNull String categorySlug) {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.MOBILE_FRIENDLY_CATEGORY_SLUG, categorySlug)
                .equals(ThemeModelTable.IS_WP_COM_THEME, true)
                .endWhere().getAsModel();
    }

    @NonNull
    public static List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    @Nullable
    public static ThemeModel getWpComThemeByThemeId(@NonNull String themeId) {
        if (TextUtils.isEmpty(themeId)) {
            return null;
        }

        List<ThemeModel> matches = WellSql.select(ThemeModel.class)
                .where().beginGroup()
                .equals(ThemeModelTable.THEME_ID, themeId)
                .equals(ThemeModelTable.IS_WP_COM_THEME, true)
                .endGroup().endWhere().getAsModel();

        if (matches == null || matches.isEmpty()) {
            return null;
        }

        return matches.get(0);
    }

    @Nullable
    public static ThemeModel getSiteThemeByThemeId(@NonNull SiteModel siteModel, @NonNull String themeId) {
        if (TextUtils.isEmpty(themeId)) {
            return null;
        }
        List<ThemeModel> matches = WellSql.select(ThemeModel.class)
                .where().beginGroup()
                .equals(ThemeModelTable.LOCAL_SITE_ID, siteModel.getId())
                .equals(ThemeModelTable.THEME_ID, themeId)
                .equals(ThemeModelTable.IS_WP_COM_THEME, false)
                .endGroup().endWhere().getAsModel();

        if (matches == null || matches.isEmpty()) {
            return null;
        }

        return matches.get(0);
    }

    public static void removeWpComThemes() {
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.IS_WP_COM_THEME, true)
                .endWhere().execute();
    }

    public static void removeSiteTheme(@NonNull SiteModel site, @NonNull ThemeModel theme) {
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .equals(ThemeModelTable.THEME_ID, theme.getThemeId())
                .equals(ThemeModelTable.IS_WP_COM_THEME, false)
                .endWhere().execute();
    }

    public static void removeSiteThemes(@NonNull SiteModel site) {
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .equals(ThemeModelTable.IS_WP_COM_THEME, false)
                .endWhere().execute();
    }
}
