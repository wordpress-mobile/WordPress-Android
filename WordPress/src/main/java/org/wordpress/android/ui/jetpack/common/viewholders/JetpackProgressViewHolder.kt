package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import kotlinx.android.synthetic.main.jetpack_list_progress_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.setVisible

class JetpackProgressViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_progress_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as ProgressState
        updateItemViewVisibility(state)
        updateProgressBar(state)
        updateProgressStateLabel(state)
        uiHelpers.setTextOrHide(progress_label, state.progressLabel)
        uiHelpers.setTextOrHide(progress_info_label, state.progressInfoLabel)
    }

    private fun updateItemViewVisibility(state: ProgressState) {
        with(itemView) {
            setVisible(state.isVisible)
            layoutParams = if (state.isVisible) {
                LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            } else {
                LayoutParams(0, 0)
            }
        }
    }

    private fun updateProgressStateLabel(state: ProgressState) {
        uiHelpers.setTextOrHide(progress_state_label, state.progressStateLabel)
        progress_state_label.textAlignment = state.progressStateLabelTextAlignment
    }

    private fun updateProgressBar(state: ProgressState) {
        with(progress_bar) {
            isIndeterminate = state.isIndeterminate
            progress = state.progress
        }
    }
}
