package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class JetpackModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore
){
    /**
     * Activates the stats module for the passed site if not already activated.
     */
    suspend fun activateStatsModule(site: SiteModel): Result<Unit> {
        if (isModuleActivated(site.siteId, STATS_MODULE_NAME)) {
            return Result.success(Unit)
        }

        val result = jetpackStore.activateStatsModule(
            ActivateStatsModulePayload(site)
        )

        return if (!result.isError && isModuleActivated(site.siteId, STATS_MODULE_NAME)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Stats module not activated"))
        }
    }

    /**
     * Checks if the passed module is activated for the passed site. Note we always get the site from
     * the store because it may have been changed when the module was activated above.
     */
    @Suppress("SameParameterValue")
    private fun isModuleActivated(siteId: Long, moduleName: String): Boolean {
        val site = siteStore.getSiteBySiteId(siteId) ?: return false
        return site.activeModules.contains(moduleName)
    }


    companion object {
        private const val STATS_MODULE_NAME = "stats"
    }
}
