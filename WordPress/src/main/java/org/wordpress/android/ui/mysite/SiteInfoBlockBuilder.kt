package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.UPDATE_SITE_TITLE
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock
import org.wordpress.android.ui.mysite.MySiteItem.SiteInfoBlock.IconState
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SiteInfoBlockBuilder
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun buildSiteInfoBlock(
        site: SiteModel,
        showSiteIconProgressBar: Boolean,
        titleClick: (SiteModel) -> Unit,
        iconClick: (SiteModel) -> Unit,
        urlClick: (SiteModel) -> Unit,
        switchSiteClick: (SiteModel) -> Unit,
        showUpdateSiteTitleFocusPoint: Boolean
    ): SiteInfoBlock {
        val homeUrl = SiteUtils.getHomeURLOrHostName(site)
        val blogTitle = SiteUtils.getSiteNameOrHomeURL(site)
        val siteIcon = if (!showSiteIconProgressBar && !site.iconUrl.isNullOrEmpty()) {
            IconState.Visible(SiteUtils.getSiteIconUrl(
                    site,
                    resourceProvider.getDimensionPixelSize(R.dimen.blavatar_sz_small)
            ))
        } else if (showSiteIconProgressBar) {
            IconState.Progress
        } else {
            IconState.Visible()
        }
        return SiteInfoBlock(
                blogTitle,
                homeUrl,
                siteIcon,
                showUpdateSiteTitleFocusPoint,
                buildTitleClick(site, titleClick),
                ListItemInteraction.create(site, iconClick),
                ListItemInteraction.create(site, urlClick),
                ListItemInteraction.create(site, switchSiteClick)
        )
    }

    private fun buildTitleClick(site: SiteModel, titleClick: (SiteModel) -> Unit): ListItemInteraction? {
        return if (SiteUtils.isAccessedViaWPComRest(site)) {
            ListItemInteraction.create(site, titleClick)
        } else {
            null
        }
    }
}
