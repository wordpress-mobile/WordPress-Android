package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.scan_list_threats_header_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatsHeaderItemState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatsHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.scan_list_threats_header_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerItemState = itemUiState as ThreatsHeaderItemState
        header_text.text = uiHelpers.getTextOfUiString(itemView.context, headerItemState.text)
    }
}
