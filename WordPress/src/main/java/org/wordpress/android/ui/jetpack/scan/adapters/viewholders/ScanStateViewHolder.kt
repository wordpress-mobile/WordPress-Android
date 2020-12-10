package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.scan_list_scan_state_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState
import org.wordpress.android.ui.utils.UiHelpers

class ScanStateViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : ScanViewHolder(R.layout.scan_list_scan_state_item, parent) {
    override fun onBind(itemUiState: ScanListItemState) {
        val scanState = itemUiState as ScanState

        scan_state_icon.setImageResource(scanState.scanIcon)
        with(uiHelpers) {
            scan_state_title.text = getTextOfUiString(itemView.context, scanState.scanTitle)
            scan_state_description.text = getTextOfUiString(itemView.context, scanState.scanDescription)
        }
    }
}
