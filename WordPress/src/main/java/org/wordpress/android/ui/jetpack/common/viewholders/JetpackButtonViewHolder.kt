package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_button_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackButtonViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_button_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val buttonState = itemUiState as ActionButtonState
        uiHelpers.setTextOrHide(button, buttonState.text)
        button.isEnabled = buttonState.isEnabled
        button.setOnClickListener { buttonState.onClick.invoke() }
    }
}
