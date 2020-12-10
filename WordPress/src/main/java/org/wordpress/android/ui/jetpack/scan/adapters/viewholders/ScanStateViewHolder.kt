package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.utils.UiHelpers

class ScanStateViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : ScanViewHolder(R.layout.scan_list_scan_state_item, parent) {
    override fun onBind(uiState: ScanListItemState) {
    }
}
