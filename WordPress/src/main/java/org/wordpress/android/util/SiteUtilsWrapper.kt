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
}
