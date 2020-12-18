package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.backup_download_list_button_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.utils.UiHelpers

class ActionButtonViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.backup_download_list_button_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val buttonState = itemUiState as ActionButtonState
        uiHelpers.setTextOrHide(backup_download_action_button, buttonState.text)
        backup_download_action_button.setOnClickListener { buttonState.onClick.invoke() }
    }
}
