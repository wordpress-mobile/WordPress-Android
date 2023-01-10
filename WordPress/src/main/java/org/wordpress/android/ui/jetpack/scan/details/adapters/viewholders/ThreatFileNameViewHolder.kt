package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.view.ViewGroup
import org.wordpress.android.databinding.ThreatDetailsListFileNameItemBinding
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatFileNameState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatFileNameViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder<ThreatDetailsListFileNameItemBinding>(
    parent,
    ThreatDetailsListFileNameItemBinding::inflate
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val threatFileNameState = itemUiState as ThreatFileNameState
        binding.fileName.text = uiHelpers.getTextOfUiString(itemView.context, threatFileNameState.fileName)
    }
}
