package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.backup_download_list_header_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.HeaderState
import org.wordpress.android.ui.utils.UiHelpers

class HeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.backup_download_list_header_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val headerState = itemUiState as HeaderState
        backup_download_header.text = uiHelpers.getTextOfUiString(itemView.context, headerState.text)
    }
}
