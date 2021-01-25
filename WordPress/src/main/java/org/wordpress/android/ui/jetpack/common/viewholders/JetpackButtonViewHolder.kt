package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.jetpack_list_button_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.setVisible

class JetpackButtonViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_button_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val buttonState = itemUiState as ActionButtonState
        updateItemViewVisibility(buttonState.isVisible)
        uiHelpers.setTextOrHide(button, buttonState.text)
        button.isEnabled = buttonState.isEnabled
        button.setOnClickListener { buttonState.onClick.invoke() }
    }

    private fun updateItemViewVisibility(isVisible: Boolean) {
        with(itemView) {
            setVisible(isVisible)
            layoutParams = if (isVisible) {
                ConstraintLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            } else {
                ConstraintLayout.LayoutParams(0, 0)
            }
        }
    }
}
