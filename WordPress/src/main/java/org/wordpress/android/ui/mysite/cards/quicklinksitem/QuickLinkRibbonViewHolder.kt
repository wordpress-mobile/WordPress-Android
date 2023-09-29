package org.wordpress.android.ui.mysite.cards.quicklinksitem

import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.divider.MaterialDividerItemDecoration
import org.wordpress.android.databinding.QuickLinksListBinding
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinksItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemViewHolder
import org.wordpress.android.util.extensions.viewBinding

class QuickLinkRibbonViewHolder(
    parent: ViewGroup
) : MySiteCardAndItemViewHolder<QuickLinksListBinding>(
    parent.viewBinding(QuickLinksListBinding::inflate)
) {
    init {
        with(binding.quickLinksItemList) {
            if (adapter == null) {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = QuickLinksItemAdapter()
            }
        }
    }

    fun bind(quickLinksItem: QuickLinksItem) = with(binding) {
        quickLinksItemList.addItemDecoration(
            MaterialDividerItemDecoration(
                quickLinksItemList.context,
                DividerItemDecoration.VERTICAL
            ).apply {
                isLastItemDecorated = false
            }
        )
        (quickLinksItemList.adapter as QuickLinksItemAdapter).update(quickLinksItem.quickLinkItems)
        if (quickLinksItem.showMoreFocusPoint) {
            quickLinksItemList.smoothScrollToPosition(quickLinksItemList.adapter!!.itemCount - 1)
        }
    }
}
