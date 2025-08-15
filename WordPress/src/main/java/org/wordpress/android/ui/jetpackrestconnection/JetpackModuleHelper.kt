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

        // ignore the response, just fetch Jetpack sites filtering for this site and check if the module is activated
        val payload = SiteStore.FetchSitesPayload(
            filters = listOf(SiteStore.SiteFilter.JETPACK)
        )
        return siteStore.fetchSites(payload).updatedSites.firstOrNull {
            it.siteId == site.siteId
        }?.let { updatedSite ->
            if (isModuleActivated(updatedSite, module)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("${module.moduleName} module not activated"))
            }
        } ?: Result.failure(Exception("Site not found"))
    }

    /**
     * Checks if the passed module is activated for the passed site
     */
    private fun isModuleActivated(site: SiteModel, module: Module): Boolean {
        return site.isActiveModuleEnabled(module.moduleName)
    }

    private enum class Module(val moduleName: String) {
        STATS("stats"),
    }
}
