package org.wordpress.android.util

import android.content.Context
import androidx.annotation.DimenRes
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
class SiteUtilsWrapper @Inject constructor(private val appContext: Context) {
    fun isPhotonCapable(site: SiteModel): Boolean = SiteUtils.isPhotonCapable(site)
    fun getAccessibilityInfoFromSite(site: SiteModel): SiteAccessibilityInfo =
            SiteUtils.getAccessibilityInfoFromSite(site)

    fun isAccessedViaWPComRest(site: SiteModel): Boolean = SiteUtils.isAccessedViaWPComRest(site)
    fun onFreePlan(site: SiteModel): Boolean = SiteUtils.onFreePlan(site)
    fun getSiteNameOrHomeURL(site: SiteModel): String = SiteUtils.getSiteNameOrHomeURL(site)
    fun getHomeURLOrHostName(site: SiteModel): String = SiteUtils.getHomeURLOrHostName(site)
    fun getSiteIconUrlOfResourceSize(site: SiteModel, @DimenRes sizeRes: Int): String {
        return SiteUtils.getSiteIconUrl(site, appContext.resources.getDimensionPixelSize(sizeRes))
    }
}
