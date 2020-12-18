package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.scan_list_threat_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState

class ThreatViewHolder(parent: ViewGroup) : JetpackViewHolder(R.layout.scan_list_threat_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val threatsFoundState = itemUiState as ThreatItemState
        // TODO: ashiagr fix title, description texts based on threat types
        threat_title.text = threatsFoundState.title
    }
}
