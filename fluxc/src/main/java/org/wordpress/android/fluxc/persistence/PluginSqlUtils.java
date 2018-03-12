package org.wordpress.android.fluxc.persistence;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.wellsql.generated.PluginDirectoryModelTable;
import com.wellsql.generated.SitePluginModelTable;
import com.wellsql.generated.WPOrgPluginModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel;
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType;
import org.wordpress.android.fluxc.model.plugin.SitePluginModel;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import static com.yarolegovich.wellsql.SelectQuery.ORDER_ASCENDING;

public class PluginSqlUtils {
    public static @NonNull List<SitePluginModel> getSitePlugins(@NonNull SiteModel site) {
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

    public static int insertOrUpdateSitePlugin(SiteModel site, SitePluginModel plugin) {
        if (plugin == null) {
            return 0;
        }

        SitePluginModel oldPlugin = getSitePluginBySlug(site, plugin.getSlug());
        plugin.setLocalSiteId(site.getId()); // Make sure the site id is set (if the plugin is retrieved from network)
        if (oldPlugin == null) {
            WellSql.insert(plugin).execute();
            return 1;
        } else {
            int oldId = oldPlugin.getId();
            return WellSql.update(SitePluginModel.class).whereId(oldId)
                    .put(plugin, new UpdateAllExceptId<>(SitePluginModel.class)).execute();
        }
    }

    public static int deleteSitePlugins(@NonNull SiteModel site) {
        return WellSql.delete(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
    }

    public static int deleteSitePlugin(SiteModel site, String slug) {
        if (TextUtils.isEmpty(slug)) {
            return 0;
        }
        // The local id of the plugin might not be set if it's coming from a network request,
        // using site id and slug is a safer approach here
        return WellSql.delete(SitePluginModel.class)
                .where()
                .equals(SitePluginModelTable.SLUG, slug)
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().execute();
    }

    public static SitePluginModel getSitePluginBySlug(@NonNull SiteModel site, String slug) {
        List<SitePluginModel> result = WellSql.select(SitePluginModel.class)
                .where().equals(SitePluginModelTable.SLUG, slug)
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.getId())
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }

    public static @Nullable WPOrgPluginModel getWPOrgPluginBySlug(String slug) {
        List<WPOrgPluginModel> result = WellSql.select(WPOrgPluginModel.class)
                .where().equals(WPOrgPluginModelTable.SLUG, slug)
                .endWhere().getAsModel();
        return result.isEmpty() ? null : result.get(0);
    }

    public static @NonNull List<WPOrgPluginModel> getWPOrgPluginsForDirectory(PluginDirectoryType directoryType) {
        List<PluginDirectoryModel> directoryModels = getPluginDirectoriesForType(directoryType);
        if (directoryModels.size() == 0) {
            // No directories found, return an empty list
            return new ArrayList<>();
        }
        List<String> slugList = new ArrayList<>(directoryModels.size());
        final HashMap<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < directoryModels.size(); i++) {
            String slug = directoryModels.get(i).getSlug();
            slugList.add(slug);
            orderMap.put(slug, i);
        }
        List<WPOrgPluginModel> wpOrgPlugins = WellSql.select(WPOrgPluginModel.class)
                .where().isIn(WPOrgPluginModelTable.SLUG, slugList)
                .endWhere().getAsModel();
        // We need to manually order the list according to the directory models since SQLite will return mixed results
        Collections.sort(wpOrgPlugins, new Comparator<WPOrgPluginModel>() {
            @Override
            public int compare(WPOrgPluginModel plugin1, WPOrgPluginModel plugin2) {
                return orderMap.get(plugin1.getSlug()).compareTo(orderMap.get(plugin2.getSlug()));
            }
        });
        return wpOrgPlugins;
    }

    public static int insertOrUpdateWPOrgPlugin(WPOrgPluginModel wpOrgPluginModel) {
        if (wpOrgPluginModel == null) {
            return 0;
        }

        // Slug is the primary key in remote, so we should use that to identify WPOrgPluginModels
        WPOrgPluginModel oldPlugin = getWPOrgPluginBySlug(wpOrgPluginModel.getSlug());

        if (oldPlugin == null) {
            WellSql.insert(wpOrgPluginModel).execute();
            return 1;
        } else {
            int oldId = oldPlugin.getId();
            return WellSql.update(WPOrgPluginModel.class).whereId(oldId)
                    .put(wpOrgPluginModel, new UpdateAllExceptId<>(WPOrgPluginModel.class)).execute();
        }
    }

    public static int insertOrUpdateWPOrgPluginList(List<WPOrgPluginModel> wpOrgPluginModels) {
        if (wpOrgPluginModels == null) {
            return 0;
        }

        int result = 0;
        for (WPOrgPluginModel pluginModel : wpOrgPluginModels) {
            result += insertOrUpdateWPOrgPlugin(pluginModel);
        }
        return result;
    }

    // Plugin Directory

    public static void deletePluginDirectoryForType(PluginDirectoryType directoryType) {
        WellSql.delete(PluginDirectoryModel.class)
                .where()
                .equals(PluginDirectoryModelTable.DIRECTORY_TYPE, directoryType.toString())
                .endWhere().execute();
    }

    public static void insertPluginDirectoryList(@Nullable List<PluginDirectoryModel> pluginDirectories) {
        if (pluginDirectories == null) {
            return;
        }

        WellSql.insert(pluginDirectories).asSingleTransaction(true).execute();
    }

    public static int getLastRequestedPageForDirectoryType(PluginDirectoryType directoryType) {
        List<PluginDirectoryModel> list = getPluginDirectoriesForType(directoryType);
        int page = 0;
        for (PluginDirectoryModel pluginDirectoryModel : list) {
            page = Math.max(pluginDirectoryModel.getPage(), page);
        }
        return page;
    }

    private static @NonNull List<PluginDirectoryModel> getPluginDirectoriesForType(PluginDirectoryType directoryType) {
        return WellSql.select(PluginDirectoryModel.class)
                .where()
                .equals(PluginDirectoryModelTable.DIRECTORY_TYPE, directoryType)
                .endWhere()
                .getAsModel();
    }
}
