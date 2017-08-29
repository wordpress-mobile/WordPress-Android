package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.wellsql.generated.ThemeModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;

import java.util.List;

public class ThemeSqlUtils {
    public static void insertTheme(@NonNull ThemeModel theme) {
        WellSql.insert(theme).execute();
    }

    public static void insertOrReplaceWpThemes(@NonNull List<ThemeModel> themes) {
        // remove existing WP.com themes
        for (ThemeModel theme : themes) {
            removeThemeWithId(theme);
        }
        WellSql.insert(themes).execute();
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

    public static void removeThemes(@NonNull SiteModel site) {
        WellSql.delete(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, site.getSiteId())
                .endWhere().execute();
    }

    public static void removeThemeWithId(@NonNull ThemeModel theme) {
        String themeId = theme.getThemeId();
        if (!TextUtils.isEmpty(themeId)) {
            WellSql.delete(ThemeModel.class)
                    .where()
                    .equals(ThemeModelTable.THEME_ID, themeId)
                    .endWhere().execute();
        }
    }

    /**
     * Retrieves themes stored with no associated site.
     */
    public static List<ThemeModel> getWpThemes() {
        return WellSql.select(ThemeModel.class)
                .where()
                .equals(ThemeModelTable.LOCAL_SITE_ID, 0)
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
