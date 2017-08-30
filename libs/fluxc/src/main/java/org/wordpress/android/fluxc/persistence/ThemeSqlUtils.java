package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
        removeThemes(null);
        WellSql.insert(themes).asSingleTransaction(true).execute();
    }

    public static int insertOrReplaceInstalledThemes(@NonNull SiteModel site, @NonNull List<ThemeModel> themes) {
        // Remove existing installed themes
        removeThemes(site);

        for (ThemeModel theme : themes) {
            theme.setLocalSiteId(site.getSiteId());
        }

        WellSql.insert(themes).execute();

        return themes.size();
    }

    public static void removeThemes(@Nullable SiteModel site) {
        long siteId = site == null ? -1 : site.getSiteId();
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, siteId)
                .endWhere().execute();
    }

    /**
     * Retrieves themes stored with no associated site.
     */
    public static List<ThemeModel> getWpThemes() {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, -1)
                .endWhere().getAsModel();
    }

    /**
     * Retrieves themes stored with a non-zero site ID. Installed themes (for Jetpack sites) are the only themes
     * with a non-zero site ID, for now.
     */
    public static List<ThemeModel> getThemesForSite(@NonNull SiteModel site) {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getSiteId())
                .endWhere().getAsModel();
    }
}
