package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import javax.inject.Inject

class LocalSiteProviderHelper @Inject constructor(
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
) : LocalDataProviderHelper {
    override fun getData(localEntityId: Int?): SitesData =
        if (accountStore.hasAccessToken()) {
            SitesData(sites = siteStore.sites, true)
        }
        else {
            // self-hosted only
            val sites = siteStore.sites.filter { !it.isUsingWpComRestApi }
            SitesData(sites = sites, sites.isNotEmpty())
        }
}
