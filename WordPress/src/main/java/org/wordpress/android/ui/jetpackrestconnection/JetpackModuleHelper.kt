package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import javax.inject.Inject

class JetpackModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
){
    suspend fun activateStatsModule(site: SiteModel): Result<Unit> = runCatching {
        // First check if stats module is already activated (installing Jetpack may do that for us)
        if (isStatsActivated(site)) {
            Result.success(Unit)
        }

        // Ignore the result of activateStatsModule as it's unreliable
        jetpackStore.activateStatsModule(
            ActivateStatsModulePayload(site)
        )

        if (isStatsActivated(site)) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Stats module not activated"))
        }
    }

    private fun isStatsActivated(site: SiteModel) = site.isActiveModuleEnabled("stats")
}
