package org.wordpress.android.fluxc.persistence

import android.text.TextUtils
import com.wellsql.generated.PluginDirectoryModelTable
import com.wellsql.generated.SitePluginModelTable
import com.wellsql.generated.WPOrgPluginModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel
import kotlin.math.max

object PluginSqlUtils {
    @JvmStatic
    fun getSitePlugins(site: SiteModel): List<SitePluginModel> =
        WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .orderBy(SitePluginModelTable.DISPLAY_NAME, SelectQuery.ORDER_ASCENDING)
            .asModel

    @JvmStatic
    fun insertOrReplaceSitePlugins(site: SiteModel, plugins: List<SitePluginModel>) {
        // Remove previous plugins for this site
        removeSitePlugins(site)
        // Insert new plugins for this site
        for (sitePluginModel in plugins) {
            sitePluginModel.localSiteId = site.id
        }
        WellSql.insert(plugins).asSingleTransaction(true).execute()
    }

    private fun removeSitePlugins(site: SiteModel) =
        WellSql.delete(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere().execute()

    @JvmStatic
    fun insertOrUpdateSitePlugin(site: SiteModel, plugin: SitePluginModel?): Int {
        if (plugin == null) {
            return 0
        }
        val oldPlugin = getSitePluginBySlug(site, plugin.slug)
        // Make sure the site id is set (if the plugin is retrieved from network)
        plugin.localSiteId = site.id
        return if (oldPlugin == null) {
            WellSql.insert(plugin).execute()
            1
        } else {
            val oldId = oldPlugin.id
            WellSql.update(SitePluginModel::class.java)
                .whereId(oldId)
                .put(plugin, UpdateAllExceptId(SitePluginModel::class.java))
                .execute()
        }
    }

    @JvmStatic
    fun deleteSitePlugins(site: SiteModel): Int =
        WellSql.delete(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere().execute()

    @JvmStatic
    fun deleteSitePlugin(site: SiteModel, slug: String?): Int {
        return if (TextUtils.isEmpty(slug)) {
            0
        } else {
            // The local id of the plugin might not be set if it's coming from a network request,
            // using site id and slug is a safer approach here
            WellSql.delete(SitePluginModel::class.java)
                .where()
                .equals(SitePluginModelTable.SLUG, slug)
                .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
                .endWhere().execute()
        }
    }

    @JvmStatic
    fun getSitePluginBySlug(site: SiteModel, slug: String?): SitePluginModel? {
        val result = WellSql.select(SitePluginModel::class.java).where()
            .equals(SitePluginModelTable.SLUG, slug)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
        return if (result.isEmpty()) null else result.first()
    }

    fun getSitePluginByName(site: SiteModel, pluginName: String?): SitePluginModel? {
        val result = WellSql.select(SitePluginModel::class.java)
            .where()
            .equals(SitePluginModelTable.NAME, pluginName)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
        return if (result.isEmpty()) null else result.first()
    }

    fun getSitePluginByNames(site: SiteModel, pluginNames: List<String?>?): List<SitePluginModel> =
        WellSql.select(SitePluginModel::class.java)
            .where()
            .isIn(SitePluginModelTable.NAME, pluginNames)
            .equals(SitePluginModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel

    @JvmStatic
    fun getWPOrgPluginBySlug(slug: String?): WPOrgPluginModel? {
        val result = WellSql.select(WPOrgPluginModel::class.java)
            .where()
            .equals(WPOrgPluginModelTable.SLUG, slug)
            .endWhere()
            .asModel
        return if (result.isEmpty()) null else result.first()
    }

    @JvmStatic
    fun getWPOrgPluginsForDirectory(directoryType: PluginDirectoryType?): List<WPOrgPluginModel> {
        val directoryModels = getPluginDirectoriesForType(directoryType)
        if (directoryModels.isEmpty()) {
            // No directories found, return an empty list
            return ArrayList()
        }
        val slugList = ArrayList<String?>(directoryModels.size)
        val orderMap = HashMap<String, Int>()
        directoryModels.forEachIndexed { index, pluginDirectoryModel ->
            val slug = pluginDirectoryModel.slug
            slugList.add(slug)
            orderMap[slug] = index
        }

        val batches = slugList.chunked(WellSqlConfig.SQLITE_MAX_VARIABLE_NUMBER)
        val wpOrgPlugins = mutableListOf<WPOrgPluginModel>()
        batches.forEach {
            val batchQueryResult = WellSql.select(WPOrgPluginModel::class.java)
                .where()
                .isIn(WPOrgPluginModelTable.SLUG, it)
                .endWhere()
                .asModel
            wpOrgPlugins.addAll(batchQueryResult)
        }
        // We need to manually order the list according to the directory models since SQLite will
        // return mixed results
        wpOrgPlugins.sortWith { plugin1, plugin2 ->
            val order1 = orderMap[plugin1.slug] ?: 0
            val order2 = orderMap[plugin2.slug] ?: 0
            order1.compareTo(order2)
        }
        return wpOrgPlugins
    }

    @JvmStatic
    fun insertOrUpdateWPOrgPlugin(wpOrgPluginModel: WPOrgPluginModel?): Int {
        if (wpOrgPluginModel == null) {
            return 0
        }

        // Slug is the primary key in remote, so we should use that to identify WPOrgPluginModels
        val oldPlugin = getWPOrgPluginBySlug(wpOrgPluginModel.slug)

        return if (oldPlugin == null) {
            WellSql.insert(wpOrgPluginModel).execute()
            1
        } else {
            val oldId = oldPlugin.id
            WellSql.update(WPOrgPluginModel::class.java)
                .whereId(oldId)
                .put(wpOrgPluginModel, UpdateAllExceptId(WPOrgPluginModel::class.java))
                .execute()
        }
    }

    @JvmStatic
    fun insertOrUpdateWPOrgPluginList(wpOrgPluginModels: List<WPOrgPluginModel?>?): Int {
        if (wpOrgPluginModels == null) {
            return 0
        }

        var result = 0
        wpOrgPluginModels.forEach { result += insertOrUpdateWPOrgPlugin(it) }
        return result
    }

    // Plugin Directory

    @JvmStatic
    fun deletePluginDirectoryForType(directoryType: PluginDirectoryType) =
        WellSql.delete(PluginDirectoryModel::class.java)
            .where()
            .equals(PluginDirectoryModelTable.DIRECTORY_TYPE, directoryType.toString())
            .endWhere()
            .execute()

    @JvmStatic
    fun insertPluginDirectoryList(pluginDirectories: List<PluginDirectoryModel>?) {
        if (pluginDirectories == null) {
            return
        }
        WellSql.insert(pluginDirectories).asSingleTransaction(true).execute()
    }

    @JvmStatic
    fun getLastRequestedPageForDirectoryType(directoryType: PluginDirectoryType?): Int {
        val list = getPluginDirectoriesForType(directoryType)
        var page = 0
        list.forEach { page = max(it.page, page) }
        return page
    }

    @JvmStatic
    private fun getPluginDirectoriesForType(
        directoryType: PluginDirectoryType?
    ): List<PluginDirectoryModel> =
        WellSql.select(PluginDirectoryModel::class.java)
            .where()
            .equals(PluginDirectoryModelTable.DIRECTORY_TYPE, directoryType)
            .endWhere()
            .asModel
}
