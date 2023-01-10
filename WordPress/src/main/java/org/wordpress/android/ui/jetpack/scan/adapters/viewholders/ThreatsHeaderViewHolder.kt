package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ScanListThreatsHeaderItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatsHeaderItemState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatsHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<ScanListThreatsHeaderItemBinding>(
    parent,
    ScanListThreatsHeaderItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerItemState = itemUiState as ThreatsHeaderItemState
        binding.headerText.text = uiHelpers.getTextOfUiString(itemView.context, headerItemState.text)
    }
}
