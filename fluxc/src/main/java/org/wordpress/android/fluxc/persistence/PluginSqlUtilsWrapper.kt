package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryModel
import org.wordpress.android.fluxc.model.plugin.PluginDirectoryType
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel
import javax.inject.Inject

class PluginSqlUtilsWrapper
@Inject constructor() {
    fun getSitePlugins(site: SiteModel): List<SitePluginModel> {
        return PluginSqlUtils.getSitePlugins(site)
    }

    fun insertOrReplaceSitePlugins(site: SiteModel, plugins: List<SitePluginModel>) {
        PluginSqlUtils.insertOrReplaceSitePlugins(site, plugins)
    }

    fun insertOrUpdateSitePlugin(site: SiteModel, plugin: SitePluginModel?): Int {
        return PluginSqlUtils.insertOrUpdateSitePlugin(site, plugin)
    }

    fun deleteSitePlugins(site: SiteModel): Int {
        return PluginSqlUtils.deleteSitePlugins(site)
    }

    fun deleteSitePlugin(site: SiteModel, slug: String?): Int {
        return PluginSqlUtils.deleteSitePlugin(site, slug)
    }

    fun getSitePluginBySlug(site: SiteModel, slug: String?): SitePluginModel? {
        return PluginSqlUtils.getSitePluginBySlug(site, slug)
    }

    fun getWPOrgPluginBySlug(slug: String?): WPOrgPluginModel? {
        return PluginSqlUtils.getWPOrgPluginBySlug(slug)
    }

    fun getWPOrgPluginsForDirectory(directoryType: PluginDirectoryType?): List<WPOrgPluginModel?> {
        return PluginSqlUtils.getWPOrgPluginsForDirectory(directoryType)
    }

    fun insertOrUpdateWPOrgPlugin(wpOrgPluginModel: WPOrgPluginModel?): Int {
        return PluginSqlUtils.insertOrUpdateWPOrgPlugin(wpOrgPluginModel)
    }

    fun insertOrUpdateWPOrgPluginList(wpOrgPluginModels: List<WPOrgPluginModel?>?): Int {
        return PluginSqlUtils.insertOrUpdateWPOrgPluginList(wpOrgPluginModels)
    }

    fun deletePluginDirectoryForType(directoryType: PluginDirectoryType) {
        PluginSqlUtils.deletePluginDirectoryForType(directoryType)
    }

    fun insertPluginDirectoryList(pluginDirectories: List<PluginDirectoryModel>?) {
        PluginSqlUtils.insertPluginDirectoryList(pluginDirectories)
    }

    fun getLastRequestedPageForDirectoryType(directoryType: PluginDirectoryType?): Int {
        return PluginSqlUtils.getLastRequestedPageForDirectoryType(directoryType)
    }
}
