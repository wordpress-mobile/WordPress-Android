package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import org.wordpress.android.databinding.QuickLinkRibbonListBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class QuickLinkRibbonViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<QuickLinkRibbonListBinding>(
    parent.viewBinding(QuickLinkRibbonListBinding::inflate)
) {
    init {
        with(binding.quickLinkRibbonItemList) {
            if (adapter == null) {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = QuickLinkRibbonItemAdapter()
            }
        }
    }

    fun bind(quickLinkRibbon: QuickLinkRibbon) = with(binding) {
        quickLinkRibbonItemList.addItemDecoration(
            MaterialDividerItemDecoration(
                quickLinkRibbonItemList.context,
                DividerItemDecoration.VERTICAL
            ).apply {
                isLastItemDecorated = false
            }
        )
        (quickLinkRibbonItemList.adapter as QuickLinkRibbonItemAdapter).update(quickLinkRibbon.quickLinkRibbonItems)
        if (quickLinkRibbon.showMoreFocusPoint) {
            quickLinkRibbonItemList.smoothScrollToPosition(quickLinkRibbonItemList.adapter!!.itemCount - 1)
        }
    }
}
