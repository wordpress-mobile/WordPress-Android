package org.wordpress.android.fluxc.persistence;

import android.database.Cursor;
import android.support.annotation.NonNull;

import com.wellsql.generated.ThemeModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;

import java.util.List;

public class ThemeSqlUtils {
    public static void insertOrUpdateTheme(@NonNull ThemeModel theme) {
        List<ThemeModel> existing = WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.THEME_ID, theme.getThemeId())
                .endWhere().getAsModel();

        if (existing.isEmpty()) {
            WellSql.insert(theme).asSingleTransaction(true).execute();
        } else {
            WellSql.update(ThemeModel.class).whereId(existing.get(0).getId())
                    .put(theme, new UpdateAllExceptId<>(ThemeModel.class)).execute();
        }
    }

    public static void insertOrReplaceWpThemes(@NonNull List<ThemeModel> themes) {
        // remove existing WP.com themes
        removeThemesWithNoSite();

        for (ThemeModel theme : themes) {
            theme.setLocalSiteId(0);
        }

        WellSql.insert(themes).asSingleTransaction(true).execute();
    }

    public static int insertOrReplaceInstalledThemes(@NonNull SiteModel site, @NonNull List<ThemeModel> themes) {
        // Remove existing installed themes
        removeThemes(site);

        for (ThemeModel theme : themes) {
            theme.setLocalSiteId(site.getId());
        }

        WellSql.insert(themes).asSingleTransaction(true).execute();

        return themes.size();
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

        // make sure active flag is set then add to db
        theme.setActive(true);
        insertOrUpdateTheme(theme);
    }

    public static List<ThemeModel> getActiveThemeForSite(@NonNull SiteModel site) {
        return WellSql.select(ThemeModel.class)
                .where().beginGroup()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .equals(ThemeModelTable.ACTIVE, true)
                .endGroup().endWhere().getAsModel();
    }

    public static Cursor getThemesWithNoSiteAsCursor() {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, 0)
                .endWhere().getAsCursor();
    }

    public static List<ThemeModel> getThemesWithNoSite() {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, 0)
                .endWhere().getAsModel();
    }

    /**
     * Retrieves themes associated with a given site. Installed themes (for Jetpack sites) are the only themes
     * targeted for now.
     */
    public static List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    /**
     * @return the first theme that matches a given theme ID; null if none found
     */
    public static ThemeModel getThemeWithId(@NonNull String themeId) {
        List<ThemeModel> matches = WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.THEME_ID, themeId)
                .endWhere().getAsModel();

        if (matches == null || matches.isEmpty()) {
            return null;
        }

        return matches.get(0);
    }

    public static void removeThemes(@NonNull SiteModel site) {
        removeThemes(site.getId());
    }

    public static void removeThemesWithNoSite() {
        // Remove themes whose localSiteId is 0
        removeThemes(0);
    }

    private static void removeThemes(int localSiteId) {
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, localSiteId)
                .endWhere().execute();
    }
}
