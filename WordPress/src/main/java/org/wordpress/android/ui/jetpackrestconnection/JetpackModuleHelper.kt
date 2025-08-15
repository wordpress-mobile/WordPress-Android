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

        // Ignore the above response, just fetch Jetpack sites filtering for this site and check if the module is now
        // activated. Note that fetching the single site fails because of changes to it during the connection flow.
        val payload = SiteStore.FetchSitesPayload(
            filters = listOf(SiteStore.SiteFilter.JETPACK)
        )
        return siteStore.fetchSites(payload).updatedSites.firstOrNull {
            it.siteId == site.siteId
        }?.let { updatedSite ->
            // the updated site will have lost the REST passwords, so we need to copy them from the original site
            copyRestPasswords(site, updatedSite)
            if (isModuleActivated(updatedSite, module)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("${module.moduleName} module not activated"))
            }
        } ?: Result.failure(Exception("Site not found"))
    }

    /**
     * Copy the REST passwords from one site to another and save the updated site
     */
    private fun copyRestPasswords(fromSite: SiteModel, toSite: SiteModel) {
        val updatedSite = toSite.also {
            it.apiRestUsernameEncrypted = fromSite.apiRestUsernameEncrypted
            it.apiRestPasswordEncrypted = fromSite.apiRestPasswordEncrypted
            it.apiRestUsernamePlain = fromSite.apiRestUsernamePlain
            it.apiRestPasswordPlain = fromSite.apiRestPasswordPlain
            it.apiRestUsernameIV = fromSite.apiRestUsernameIV
            it.apiRestPasswordIV = fromSite.apiRestPasswordIV
        }
        siteStore.updateSite(updatedSite)
    }

    private fun isModuleActivated(site: SiteModel, module: Module): Boolean {
        return site.isActiveModuleEnabled(module.moduleName)
    }

    private enum class Module(val moduleName: String) {
        STATS("stats"),
    }
}
