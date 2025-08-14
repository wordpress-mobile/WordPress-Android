package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class JetpackModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore
) {
    suspend fun activateStatsModule(site: SiteModel): Result<Unit> =
        activateModule(site, Module.STATS)

    private suspend fun activateModule(site: SiteModel, module: Module): Result<Unit> {
        if (isModuleActivated(site, module)) {
            return Result.success(Unit)
        }

        when (module) {
            Module.STATS -> {
                jetpackStore.activateStatsModule(ActivateStatsModulePayload(site))
            }
        }

        // ignore the response, just reload sites and check if the module is activated now
        siteStore.fetchSites(SiteStore.FetchSitesPayload()) // TODO filter?
        return if (isModuleActivated(site, module)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("${module.moduleName} module not activated"))
        }
    }

    /**
     * Checks if the passed module is activated for the passed site. Note we always get the site from
     * the store because it may have been changed when the module was activated above.
     */
    private fun isModuleActivated(site: SiteModel, module: Module): Boolean {
        val freshSite = siteStore.getSiteByLocalId(site.id) ?: return false
        return freshSite.isActiveModuleEnabled(module.moduleName)
    }

    private enum class Module(val moduleName: String) {
        STATS("stats"),
    }
}
