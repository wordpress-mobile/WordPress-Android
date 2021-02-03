package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
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
        titleClick: () -> Unit,
        iconClick: () -> Unit,
        urlClick: () -> Unit,
        switchSiteClick: () -> Unit,
        showUpdateSiteTitleFocusPoint: Boolean,
        showUploadSiteIconFocusPoint: Boolean
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
                showUploadSiteIconFocusPoint,
                buildTitleClick(site, titleClick),
                ListItemInteraction.create(iconClick),
                ListItemInteraction.create(urlClick),
                ListItemInteraction.create(switchSiteClick)
        )
    }

    private fun buildTitleClick(site: SiteModel, titleClick: () -> Unit): ListItemInteraction? {
        return if (SiteUtils.isAccessedViaWPComRest(site)) {
            ListItemInteraction.create(titleClick)
        } else {
            null
        }
    }
}
