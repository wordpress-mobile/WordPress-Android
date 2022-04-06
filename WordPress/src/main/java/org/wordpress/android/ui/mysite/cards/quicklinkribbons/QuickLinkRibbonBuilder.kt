package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class QuickLinkRibbonBuilder @Inject constructor() {
    fun build(params: QuickLinkRibbonBuilderParams) = QuickLinkRibbon(
        quickLinkRibbonItems = getQuickLinkRibbonItems(params),
        // todo @ajesh : extract to function
        showStatsFocusPoint = params.activeTask == QuickStartTask.CHECK_STATS && params.enableFocusPoints,
        showPagesFocusPoint = params.activeTask == QuickStartTask.EDIT_HOMEPAGE ||
                params.activeTask == QuickStartTask.REVIEW_PAGES && params.enableFocusPoints
    )

    private fun getQuickLinkRibbonItems(params: QuickLinkRibbonBuilderParams): MutableList<QuickLinkRibbonItem> {
        val items = mutableListOf<QuickLinkRibbonItem>()
        if (params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages) {
            val pages = QuickLinkRibbonItem(
                label = R.string.pages,
                icon = R.drawable.ic_pages_white_24dp,
                onClick = ListItemInteraction.create(params.onPagesClick),
                showFocusPoint = params.activeTask == QuickStartTask.EDIT_HOMEPAGE ||
                        params.activeTask == QuickStartTask.REVIEW_PAGES
            )
            items.add(pages)
        }
        items.apply {
            add(
                QuickLinkRibbonItem(
                    label = R.string.posts,
                    icon = R.drawable.ic_posts_white_24dp,
                    onClick = ListItemInteraction.create(params.onPostsClick)
                )
            )
            add(
                QuickLinkRibbonItem(
                    label = R.string.media,
                    icon = R.drawable.ic_media_white_24dp,
                    onClick = ListItemInteraction.create(params.onMediaClick),
                )
            )

            add(
                QuickLinkRibbonItem(
                    label =R.string.stats,
                    icon = R.drawable.ic_stats_alt_white_24dp,
                    onClick = ListItemInteraction.create(params.onStatsClick),
                    showFocusPoint = params.activeTask == QuickStartTask.CHECK_STATS
                )
            )
        }
        return items
    }
}
