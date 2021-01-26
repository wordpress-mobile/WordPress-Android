package org.wordpress.android.ui.jetpack.common.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_list_checkbox_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.utils.UiHelpers

class JetpackCheckboxViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_list_checkbox_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val checkboxState = itemUiState as CheckboxState
        uiHelpers.setTextOrHide(checkbox_label, checkboxState.label)
        checkbox.isChecked = checkboxState.checked
        item_container.setOnClickListener { checkboxState.onClick.invoke() }
    }
}
