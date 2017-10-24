package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;

import com.wellsql.generated.PluginInfoModelTable;
import com.wellsql.generated.PluginModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PluginInfoModel;
import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

public class PluginSqlUtils {
    public static List<PluginModel> getSitePlugins(@NonNull SiteModel site) {
        return WellSql.select(PluginModel.class)
                .where()
                .equals(PluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    public static void insertOrReplaceSitePlugins(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
        // Remove previous plugins for this site
        removeSitePlugins(site);
        // Insert new plugins for this site
        for (PluginModel pluginModel : plugins) {
            pluginModel.setLocalSiteId(site.getId());
        }
        WellSql.insert(plugins).asSingleTransaction(true).execute();
    }

    private static void removeSitePlugins(@NonNull SiteModel site) {
        WellSql.delete(PluginModel.class)
                .where()
                .equals(PluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
    }

    public static int insertOrUpdateSitePlugin(SiteModel site, PluginModel plugin) {
        if (plugin == null) {
            return 0;
        }

        PluginModel oldPlugin = getSitePluginByName(site, plugin.getName());
        if (oldPlugin == null) {
            WellSql.insert(plugin).execute();
            return 1;
        } else {
            int oldId = oldPlugin.getId();
            return WellSql.update(PluginModel.class).whereId(oldId)
                    .put(plugin, new UpdateAllExceptId<>(PluginModel.class)).execute();
        }
    }

    public static int insertOrUpdatePluginInfo(PluginInfoModel pluginInfo) {
        if (pluginInfo == null) {
            return 0;
        }

        // Slug is the primary key in remote, so we should use that to identify PluginInfoModels
        PluginInfoModel oldPluginInfo = getPluginInfoBySlug(pluginInfo.getSlug());

        if (oldPluginInfo == null) {
            WellSql.insert(pluginInfo).execute();
            return 1;
        } else {
            int oldId = oldPluginInfo.getId();
            return WellSql.update(PluginInfoModel.class).whereId(oldId)
                    .put(pluginInfo, new UpdateAllExceptId<>(PluginInfoModel.class)).execute();
        }
    }

    public static PluginModel getSitePluginByName(SiteModel site, String name) {
        List<PluginModel> result = WellSql.select(PluginModel.class)
                .where().equals(PluginModelTable.NAME, name)
                .equals(PluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }

    public static PluginInfoModel getPluginInfoBySlug(String slug) {
        List<PluginInfoModel> result = WellSql.select(PluginInfoModel.class)
                .where().equals(PluginInfoModelTable.SLUG, slug)
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }
}
