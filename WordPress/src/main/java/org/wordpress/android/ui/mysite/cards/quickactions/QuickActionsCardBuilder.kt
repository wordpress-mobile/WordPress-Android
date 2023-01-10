package org.wordpress.android.ui.mysite.cards.quickactions

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class QuickActionsCardBuilder @Inject constructor() {
    fun build(params: QuickActionsCardBuilderParams) = QuickActionsCard(
        title = UiStringRes(R.string.my_site_quick_actions_title),
        onStatsClick = ListItemInteraction.create(params.onQuickActionStatsClick),
        onPagesClick = ListItemInteraction.create(params.onQuickActionPagesClick),
        onPostsClick = ListItemInteraction.create(params.onQuickActionPostsClick),
        onMediaClick = ListItemInteraction.create(params.onQuickActionMediaClick),
        showPages = params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages
    )
}
