package org.wordpress.android.ui.jetpackrestconnection

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class JetpackStatsModuleHelper @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore
) {
    suspend fun activateStatsModule(site: SiteModel): Result<Unit> {
        if (isStatsModuleActivated(site)) {
            return Result.success(Unit)
        }

        jetpackStore.activateStatsModule(ActivateStatsModulePayload(site))

        // ignore the activation response as it's unreliable, instead just check if it's activated now
        return verifyStatsModuleActivation(site)
    }

    /**
     * Fetch Jetpack sites filtering for this site and check if the module is now activated. Note that fetching
     * the single site fails because of changes to it during the connection flow.
     */
    private suspend fun verifyStatsModuleActivation(
        site: SiteModel,
    ): Result<Unit> {
        val payload = SiteStore.FetchSitesPayload(
            filters = listOf(SiteStore.SiteFilter.JETPACK)
        )
        return siteStore.fetchSites(payload).updatedSites.firstOrNull {
            it.siteId == site.siteId
        }?.let { updatedSite ->
            if (isStatsModuleActivated(updatedSite)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Stats module not activated"))
            }
        } ?: Result.failure(Exception("Site not found"))
    }

    private fun isStatsModuleActivated(site: SiteModel) =
        site.isActiveModuleEnabled(STATS_MODULE_NAME)

    companion object {
        private const val STATS_MODULE_NAME = "stats"
    }
}
