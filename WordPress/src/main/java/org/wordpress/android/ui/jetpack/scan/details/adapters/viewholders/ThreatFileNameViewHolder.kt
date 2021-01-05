package org.wordpress.android.ui.jetpack.scan.details.adapters.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.threat_details_list_file_name_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsListItemState.ThreatFileNameState
import org.wordpress.android.ui.utils.UiHelpers

class ThreatFileNameViewHolder(private val uiHelpers: UiHelpers, parent: ViewGroup) : JetpackViewHolder(
    R.layout.threat_details_list_file_name_item,
    parent
) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val threatFileNameState = itemUiState as ThreatFileNameState
        file_name.text = uiHelpers.getTextOfUiString(itemView.context, threatFileNameState.fileName)
    }
}
