package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.scan_list_threat_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.getColorFromAttribute

class ThreatViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.scan_list_threat_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val threatItemState = itemUiState as ThreatItemState
        with(uiHelpers) {
            setTextOrHide(threat_header, threatItemState.header)
            setTextOrHide(threat_sub_header, threatItemState.subHeader)
        }
        threat_sub_header.setTextColor(threat_sub_header.context.getColorFromAttribute(itemUiState.subHeaderColor))
        threat_icon.setImageResource(itemUiState.icon)
        threat_icon.setBackgroundResource(itemUiState.iconBackground)
        itemView.setOnClickListener { threatItemState.onClick.invoke() }
    }
}
