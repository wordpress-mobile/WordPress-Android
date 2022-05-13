package org.wordpress.android.ui.mysite.cards.siteinfo

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.Companion.QUICK_START_VIEW_SITE_LABEL
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.SiteInfoHeaderCard.IconState
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class SiteInfoHeaderCardBuilder
@Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val quickStartRepository: QuickStartRepository
) {
    fun buildSiteInfoCard(params: SiteInfoCardBuilderParams): SiteInfoHeaderCard {
        val homeUrl = SiteUtils.getHomeURLOrHostName(params.site)
        val blogTitle = SiteUtils.getSiteNameOrHomeURL(params.site)
        val siteIcon = if (!params.showSiteIconProgressBar && !params.site.iconUrl.isNullOrEmpty()) {
            IconState.Visible(
                    SiteUtils.getSiteIconUrl(
                            params.site,
                            resourceProvider.getDimensionPixelSize(R.dimen.blavatar_sz_small)
                    )
            )
        } else if (params.showSiteIconProgressBar) {
            IconState.Progress
        } else {
            IconState.Visible()
        }
        return SiteInfoHeaderCard(
                blogTitle,
                homeUrl,
                siteIcon,
                params.activeTask == QuickStartNewSiteTask.UPDATE_SITE_TITLE,
                params.activeTask ==
                        quickStartRepository.quickStartType.getTaskFromString(QUICK_START_VIEW_SITE_LABEL),
                params.activeTask == QuickStartNewSiteTask.UPLOAD_SITE_ICON,
                buildTitleClick(params.site, params.titleClick),
                ListItemInteraction.create(params.iconClick),
                ListItemInteraction.create(params.urlClick),
                ListItemInteraction.create(params.switchSiteClick)
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
