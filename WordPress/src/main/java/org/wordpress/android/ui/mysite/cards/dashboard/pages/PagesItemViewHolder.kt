package org.wordpress.android.ui.mysite.cards.dashboard.pages

import android.view.ViewGroup
import org.wordpress.android.databinding.PagesItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.PagesCard.PagesCardWithData.PageContentItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.viewBinding

class PagesItemViewHolder(
    parent: ViewGroup,
    private val uiHelpers: UiHelpers
) : MySiteCardAndItemViewHolder<PagesItemBinding>(
    parent.viewBinding(PagesItemBinding::inflate)
) {
    fun bind(item: PageContentItem) = with(binding) {
        pagesContentContainer.setOnClickListener { item.onClick.click() }
        uiHelpers.setTextOrHide(title, item.title)
        setStatusIcon(item.status, item.statusIcon)
        uiHelpers.setTextOrHide(lastEditedOrScheduledTime, item.lastEditedOrScheduledTime)
    }

    private fun setStatusIcon(statusText: UiString?, statusIcon: Int?) = with(binding) {
        statusText?.let {
            uiHelpers.setTextOrHide(status, it)
            statusIcon?.let { status.setCompoundDrawablesWithIntrinsicBounds(statusIcon, 0, 0, 0) }
        }
    }
}
