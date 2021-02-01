package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.threat_details_list_header.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatDetailHeaderState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatDetailHeaderViewHolder(private val uiHelpers: UiHelpers, parent: ViewGroup) : JetpackViewHolder(
        R.layout.threat_details_list_header,
        parent
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as ThreatDetailHeaderState
        with(uiHelpers) {
            setTextOrHide(header, state.header)
            setTextOrHide(description, state.description)
        }
        icon.setImageResource(state.icon)
        icon.setBackgroundResource(itemUiState.iconBackground)
    }
}
