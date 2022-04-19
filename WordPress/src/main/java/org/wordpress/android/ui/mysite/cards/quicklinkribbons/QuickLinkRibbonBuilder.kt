package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class QuickLinkRibbonBuilder @Inject constructor() {
    fun build(params: QuickLinkRibbonBuilderParams) = QuickLinkRibbon(
        quickLinkRibbonItems = getQuickLinkRibbonItems(params)
    )

    private fun getQuickLinkRibbonItems(params: QuickLinkRibbonBuilderParams): MutableList<QuickLinkRibbonItem> {
        val items = mutableListOf<QuickLinkRibbonItem>()
        if (params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages) {
            val pages = QuickLinkRibbonItem(
                label = R.string.pages,
                icon = R.drawable.ic_pages_white_24dp,
                onClick = ListItemInteraction.create(params.onPagesClick)
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
                    onClick = ListItemInteraction.create(params.onMediaClick)
                )
            )

            add(
                QuickLinkRibbonItem(
                    label = R.string.stats,
                    icon = R.drawable.ic_stats_alt_white_24dp,
                    onClick = ListItemInteraction.create(params.onStatsClick)
                )
            )
        }
        return items
    }
}
