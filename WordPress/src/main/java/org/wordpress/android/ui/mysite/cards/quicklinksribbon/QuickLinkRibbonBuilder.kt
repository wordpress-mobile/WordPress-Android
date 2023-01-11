package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import org.wordpress.android.R
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class QuickLinkRibbonBuilder @Inject constructor(
    val quickStartRepository: QuickStartRepository
) {
    fun build(params: QuickLinkRibbonBuilderParams) = QuickLinkRibbon(
        quickLinkRibbonItems = getQuickLinkRibbonItems(params),
        showPagesFocusPoint = shouldShowPagesFocusPoint(params),
        showStatsFocusPoint = shouldShowStatsFocusPoint(params),
        showMediaFocusPoint = shouldShowMediaFocusPoint(params)
    )

    private fun getQuickLinkRibbonItems(params: QuickLinkRibbonBuilderParams): MutableList<QuickLinkRibbonItem> {
        val items = mutableListOf<QuickLinkRibbonItem>()
        items.apply {
            add(
                QuickLinkRibbonItem(
                    label = R.string.stats,
                    icon = R.drawable.ic_stats_alt_white_24dp,
                    onClick = ListItemInteraction.create(params.onStatsClick),
                    showFocusPoint = shouldShowStatsFocusPoint(params)
                )
            )
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
                    showFocusPoint = shouldShowMediaFocusPoint(params)
                )
            )
        }
        if (params.siteModel.isSelfHostedAdmin || params.siteModel.hasCapabilityEditPages) {
            val pages = QuickLinkRibbonItem(
                label = R.string.pages,
                icon = R.drawable.ic_pages_white_24dp,
                onClick = ListItemInteraction.create(params.onPagesClick),
                showFocusPoint = shouldShowPagesFocusPoint(params)
            )
            items.add(QUICK_LINK_PAGE_INDEX, pages)
        }
        return items
    }

    private fun shouldShowPagesFocusPoint(params: QuickLinkRibbonBuilderParams): Boolean {
        return params.enableFocusPoints && params.activeTask == QuickStartNewSiteTask.REVIEW_PAGES
    }

    private fun shouldShowStatsFocusPoint(params: QuickLinkRibbonBuilderParams): Boolean {
        return params.enableFocusPoints && params.activeTask == quickStartRepository.quickStartType.getTaskFromString(
            QuickStartStore.QUICK_START_CHECK_STATS_LABEL
        )
    }

    private fun shouldShowMediaFocusPoint(params: QuickLinkRibbonBuilderParams): Boolean {
        return params.enableFocusPoints && params.activeTask == quickStartRepository.quickStartType.getTaskFromString(
            QuickStartStore.QUICK_START_UPLOAD_MEDIA_LABEL
        )
    }

    companion object {
        private const val QUICK_LINK_PAGE_INDEX = 2
    }
}
