package org.wordpress.android.ui.newstats

import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository

/**
 * Every New Stats screen renders the currently selected site: none of them take a site, and the
 * view models read it from [SelectedSiteRepository]. So a caller that can be open for a different
 * site - a stats widget, a deep link, or the Posts list opened from a notification - names its
 * site in the Intent as [org.wordpress.android.WordPress.LOCAL_SITE_ID], and it is selected here
 * before the screen is composed.
 *
 * Call this before anything that reads the selected site: the fetch, and the tracking that
 * attributes the open to a blog id.
 *
 * [localSiteId] of 0 means the Intent named no site, which is the usual case from inside New
 * Stats, and leaves the selection alone. So does a site the store cannot resolve.
 */
internal fun selectNewStatsSiteIfNeeded(
    localSiteId: Int,
    siteStore: SiteStore,
    selectedSiteRepository: SelectedSiteRepository
) {
    if (localSiteId == 0 || localSiteId == selectedSiteRepository.getSelectedSiteLocalId()) return
    siteStore.getSiteByLocalId(localSiteId)?.let { site ->
        selectedSiteRepository.updateSite(site)
    }
}
