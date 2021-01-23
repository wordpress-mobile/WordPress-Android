package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_progress_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.setVisible

class JetpackProgressViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_progress_item, parent) { // TODO ashiagr replace layout
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as ProgressState
        with(progress_label) {
            setVisible(state.isLabelVisible)
            text = uiHelpers.getTextOfUiString(itemView.context, state.progressLabel)
        }

        with(progress_bar) {
            setVisible(state.isVisible)
            isIndeterminate = state.isIndeterminate
            progress = state.progress
        }
    }
}
