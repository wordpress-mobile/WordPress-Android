package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ScanListThreatsDateItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatDateItemState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatsDateHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<ScanListThreatsDateItemBinding>(
    parent,
    ScanListThreatsDateItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerItemState = itemUiState as ThreatDateItemState
        binding.dateText.text = uiHelpers.getTextOfUiString(itemView.context, headerItemState.text)
    }
}
