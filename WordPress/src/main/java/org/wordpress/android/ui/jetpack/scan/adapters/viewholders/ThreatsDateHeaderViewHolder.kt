package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.scan_list_threats_date_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatDateItemState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatsDateHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.scan_list_threats_date_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerItemState = itemUiState as ThreatDateItemState
        date_text.text = uiHelpers.getTextOfUiString(itemView.context, headerItemState.text)
    }
}
