package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import android.view.ViewGroup
import org.wordpress.android.databinding.QuickLinkRibbonsListBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbons
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class QuickLinkRibbonsViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<QuickLinkRibbonsListBinding>(
        parent.viewBinding(QuickLinkRibbonsListBinding::inflate)
) {
    fun bind(quickLinkRibbons: QuickLinkRibbons) = with(binding) {
        stats.setOnClickListener { quickLinkRibbons.onStatsClick.click() }
        posts.setOnClickListener { quickLinkRibbons.onPostsClick.click() }
        media.setOnClickListener { quickLinkRibbons.onMediaClick.click() }
        pages.setOnClickListener { quickLinkRibbons.onPagesClick.click() }
        uiHelpers.updateVisibility(pages, quickLinkRibbons.showPages)
    }
}
