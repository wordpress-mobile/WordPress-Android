package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.ViewGroup
import org.wordpress.android.databinding.PagesItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.extensions.viewBinding

class PagesItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<PagesItemBinding>(
    parent.viewBinding(PagesItemBinding::inflate)
) {
    fun bind(item: PageContentItem) = with(binding) {
        uiHelpers.setTextOrHide(title, item.title)
    }
}
