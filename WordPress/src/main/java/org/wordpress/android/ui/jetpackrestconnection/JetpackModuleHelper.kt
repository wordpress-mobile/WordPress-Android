package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class JetpackModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteRestClient: SiteRestClient,
    private val siteStore: SiteStore,
){
    /**
     * Activates the stats module for the passed site if not already activated.
     */
    suspend fun activateStatsModule(site: SiteModel): Result<Unit> {
        if (isModuleActivated(site, STATS_MODULE_NAME)) {
            return Result.success(Unit)
        }

        // Ignore the result of activateStatsModule as it's unreliable
        jetpackStore.activateStatsModule(
            ActivateStatsModulePayload(site)
        )

        return if (isModuleActivated(site, STATS_MODULE_NAME)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Stats module not activated"))
        }
    }

    /**
     * Returns true if the passed module is activated for the passed site. If not, we
     * fetch the site and check again, and if it's now activated then we update the
     * site in the store.
     */
    private suspend fun isModuleActivated(site: SiteModel, moduleName: String): Boolean {
        return if (site.isActiveModuleEnabled(moduleName)) {
            true
        } else {
            val updatedSite = siteRestClient.fetchSite(site)
            if (updatedSite.isActiveModuleEnabled(moduleName)) {
                siteStore.fetchSite(updatedSite)
                true
            } else {
                false
            }
        }
    }

    companion object {
        private const val STATS_MODULE_NAME = "stats"
    }
}
