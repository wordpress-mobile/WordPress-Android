package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbons
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonsBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class QuickLinkRibbonsBuilder @Inject constructor() {
    fun build(params: QuickLinkRibbonsBuilderParams) = QuickLinkRibbons(
            onStatsClick = ListItemInteraction.create(params.onStatsClick),
            onPagesClick = ListItemInteraction.create(params.onPagesClick),
            onPostsClick = ListItemInteraction.create(params.onPostsClick),
            onMediaClick = ListItemInteraction.create(params.onMediaClick),
            showPages = params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages,
    )
}
