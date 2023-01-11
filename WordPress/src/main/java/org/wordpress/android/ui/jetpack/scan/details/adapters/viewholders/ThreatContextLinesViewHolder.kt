package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ThreatDetailsListContextLinesItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState
import org.wordpress.android.ui.jetpack.scan.details.adapters.ThreatContextLinesAdapter

class ThreatContextLinesViewHolder(parent: ViewGroup) : JetpackViewHolder<ThreatDetailsListContextLinesItemBinding>(
    parent,
    ThreatDetailsListContextLinesItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) = with(binding) {
        val contextLinesItemState = itemUiState as ThreatContextLinesItemState
        if (recyclerView.adapter == null) {
            recyclerView.adapter = ThreatContextLinesAdapter()
        }
        (recyclerView.adapter as ThreatContextLinesAdapter).update(contextLinesItemState.lines)
    }
}
