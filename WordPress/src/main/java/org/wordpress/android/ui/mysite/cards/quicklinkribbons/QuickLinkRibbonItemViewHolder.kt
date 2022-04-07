package org.wordpress.android.ui.mysite.cards.quicklinkribbons

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.QuickLinkRibbonItemBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem
import org.wordpress.android.util.extensions.viewBinding

class QuickLinkRibbonItemViewHolder(
    parent: ViewGroup,
    private val binding: QuickLinkRibbonItemBinding = parent.viewBinding(QuickLinkRibbonItemBinding::inflate)
) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(item: QuickLinkRibbonItem) = with(binding.quickLinkItem) {
        setText(item.label)
        setIconResource(item.icon)
        setOnClickListener { item.onClick.click() }
    }
}
