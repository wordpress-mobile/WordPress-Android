package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.threat_details_list_context_lines_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatContextLinesItemState
import org.wordpress.android.ui.jetpack.scan.details.adapters.ThreatContextLinesAdapter

class ThreatContextLinesViewHolder(parent: ViewGroup) : JetpackViewHolder(
    R.layout.threat_details_list_context_lines_item,
    parent
) {
    init {
        recycler_view.adapter = ThreatContextLinesAdapter()
    }

    override fun onBind(itemUiState: JetpackListItemState) {
        val contextLinesItemState = itemUiState as ThreatContextLinesItemState
        (recycler_view.adapter as ThreatContextLinesAdapter).update(contextLinesItemState.lines)
    }
}
