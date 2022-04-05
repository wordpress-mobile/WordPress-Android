package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class QuickLinkRibbonBuilder @Inject constructor() {
    fun build(params: QuickLinkRibbonBuilderParams) = QuickLinkRibbon(
            onPagesClick = ListItemInteraction.create(params.onPagesClick),
            onPostsClick = ListItemInteraction.create(params.onPostsClick),
            onMediaClick = ListItemInteraction.create(params.onMediaClick),
            onStatsClick = ListItemInteraction.create(params.onStatsClick),
            showPages = params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages
    )
}
