package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;

import com.wellsql.generated.DotOrgPluginModelTable;
import com.wellsql.generated.SitePluginModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.DotOrgPluginModel;
import org.wordpress.android.fluxc.model.SitePluginModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

import static com.yarolegovich.wellsql.SelectQuery.ORDER_ASCENDING;

public class PluginSqlUtils {
    public static List<SitePluginModel> getSitePlugins(@NonNull SiteModel site) {
        return WellSql.select(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere()
                .orderBy(SitePluginModelTable.DISPLAY_NAME, ORDER_ASCENDING)
                .getAsModel();
    }

    public static void insertOrReplaceSitePlugins(@NonNull SiteModel site, @NonNull List<SitePluginModel> plugins) {
        // Remove previous plugins for this site
        removeSitePlugins(site);
        // Insert new plugins for this site
        for (SitePluginModel sitePluginModel : plugins) {
            sitePluginModel.setLocalSiteId(site.getId());
        }
        WellSql.insert(plugins).asSingleTransaction(true).execute();
    }

    private static void removeSitePlugins(@NonNull SiteModel site) {
        WellSql.delete(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
    }

    public static int insertOrUpdateSitePlugin(SitePluginModel plugin) {
        if (plugin == null) {
            return 0;
        }

        List<SitePluginModel> pluginResult = WellSql.select(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.ID, plugin.getId())
                .endWhere().getAsModel();
        if (pluginResult.isEmpty()) {
            WellSql.insert(plugin).execute();
            return 1;
        } else {
            int oldId = plugin.getId();
            return WellSql.update(SitePluginModel.class).whereId(oldId)
                    .put(plugin, new UpdateAllExceptId<>(SitePluginModel.class)).execute();
        }
    }

    public static int deleteSitePlugin(SiteModel site, SitePluginModel plugin) {
        if (plugin == null) {
            return 0;
        }
        // The local id of the plugin might not be set if it's coming from a network request,
        // using site id and name is a safer approach here
        return WellSql.delete(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.NAME, plugin.getName())
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
    }

    public static int insertOrUpdatePluginInfo(DotOrgPluginModel pluginInfo) {
        if (pluginInfo == null) {
            return 0;
        }

        // Slug is the primary key in remote, so we should use that to identify PluginInfoModels
        DotOrgPluginModel oldPluginInfo = getPluginInfoBySlug(pluginInfo.getSlug());

        if (oldPluginInfo == null) {
            WellSql.insert(pluginInfo).execute();
            return 1;
        } else {
            int oldId = oldPluginInfo.getId();
            return WellSql.update(DotOrgPluginModel.class).whereId(oldId)
                    .put(pluginInfo, new UpdateAllExceptId<>(DotOrgPluginModel.class)).execute();
        }
    }

    public static SitePluginModel getSitePluginByName(SiteModel site, String name) {
        List<SitePluginModel> result = WellSql.select(SitePluginModel.class)
                .where().equals(SitePluginModelTable.NAME, name)
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }

    public static DotOrgPluginModel getPluginInfoBySlug(String slug) {
        List<DotOrgPluginModel> result = WellSql.select(DotOrgPluginModel.class)
                .where().equals(DotOrgPluginModelTable.SLUG, slug)
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }
}
