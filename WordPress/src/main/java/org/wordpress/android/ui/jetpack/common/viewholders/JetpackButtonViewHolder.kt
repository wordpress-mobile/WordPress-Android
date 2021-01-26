package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.setVisible
import kotlinx.android.synthetic.main.jetpack_list_button_primary_item.button as primaryButton
import kotlinx.android.synthetic.main.jetpack_list_button_secondary_item.button as secondaryButton

sealed class JetpackButtonViewHolder(@LayoutRes layout: Int, parent: ViewGroup) : JetpackViewHolder(layout, parent) {
    class Primary(
        private val uiHelpers: UiHelpers,
        parent: ViewGroup
    ) : JetpackButtonViewHolder(R.layout.jetpack_list_button_primary_item, parent) {
        override fun onBind(itemUiState: JetpackListItemState) {
            primaryButton.updateState(itemUiState as ActionButtonState, uiHelpers)
        }
    }

    class Secondary(
        private val uiHelpers: UiHelpers,
        parent: ViewGroup
    ) : JetpackButtonViewHolder(R.layout.jetpack_list_button_secondary_item, parent) {
        override fun onBind(itemUiState: JetpackListItemState) {
            secondaryButton.updateState(itemUiState as ActionButtonState, uiHelpers)
        }
    }

    internal fun MaterialButton.updateState(buttonState: ActionButtonState, uiHelpers: UiHelpers) {
        updateItemViewVisibility(buttonState.isVisible)
        uiHelpers.setTextOrHide(this, buttonState.text)
        isEnabled = buttonState.isEnabled
        setOnClickListener { buttonState.onClick.invoke() }
        buttonState.iconRes?.let {
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            icon = this.context.getDrawable(it)
        }
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
