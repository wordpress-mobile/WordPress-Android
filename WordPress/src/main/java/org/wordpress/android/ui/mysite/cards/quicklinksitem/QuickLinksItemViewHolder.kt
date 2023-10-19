package org.wordpress.android.ui.mysite.cards.quicklinksitem

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.QuickLinkItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem
import org.wordpress.android.util.extensions.viewBinding

class QuickLinksItemViewHolder(
    parent: ViewGroup,
    private val binding: QuickLinkItemBinding = parent.viewBinding(QuickLinkItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(item: QuickLinkItem) = with(binding) {
        quickLinkItem.setText(item.label.stringRes)
        quickLinkItem.setIconResource(item.icon)
        quickLinkItem.setOnClickListener { item.onClick.click() }
        quickLinkItemQuickStartFocusPoint.setVisibleOrGone(item.showFocusPoint)
        if (item.disableTint) quickLinkItem.iconTint = null
    }
}
