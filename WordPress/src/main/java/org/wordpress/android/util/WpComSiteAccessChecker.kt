package org.wordpress.android.util

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Answers whether the WordPress.com account signed in to this app can reach a site over the
 * WordPress.com REST API.
 *
 * A site added with an application password carries the blog ID of whichever WordPress.com account
 * its Jetpack connection belongs to, and that needn't be the account signed in here — a site can be
 * connected to WordPress.com by someone else entirely. So `isJetpackConnected` says the site is
 * connected to *an* account, not that it is connected to *this* one, and the WordPress.com-backed
 * features (Stats above all) fail with a 403 for the difference.
 */
@Singleton
class WpComSiteAccessChecker @Inject constructor(
    private val siteStore: SiteStore
) {
    /**
     * @return true if this account can use WordPress.com endpoints for [site]. Sites reached over the
     * WordPress.com REST API qualify by definition. Anything else has to prove it: the account's own
     * site list is the record of what it can reach, so the site qualifies when the same blog ID also
     * arrived from `/me/sites`.
     */
    fun hasWpComAccess(site: SiteModel): Boolean =
        site.isUsingWpComRestApi ||
                (site.siteId != 0L && siteStore.sitesAccessedViaWPComRest.any { it.siteId == site.siteId })
}
