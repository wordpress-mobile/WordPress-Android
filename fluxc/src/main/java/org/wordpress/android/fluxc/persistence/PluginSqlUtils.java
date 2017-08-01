package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;

import com.wellsql.generated.PluginModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.PluginModel;
import org.wordpress.android.fluxc.model.SiteModel;

import java.util.List;

public class PluginSqlUtils {
    public static List<PluginModel> getPlugins(@NonNull SiteModel site) {
        return WellSql.select(PluginModel.class)
                .where()
                .equals(PluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
    }

    public static void insertOrReplacePlugins(@NonNull SiteModel site, @NonNull List<PluginModel> plugins) {
        // Remove previous post formats for this site
        WellSql.delete(PluginModel.class)
                .where()
                .equals(PluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
        // Insert new post formats for this site
        for (PluginModel pluginModel : plugins) {
            pluginModel.setLocalSiteId(site.getId());
        }
        WellSql.insert(plugins).execute();
    }
}
