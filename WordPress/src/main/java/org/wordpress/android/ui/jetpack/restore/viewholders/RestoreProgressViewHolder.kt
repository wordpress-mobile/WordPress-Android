package org.wordpress.android.ui.jetpack.restore.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_progress_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.jetpack.restore.RestoreListItemState.ProgressState
import org.wordpress.android.ui.utils.UiHelpers

class RestoreProgressViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_progress_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as ProgressState
        progress_label.text = uiHelpers.getTextOfUiString(itemView.context, state.label)
    }
}
