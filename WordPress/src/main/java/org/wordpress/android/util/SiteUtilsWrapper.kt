package org.wordpress.android.util

import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.reader.utils.SiteAccessibilityInfo
import javax.inject.Inject

/**
 * Injectable wrapper around SiteUtilsWrapper.
 *
 * SiteUtilsWrapper interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Reusable
class SiteUtilsWrapper @Inject constructor() {
    fun isPhotonCapable(site: SiteModel): Boolean = SiteUtils.isPhotonCapable(site)
    fun getAccessibilityInfoFromSite(site: SiteModel): SiteAccessibilityInfo =
            SiteUtils.getAccessibilityInfoFromSite(site)

    fun isAccessedViaWPComRest(site: SiteModel): Boolean = SiteUtils.isAccessedViaWPComRest(site)
    fun onFreePlan(site: SiteModel): Boolean = SiteUtils.onFreePlan(site)
    fun hasCustomDomain(site: SiteModel): Boolean = SiteUtils.hasCustomDomain(site)

    // TODO: Inline this method when legacy MySiteFragment is removed
    fun isScanEnabled(scanFeatureFlagEnabled: Boolean, scanPurchased: Boolean, site: SiteModel) =
            SiteUtils.isScanEnabled(scanFeatureFlagEnabled, scanPurchased, site)
    // TODO: Inline this method when legacy MySiteFragment is removed
    fun isBackupEnabled(backupFeatureFlagEnabled: Boolean, backupPurchased: Boolean) =
            SiteUtils.isBackupEnabled(backupFeatureFlagEnabled, backupPurchased)
}
