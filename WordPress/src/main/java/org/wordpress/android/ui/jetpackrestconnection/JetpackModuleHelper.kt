package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import javax.inject.Inject

class JetpackModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
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

    @Suppress("SameParameterValue")
    private fun isModuleActivated(site: SiteModel, moduleName: String) =
        site.isActiveModuleEnabled(moduleName)

    companion object {
        private const val STATS_MODULE_NAME = "stats"
    }
}
