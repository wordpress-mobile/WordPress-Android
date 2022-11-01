package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.SitesData
import javax.inject.Inject

class LocalSiteProviderHelper @Inject constructor(
    private val siteStore: SiteStore,
): LocalDataProviderHelper {
    override fun getData(localSiteId: Int?, localEntityId: Int?): LocalContentEntityData {
        return SitesData(localIds = siteStore.sites.map { it.id })
    }
}
