package org.wordpress.android.localcontentmigration

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.localcontentmigration.LocalContentEntityData.EligibilityStatusData
import javax.inject.Inject

class LocalEligibilityStatusProviderHelper @Inject constructor(
        private val siteStore: SiteStore,
): LocalDataProviderHelper {
    override fun getData(localSiteId: Int?, localEntityId: Int?): LocalContentEntityData {
        @Suppress("ForbiddenComment")
        // TODO: check for eligibility on-the-fly
        return EligibilityStatusData(true, siteStore.sitesCount)
    }
}
